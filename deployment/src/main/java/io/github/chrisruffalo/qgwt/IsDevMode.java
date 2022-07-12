package io.github.chrisruffalo.qgwt;

import io.quarkus.runtime.LaunchMode;

import java.util.function.BooleanSupplier;

public class IsDevMode implements BooleanSupplier {
    LaunchMode launchMode;

    public boolean getAsBoolean() {
        return launchMode == LaunchMode.DEVELOPMENT;
    }
}
