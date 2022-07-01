package io.github.chrisruffalo.qgwt;

import com.google.gwt.dev.DevMode;
import com.google.gwt.dev.codeserver.CodeServer;
import com.google.gwt.dev.codeserver.Options;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import org.jboss.logging.Logger;

public class DevModeGwtStarter {

    private static final Logger LOGGER = Logger.getLogger(DevModeGwtStarter.class);

    @BuildStep
    public FeatureBuildItem startDevModeGwt(LaunchModeBuildItem launchMode) {
        // only start the dev mode in dev mode
        if (!LaunchMode.DEVELOPMENT.equals(launchMode.getLaunchMode())) {
            return null;
        }

        LOGGER.info("Spawning GWT Dev Mode...");
        new Thread(() -> {
            DevMode.main(
                // for reference: https://www.gwtproject.org/doc/latest/DevGuideCompilingAndDebugging.html#What_options_can_be_passed_to_development_mode
                new String[]{
                    "-noserver",
                    "-war",
                    "src/main/resources/META-INF/resources",
                    "io.github.chrisruffalo.qgwt.example.Ui"
                }
            );
        }).start();

        return new FeatureBuildItem("gwt-dev-mode");
    }

}
