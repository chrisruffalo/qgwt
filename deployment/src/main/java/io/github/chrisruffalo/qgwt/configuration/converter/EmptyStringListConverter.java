package io.github.chrisruffalo.qgwt.configuration.converter;

import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.Converters;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Adapted from the smallrye collection converter to return an empty collection instead of a null collection
 */
public class EmptyStringListConverter implements Converter<List<String>> {

    private final Converter<List<String>> delegate;

    public EmptyStringListConverter() {
        this.delegate = Converters.newCollectionConverter(new TrimmedStringConverter(), size -> new LinkedList<>());
    }

    @Override
    public List<String> convert(String s) throws IllegalArgumentException, NullPointerException {
        if (s == null || s.trim().isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> converted = this.delegate.convert(s);
        if (converted == null) {
            return Collections.emptyList();
        }

        return converted;
    }
}
