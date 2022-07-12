package io.github.chrisruffalo.qgwt;

import com.google.gwt.dev.codeserver.Options;
import com.google.gwt.dev.codeserver.QuarkusCodeServer;
import io.github.chrisruffalo.qgwt.configuration.GwtConfiguration;
import io.github.chrisruffalo.qgwt.logger.QuarkusTreeLogger;
import io.github.chrisruffalo.qgwt.model.GwtInherits;
import io.github.chrisruffalo.qgwt.model.GwtPathElement;
import io.github.chrisruffalo.qgwt.model.SimpleGwtModuleXml;
import io.github.chrisruffalo.qgwt.runtime.CodeServerProxy;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import org.jboss.logging.Logger;

import javax.servlet.DispatcherType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CodeServer {

    private static final Logger LOGGER = Logger.getLogger(CodeServer.class);

    private static final String GWT_BRIDGE = "com/google/gwt/core/client/GWTBridge.java";

    private static final String FEATURE = "gwt-codeserver";

    private static final AtomicReference<QuarkusCodeServer> quarkusCodeServerRef = new AtomicReference<>();

    @BuildStep(onlyIf = IsDevMode.class)
    public FilterBuildItem addCodeServerProxyFilter(GwtConfiguration config) {
        return FilterBuildItem.builder("gwt-codeserver-proxy", CodeServerProxy.class.getName())
            .addFilterUrlMapping("/*", DispatcherType.REQUEST)
            .addInitParam(CodeServerProxy.BIND_ADDRESS_PARAM, config.getCodeServer().getBindAddress())
            .addInitParam(CodeServerProxy.BIND_PORT_PARAM, String.valueOf(config.getCodeServer().getPort()))
            .build();
    }

    @BuildStep(onlyIf = IsDevMode.class)
    public FeatureBuildItem startCodeServer(LiveReloadBuildItem reload, GwtConfiguration config) {

        if (config.getModules().isEmpty()) {
            LOGGER.warn("Skipping GWT code server execution, no modules given ('quarkus.gwt.modules')");
            return null;
        }

        final Path mainSourceRoot = Paths.get(config.getSourceRoot()).normalize().toAbsolutePath();
        if (!Files.exists(mainSourceRoot) || !Files.isDirectory(mainSourceRoot)) {
            LOGGER.errorf("Could not find or read main source root directory '%s'", mainSourceRoot);
            return null;
        }

        // get classpath root
        Path rootPath = Paths.get(config.getClassesDir()).normalize().toAbsolutePath();
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            LOGGER.errorf("Classes root '%s' %s", rootPath.toString(), Files.exists(rootPath) ? "is not a directory" : "does not exist");
            return null;
        }

        // need temp directory
        Path tmpDir = Paths.get(config.getTempDir()).normalize().toAbsolutePath();
        if (!Files.exists(tmpDir)) {
            try {
                Files.createDirectories(tmpDir);
            } catch (IOException e) {
                LOGGER.errorf("Could not create QGWT temporary directory '%s': %s", tmpDir, e.getMessage());
                return null;
            }
        }
        if (!Files.isDirectory(tmpDir)) {
            throw new RuntimeException(String.format("Temporary path '%s' already exists and is not a directory", tmpDir));
        }

        tmpDir = tmpDir.normalize().toAbsolutePath();
        final Path work = tmpDir.resolve("work");

        try {
            Files.createDirectories(work);
        } catch (IOException e) {
            throw new RuntimeException("could not create directory for qgwt use", e);
        }

        final List<String> argList = new LinkedList<>();
        argList.add("-noprecompile");
        argList.add("-logLevel");
        argList.add("ALL");
        argList.add("-workDir");
        argList.add(work.toString());
        argList.add("-sourceLevel");
        argList.add("11");

        // add all source roots
        argList.add("-src");
        argList.add(mainSourceRoot.toString());
        if(config.getSourceRoots().isPresent()) {
            final List<String> sourceRoots = config.getSourceRoots().get();
            sourceRoots.stream()
                .filter(path -> path != null && !path.isEmpty())
                .map(Paths::get)
                .filter(Files::exists)
                .forEach(path -> {
                    argList.add("-src");
                    argList.add(path.normalize().toAbsolutePath().toString());
                });
        }

        final List<String> selectedModules = new LinkedList<>(config.getModules());
        final Set<Path> changableSourcePaths = new HashSet<>();
        final List<String> modules = config.getModules();

        // the root path is also a source root
        argList.add("-src");
        argList.add(rootPath.toString());

        // extract required jar resources to classpath
        if (!reload.isLiveReload()) {
            // we need to add java source files in the gwt-dev jar to the classpath so first find the GWT bridge
            URL bridge = Thread.currentThread().getContextClassLoader().getResource(GWT_BRIDGE);
            if (bridge == null) {
                LOGGER.errorf("Could not start GWT code server because gwt-dev jar could not be found on classpath / %s could not be resolved as a resource", GWT_BRIDGE);
                return null;
            }
            final JarExtractor jarExtractor = new JarExtractor();
            jarExtractor.extract(bridge, rootPath);
        }

        // go through modules to add source directories after loading xml, adding modules as
        // they are found but using this guard to prevent adding the same module multiple times
        final Set<String> guard = new HashSet<>();
        for(int idx = 0; idx < modules.size(); idx++) {
            final String module = modules.get(idx);
            guard.add(module);
            final String xmlResourceLocation = String.format("%s.gwt.xml", module.replace(".", "/"));
            final URL xmlResource = Thread.currentThread().getContextClassLoader().getResource(xmlResourceLocation);
            if(null == xmlResource) {
                LOGGER.warnf("Could not find '%s' for module '%s'", xmlResourceLocation, module);
                continue;
            }
            Path xmlPath;
            URI xmlUri = null;
            try {
                xmlUri = xmlResource.toURI();
                xmlPath = Paths.get(xmlUri);
            } catch (URISyntaxException uex) {
                throw new RuntimeException(uex);
            } catch (FileSystemNotFoundException fsne) {
                xmlPath = null;
            }

            SimpleGwtModuleXml model;
            JAXBContext context;
            try {
                context = JAXBContext.newInstance(SimpleGwtModuleXml.class);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            try {
                model = (SimpleGwtModuleXml) context.createUnmarshaller().unmarshal(xmlResource);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            LOGGER.debugf("Working on loaded module: %s", xmlResource);

            // add inherited modules
            model.getInherits().stream().map(GwtInherits::getName)
                .filter(moduleName -> !guard.contains(moduleName))
                .forEach(moduleName -> {
                    guard.add(moduleName);
                    modules.add(moduleName);
                });

            final List<GwtPathElement> paths = new LinkedList<>();
            paths.addAll(model.getSuperSources());
            paths.addAll(model.getSources());
            paths.addAll(model.getPublicResources());
            paths.addAll(model.getStylesheets());

            // extract path resources as needed
            final ModuleExtractor resourceExtractor = new ModuleExtractor(xmlPath, xmlUri, rootPath, reload.isLiveReload());
            paths.stream().map(GwtPathElement::getPath).forEach(resourceExtractor);
            changableSourcePaths.addAll(resourceExtractor.getSourceRoots());
        }

        // determine recompile
        AtomicBoolean recompile = new AtomicBoolean(false);

        // update changed resources
        if(reload.isLiveReload()) {
            final List<String> needsUpdates = new LinkedList<>();
            needsUpdates.addAll(reload.getChangeInformation().getChangedClasses());
            needsUpdates.addAll(reload.getChangeInformation().getAddedClasses());

            final List<String> needsRemoval = new LinkedList<>();
            needsRemoval.addAll(reload.getChangeInformation().getChangedClasses());
            needsRemoval.addAll(reload.getChangeInformation().getDeletedClasses());

            // remove changed java
            needsRemoval
                .stream()
                .map(remove -> String.format("%s.java", remove.replace(".", "/")))
                .map(java -> rootPath.resolve(java).normalize().toAbsolutePath())
                .filter(Files::exists)
                .forEach(target -> {
                    try {
                        Files.delete(target);
                        recompile.set(true);
                    } catch (IOException e) {
                        LOGGER.errorf("Could not remove changed file %s", target);
                    }
                });

            // copy new files if they are inside the modules
            needsUpdates
                .stream()
                .map(remove -> String.format("%s.java", remove.replace(".", "/")))
                .map(java -> mainSourceRoot.resolve(java).normalize().toAbsolutePath())
                .filter(Files::exists)
                .filter(target -> changableSourcePaths.stream().anyMatch(changable -> mainSourceRoot.relativize(target).startsWith(rootPath.relativize(changable))))
                .forEach(changed -> {
                    final Path target = rootPath.resolve(mainSourceRoot.relativize(changed)).normalize().toAbsolutePath();
                    try {
                        Files.createDirectories(target.getParent());
                        Files.copy(changed, target, StandardCopyOption.REPLACE_EXISTING);
                        recompile.set(true);
                    } catch (IOException e) {
                        LOGGER.errorf("Could not copy changed file %s to target path %s", changed, target);
                    }
                });
        }

        // if the modules are available add them all as arguments
        argList.addAll(selectedModules);
        LOGGER.infof("Selected modules: %s", String.join(", ", selectedModules));
        final String[] args = argList.toArray(new String[0]);

        // do not re-start
        if(quarkusCodeServerRef.get() == null) {
            LOGGER.info("Launching Code Server...");
            LOGGER.debugf("Args: %s", String.join(" ", args));
            final QuarkusTreeLogger treeLogger = new QuarkusTreeLogger(LOGGER);
            try {
                final Options options = new Options();
                if (!options.parseArgs(args)) {
                    LOGGER.errorf("Could not parse GWT code server options, feature '%s' will not be loaded", FEATURE);
                    return null;
                }
                final QuarkusCodeServer quarkusCodeServer = new QuarkusCodeServer(tmpDir);
                quarkusCodeServer.start(treeLogger, options);
                quarkusCodeServerRef.set(quarkusCodeServer);
            } catch (Exception ex) {
                LOGGER.errorf("Could not start GWT codeserver: %s", ex.getMessage());
                return null;
            }
        } else if(recompile.get()) {
            // try and update
            quarkusCodeServerRef.get().refresh();
        }

        return new FeatureBuildItem(FEATURE);
    }

}
