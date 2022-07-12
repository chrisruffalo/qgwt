package io.github.chrisruffalo.qgwt;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ModuleExtractor extends Extractor implements Consumer<String> {

    private static final Logger LOGGER = Logger.getLogger(ModuleExtractor.class);

    final Path resolvedSource;

    final URI resolvedXml;

    final Path targetRoot;

    final Set<Path> sourceRoots = new LinkedHashSet<>();

    final boolean liveReload;

    public ModuleExtractor(final Path resolvedSource, final URI resolvedXml, final Path targetRoot, final boolean liveReload) {
        this.resolvedSource = resolvedSource;
        this.resolvedXml = resolvedXml;
        this.targetRoot = targetRoot;
        this.liveReload = liveReload;
    }

    @Override
    public void accept(String path) {
        // skip null path
        if (path == null) {
            return;
        }

        if (resolvedSource != null) {
            final Path sourcePath = resolvedSource.resolveSibling(path).normalize().toAbsolutePath();
            LOGGER.debugf("Adding source path: %s", sourcePath);
            sourceRoots.add(sourcePath);
        } else if(!liveReload && resolvedXml.toString().startsWith("jar") && resolvedXml.toString().contains("!")) {
            final String[] split = resolvedXml.toString().split("!");
            if (split[1] == null || split[1].isEmpty()) {
                return;
            }

            // load jar filesystem
            try (final FileSystem fs = FileSystems.newFileSystem(new URI(split[0]), new HashMap<>())) {
                final Path targetXml = Paths.get(targetRoot.toString(), split[1].substring(1));
                Files.createDirectories(targetXml.getParent());
                Files.copy(fs.getPath(split[1]), targetXml, StandardCopyOption.REPLACE_EXISTING);

                final Path root = fs.getPath(split[1]);
                final Path resolved = root.resolveSibling(path);
                if (Files.exists(resolved)) {
                    LOGGER.debugf("Extracting source path zip: %s to target %s", resolved, targetRoot);
                    try (Stream<Path> stream = Files.walk(resolved)) {
                        processStreamToDirectory(split[0], stream, targetRoot);
                    }
                }
            } catch (URISyntaxException | IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public Set<Path> getSourceRoots() {
        return sourceRoots;
    }
}
