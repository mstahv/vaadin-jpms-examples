package com.vaadin.flow.plugin.jenesis;

import java.io.File;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.plugin.base.PluginAdapterBuild;
import com.vaadin.flow.server.frontend.BundleValidationUtil;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.MissingLicenseKeyException;

/**
 * What the prepare-frontend and build-frontend goals of the Vaadin Maven
 * plugin do, in that order, for any {@link PluginAdapterBuild}.
 * <p>
 * Copied from {@code PrepareFrontendMojo} and {@code BuildFrontendMojo},
 * without cleaning up the generated files. In
 * Flow, this would belong in flow-plugin-base, so that the Maven, Gradle and
 * Jenesis plugins share it.
 */
final class FrontendBuild {

    private FrontendBuild() {
    }

    static void run(PluginAdapterBuild adapter) throws Exception {
        long start = System.nanoTime();

        // prepare-frontend
        File tokenFile = BuildFrontendUtil.propagateBuildInfo(adapter);
        FrontendUtils.checkLegacyFrontendFolder(adapter.projectBaseDirectory());
        BuildFrontendUtil.prepareFrontend(adapter);

        // build-frontend
        boolean reactEnabled = adapter.isReactEnabled()
                && FrontendUtils.isReactRouterRequired(
                        BuildFrontendUtil.getFrontendDirectory(adapter));
        FeatureFlags featureFlags = new FeatureFlags(
                adapter.createLookup(adapter.getClassFinder()));
        if (adapter.javaResourceFolder() != null) {
            featureFlags.setPropertiesLocation(adapter.javaResourceFolder());
        }
        FrontendDependenciesScanner frontendDependencies = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(!adapter.optimizeBundle(),
                        adapter.getClassFinder(),
                        adapter.generateEmbeddableWebComponents(),
                        featureFlags, reactEnabled, true);

        BuildFrontendUtil.runNodeUpdater(adapter, frontendDependencies);

        if (adapter.generateBundle() && BundleValidationUtil
                .needsBundleBuild(adapter.servletResourceOutputDirectory())) {
            // Unlike the Maven plugin, the generated files and node_modules
            // are kept: they are in the step's own persistent folder, not in
            // the project, and the next build reuses them
            BuildFrontendUtil.runFrontendBuild(adapter);
        }

        LicenseChecker.setStrictOffline(true);
        boolean licenseRequired;
        boolean commercialBannerRequired;
        try {
            licenseRequired = BuildFrontendUtil.validateLicenses(adapter,
                    frontendDependencies);
            commercialBannerRequired = false;
        } catch (MissingLicenseKeyException e) {
            licenseRequired = true;
            commercialBannerRequired = true;
            adapter.logInfo(e.getMessage());
        }
        BuildFrontendUtil.updateBuildFile(adapter, licenseRequired,
                commercialBannerRequired);

        adapter.logInfo("Built the Vaadin frontend in "
                + (System.nanoTime() - start) / 1_000_000 + " ms, token "
                + tokenFile);
    }
}
