package io.github.chrisruffalo.qgwt.configuration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CodeServerConfiguration {

    /**
     * The port that the GWT code server will run on.
     */
    @ConfigItem(defaultValue = "9876")
    int port;

    /**
     * The address that the code server will bind to.
     */
    @ConfigItem(name="bind-address", defaultValue = "127.0.0.1")
    String bindAddress;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }
}
