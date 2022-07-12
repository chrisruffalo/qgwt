package io.github.chrisruffalo.qgwt.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@ConfigRoot(name = "gwt", phase = ConfigPhase.BUILD_TIME)
public class GwtConfiguration {

    /**
     * List of modules that will be built and included as part of the GWT project
     */
    @ConfigItem(defaultValue = "")
    List<String> modules = new LinkedList<>();

    /**
     * Where to look for the main source root. This will be used to pull java
     * source files for live-reload.
     */
    @ConfigItem(name="source-root", defaultValue = "src/main/java")
    String sourceRoot;

    /**
     * List of source roots for the GWT application
     */
    @ConfigItem(name="additional-source-roots", defaultValue = "")
    Optional<List<String>> sourceRoots = Optional.of(new LinkedList<>());

    /**
     * The classes dir that will be used during build time to add
     * items to the classpath for the compiler or code server.
     *
     * With maven, for example, the value is `target/classes`.
     */
    @ConfigItem(name="classes-dir", defaultValue = "target/classes")
    String classesDir;

    /**
     * The temporary directory for handling gwt/qgwt required files during
     * compilation or code server use. Defaults to the appropriate value for
     * maven ('target/qgwt').
     */
    @ConfigItem(name="temp-dir", defaultValue = "target/qgwt")
    String tempDir;

    CodeServerConfiguration codeServer;

    public List<String> getModules() {
        return modules;
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    public Optional<List<String>> getSourceRoots() {
        return sourceRoots;
    }

    public void setSourceRoots(Optional<List<String>> sourceRoots) {
        this.sourceRoots = sourceRoots;
    }

    public String getClassesDir() {
        return classesDir;
    }

    public void setClassesDir(String classesDir) {
        this.classesDir = classesDir;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public CodeServerConfiguration getCodeServer() {
        return codeServer;
    }

    public void setCodeServer(CodeServerConfiguration codeServer) {
        this.codeServer = codeServer;
    }

    public String getSourceRoot() {
        return sourceRoot;
    }

    public void setSourceRoot(String sourceRoot) {
        this.sourceRoot = sourceRoot;
    }
}
