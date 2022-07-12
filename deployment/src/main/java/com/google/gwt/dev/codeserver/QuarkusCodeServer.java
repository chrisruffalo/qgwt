package com.google.gwt.dev.codeserver;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.util.tools.Utility;
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;
import io.github.chrisruffalo.qgwt.logger.QuarkusTreeLogger;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This is a copy of the CodeServer class that allows injecting a custom (tree) logger as well
 * as setting up some custom directories/locations for GWT to work with Quarkus in a more seamless way.
 */
public class QuarkusCodeServer extends CodeServer {

    private final Path baseCachePath;

    private OutboxTable outboxTable;

    private TreeLogger currentLogger;

    public QuarkusCodeServer() {
        this(Paths.get(System.getProperty("java.io.tmpdir")));
    }

    public QuarkusCodeServer(final Path tmpDir) {
        this.baseCachePath = tmpDir;
    }

    public WebServer start(QuarkusTreeLogger logger, Options options) throws IOException, UnableToCompleteException {
        this.currentLogger = logger;
        logger.setMaxDetail(options.getLogLevel());

        TreeLogger startupLogger = logger.branch(TreeLogger.Type.INFO, "Quarkus Super Dev Mode starting up");

        File baseCacheDir = computePreferredCacheDir(this.baseCachePath, options.getModuleNames(), startupLogger);

        UnitCache unitCache = UnitCacheSingleton.get(startupLogger, null, baseCacheDir, new CompilerOptionsImpl(options));

        MinimalRebuildCacheManager minimalRebuildCacheManager = createMinimalRebuildCacheManager(logger, options, baseCacheDir);

        OutboxTable outboxTable = makeOutboxTable(options, startupLogger, unitCache, minimalRebuildCacheManager);
        this.outboxTable = outboxTable;

        JobEventTable eventTable = new JobEventTable();

        JobRunner runner = new JobRunner(eventTable, minimalRebuildCacheManager);

        JsonExporter exporter = new JsonExporter(options, outboxTable);

        SourceHandler sourceHandler = new SourceHandler(outboxTable, exporter);

        SymbolMapHandler symbolMapHandler = new SymbolMapHandler(outboxTable);

        WebServer webServer = new WebServer(sourceHandler, symbolMapHandler, exporter, outboxTable, runner, eventTable, options.getBindAddress(), options.getPort());
        webServer.start(logger);

        return webServer;
    }

    public void refresh() {
        final TreeLogger logger = this.currentLogger != null ? this.currentLogger : new QuarkusTreeLogger(Logger.getLogger(this.getClass()));
        if (this.outboxTable != null) {
            try {
                this.outboxTable.defaultCompileAll(logger.branch(TreeLogger.Type.INFO, "Recompiling due to refreshed module source"));
            } catch (UnableToCompleteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static MinimalRebuildCacheManager createMinimalRebuildCacheManager(TreeLogger logger, Options options,File baseCacheDir) {
        return new MinimalRebuildCacheManager(
            logger,
            baseCacheDir,
            ImmutableMap.of(
            "style", options.getOutput().name(),
            "closureFormattedOutput", String.valueOf(options.isClosureFormattedOutput()),
            "generateJsInteropExports", String.valueOf(options.shouldGenerateJsInteropExports()),
            "exportFilters", options.getJsInteropExportFilter().toString(),
            "methodDisplayMode", options.getMethodNameDisplayMode().name()));
}

    private static OutboxTable makeOutboxTable(Options options, TreeLogger logger, UnitCache unitCache, MinimalRebuildCacheManager minimalRebuildCacheManager)
            throws IOException, UnableToCompleteException {

        File workDir = ensureWorkDir(options);
        logger.log(TreeLogger.Type.INFO, "workDir: " + workDir);

        LauncherDir launcherDir = LauncherDir.maybeCreate(options);

        int nextOutboxId = 1;
        OutboxTable outboxTable = new OutboxTable();
        for (String moduleName : options.getModuleNames()) {
            OutboxDir outboxDir = OutboxDir.create(new File(workDir, moduleName), logger);

            Recompiler recompiler = new Recompiler(outboxDir, launcherDir, moduleName,
                    options, unitCache, minimalRebuildCacheManager);

            // The id should be treated as an opaque string since we will change it again.
            // TODO: change outbox id to include binding properties.
            String outboxId = moduleName + "_" + nextOutboxId;
            nextOutboxId++;

            outboxTable.addOutbox(new Outbox(outboxId, recompiler, options, logger));
        }
        return outboxTable;
    }

    private static File ensureWorkDir(Options options) throws IOException {
        File workDir = options.getWorkDir();
        if (workDir == null) {
            workDir = Utility.makeTemporaryDirectory(null, "gwt-codeserver-");
        } else {
            if (!workDir.isDirectory()) {
                throw new IOException("workspace directory doesn't exist: " + workDir);
            }
        }
        return workDir;
    }

    public synchronized File computePreferredCacheDir(Path tmpDir, List<String> moduleNames, TreeLogger logger) {
        String tempDir = tmpDir.toAbsolutePath().toString();
        String currentWorkingDirectory = System.getProperty("user.dir");
        String preferredCacheDirName = "gwt-cache-" + StringUtils.toHexString(Md5Utils.getMd5Digest(currentWorkingDirectory + Joiner.on(", ").join(moduleNames)));

        File preferredCacheDir = new File(tempDir, preferredCacheDirName);
        if (!preferredCacheDir.exists() && !preferredCacheDir.mkdir()) {
            logger.log(TreeLogger.WARN, "Can't create cache directory: " + preferredCacheDir);
            return null;
        }
        return preferredCacheDir;
    }
}
