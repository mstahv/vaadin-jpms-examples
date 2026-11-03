package com.vaadin.flow.plugin.jenesis;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.plugin.base.PluginAdapterBuild;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;

/**
 * Maps the folders of a Jenesis build step to the ones the Vaadin frontend
 * build expects, with the defaults of the Vaadin Maven plugin.
 * <p>
 * The project folder of the frontend build is the step's persistent folder:
 * the generated frontend files and the frontend tool installation stay there
 * between builds. The output goes into the {@code classes} folder the step
 * hands to the jar.
 * <p>
 * Options, from {@code plugin-vaadin.properties}:
 * <ul>
 * <li>{@code react}: {@code true} or {@code false}, by default whether React
 * Router is needed</li>
 * <li>{@code pnpm}: {@code true} to install with pnpm instead of npm</li>
 * <li>{@code node.version}: the Node.js version to install when none is
 * found</li>
 * </ul>
 */
final class JenesisPluginAdapter implements PluginAdapterBuild, AutoCloseable {

    private final Path work;
    private final Path classes;
    private final List<Path> jars;
    private final Path output;
    private final Map<String, String> options;
    private URLClassLoader classLoader;
    private ClassFinder classFinder;

    JenesisPluginAdapter(Path work, Path classes, List<Path> jars, Path output,
            Map<String, String> options) {
        this.work = work;
        this.classes = classes;
        this.jars = List.copyOf(jars);
        this.output = output;
        this.options = Map.copyOf(options);
    }

    private File workFile(String path) {
        return work.resolve(path).toFile();
    }

    private File outputFile(String path) {
        return output.resolve(path).toFile();
    }

