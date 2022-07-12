package io.github.chrisruffalo.qgwt;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public abstract class Extractor {

    private Logger logger;

    public Extractor() {
        this.logger = Logger.getLogger(this.getClass());
    }

    protected void processStreamToDirectory(final String inputArchive, final Stream<Path> stream, final Path targetRoot) {
        stream
            .filter(Files::isRegularFile)
            .forEach(sourceFile -> {
                String relative = sourceFile.toString();
                while (relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                final Path target = targetRoot.resolve(relative);
                try {
                    Files.createDirectories(target.getParent());
                } catch (IOException e) {
                    this.logger.errorf("Could not create directories for target: %s", e.getMessage());
                    return;
                }
                try {
                    if(!sourceFile.toString().endsWith(".class")) {
                        this.logger.tracef("Copying %s to %s", inputArchive + "!" + sourceFile, target);
                        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException | ProviderMismatchException e) {
                    this.logger.errorf("Could not copy %s to %s: %s", inputArchive + "!" + sourceFile, target, e.getMessage());
                }
            });

    }

}
