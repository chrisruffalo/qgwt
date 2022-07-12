package io.github.chrisruffalo.qgwt;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

public class JarExtractor extends Extractor {

    private static final Logger LOGGER = Logger.getLogger(ModuleExtractor.class);

    public void extract(final URL resourceInJar, final Path targetRoot) {
        // attempt to resolve in filesystem
        Path resourcePath;
        URI resourceUri = null;
        try {
            resourceUri = resourceInJar.toURI();
            resourcePath = Paths.get(resourceUri);
        } catch (URISyntaxException uex) {
            throw new RuntimeException(uex);
        } catch (FileSystemNotFoundException fsne) {
            resourcePath = null;
        }

        // see if the path is on disk
        if (resourcePath != null) {
            // don't worry about it, this is already somewhere on disk
            return;
        }

        if (!resourceUri.toString().startsWith("jar") || !resourceUri.toString().contains("!")) {
            // we did not resolve a jar resource
            return;
        }

        final String[] split = resourceUri.toString().split("!");

        // load jar filesystem
        try (final FileSystem fs = FileSystems.newFileSystem(new URI(split[0]), new HashMap<>())) {
            LOGGER.debugf("Extracting source path zip: %s to target %s", split[0], targetRoot);
            try (Stream<Path> stream = Files.walk(fs.getPath("/"))) {
                processStreamToDirectory(split[0], stream, targetRoot);
            }
        } catch (URISyntaxException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