    @Override
    public ClassFinder getClassFinder() {
        if (classFinder == null) {
            List<URL> urls = new ArrayList<>();
            try {
                urls.add(classes.toUri().toURL());
                for (Path jar : jars) {
                    urls.add(jar.toUri().toURL());
                }
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
            URL[] array = urls.toArray(URL[]::new);
            // The module's class path on top of the plugin's: the frontend
            // build finds classes by Flow's annotations, which have to be
            // the plugin's own classes, so Flow is loaded from the plugin
            classLoader = new URLClassLoader(array,
                    JenesisPluginAdapter.class.getClassLoader());
            classFinder = new ReflectionsClassFinder(classLoader, array);
        }
        return classFinder;
    }

    /**
     * Jenesis names the jars it resolves after their module names, so a
     * dependency is checked through a class it contains. Only the license
     * checker is checked by the frontend build.
     */
    @Override
    public boolean checkRuntimeDependency(String groupId, String artifactId,
            Consumer<String> missingDependencyMessage) {
        if (!"com.vaadin".equals(groupId)
                || !"license-checker".equals(artifactId)) {
            return true;
        }
        try {
            getClassFinder()
                    .loadClass("com.vaadin.pro.licensechecker.LicenseChecker");
            return true;
        } catch (ClassNotFoundException e) {
            if (missingDependencyMessage != null) {
                missingDependencyMessage.accept("Add requires "
                        + "com.vaadin.pro.licensechecker to module-info.java");
            }
            return false;
        }
    }

    @Override
    public Set<File> getJarFiles() {
        Set<File> files = new LinkedHashSet<>();
        jars.forEach(jar -> files.add(jar.toFile()));
        return files;
    }

    @Override
    public void close() throws IOException {
        if (classLoader != null) {
            classLoader.close();
        }
    }

    @Override
    public File frontendResourcesDirectory() {
        return workFile(Constants.LOCAL_FRONTEND_RESOURCES_PATH);
    }

    @Override
    public boolean generateBundle() {
        return true;
    }

    @Override
    public boolean generateEmbeddableWebComponents() {
        return true;
    }

    @Override
    public boolean optimizeBundle() {
        return true;
    }

    @Override
    public boolean runNpmInstall() {
        return true;
    }

    @Override
    public boolean ciBuild() {
        return false;
    }

    @Override
    public boolean forceProductionBuild() {
        return false;
    }

    @Override
    public boolean compressBundle() {
        return true;
    }

    @Override
    public File resourcesOutputDirectory() {
        return outputFile(Constants.META_INF + "resources/");
    }

    @Override
    public File applicationProperties() {
        return workFile("src/main/resources/application.properties");
    }

    @Override
    public boolean eagerServerLoad() {
        return false;
    }

    @Override
    public File frontendDirectory() {
        return workFile("src/main/" + FrontendUtils.FRONTEND);
    }

    @Override
    public File generatedTsFolder() {
        return new File(frontendDirectory(), FrontendUtils.GENERATED);
    }

    @Override
    public boolean isJarProject() {
        return true;
    }

    @Override
    public boolean isDebugEnabled() {
        return Boolean.parseBoolean(options.get("debug"));
    }

    @Override
    public File javaSourceFolder() {
        return workFile("src/main/java");
    }

    @Override
    public File javaResourceFolder() {
        return workFile("src/main/resources");
    }

    @Override
    public void logDebug(CharSequence debugMessage) {
        if (isDebugEnabled()) {
            System.out.println("[vaadin] " + debugMessage);
        }
    }

    @Override
    public void logDebug(CharSequence debugMessage, Throwable throwable) {
        logDebug(debugMessage);
        if (isDebugEnabled()) {
            throwable.printStackTrace(System.out);
        }
    }

    @Override
    public void logInfo(CharSequence infoMessage) {
        System.out.println("[vaadin] " + infoMessage);
    }

    @Override
    public void logWarn(CharSequence warningMessage) {
        System.out.println("[vaadin] WARNING: " + warningMessage);
    }

    @Override
    public void logError(CharSequence errorMessage) {
        System.err.println("[vaadin] ERROR: " + errorMessage);
    }

    @Override
    public void logWarn(CharSequence warningMessage, Throwable throwable) {
        logWarn(warningMessage + ": " + throwable);
    }

    @Override
    public void logError(CharSequence warning, Throwable e) {
        logError(warning + ": " + e);
    }

    @Override
    public URI nodeDownloadRoot() throws URISyntaxException {
        String root = options.get("node.download.root");
        return root == null ? URI.create(
                com.vaadin.flow.server.frontend.installer.NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
                : new URI(root);
    }

    @Override
    public String nodeVersion() {
        return options.getOrDefault("node.version",
                FrontendTools.DEFAULT_NODE_VERSION);
    }

    @Override
    public File npmFolder() {
        return work.toFile();
    }

    @Override
    public File openApiJsonFile() {
        return workFile("target/generated-resources/openapi.json");
    }

    @Override
    public boolean pnpmEnable() {
        return Boolean.parseBoolean(options.getOrDefault("pnpm",
                String.valueOf(Constants.ENABLE_PNPM_DEFAULT)));
    }

    @Override
    public boolean bunEnable() {
        return Constants.ENABLE_BUN_DEFAULT;
    }

    @Override
    public boolean useGlobalPnpm() {
        return Constants.GLOBAL_PNPM_DEFAULT;
    }

    @Override
    public Path projectBaseDirectory() {
        return work;
    }

    @Override
    public boolean requireHomeNodeExec() {
        return Constants.DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE;
    }

    @Override
    public String nodeFolder() {
        return null;
    }

    @Override
    public File servletResourceOutputDirectory() {
        return outputFile(Constants.VAADIN_SERVLET_RESOURCES);
    }

    @Override
    public File webpackOutputDirectory() {
        return outputFile(Constants.VAADIN_WEBAPP_RESOURCES);
    }

    @Override
    public File frontendOutputDirectory() {
        return webpackOutputDirectory();
    }

    @Override
    public String buildFolder() {
        return "target";
    }

    @Override
    public List<String> postinstallPackages() {
        return Collections.emptyList();
    }

    @Override
    public List<String> excludePostinstallPackages() {
        return Collections.emptyList();
    }

    @Override
    public boolean isFrontendHotdeploy() {
        return false;
    }

    @Override
    public boolean skipDevBundleBuild() {
        return false;
    }

    @Override
    public boolean isPrepareFrontendCacheDisabled() {
        return false;
    }

    @Override
    public boolean isReactEnabled() {
        String react = options.get("react");
        if (react != null) {
            return Boolean.parseBoolean(react);
        }
        return FrontendUtils.isReactRouterRequired(
                BuildFrontendUtil.getFrontendDirectory(this));
    }

    @Override
    public String applicationIdentifier() {
        return "app-" + StringUtil.getHash(classes.toString(),
                StandardCharsets.UTF_8);
    }

    @Override
    public List<String> frontendExtraFileExtensions() {
        return Collections.emptyList();
    }

    @Override
    public boolean isNpmExcludeWebComponents() {
        return false;
    }

    @Override
    public boolean isFrontendIgnoreVersionChecks() {
        return false;
    }
}
