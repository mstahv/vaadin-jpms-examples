package com.vaadin.flow.plugin.jenesis;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

import build.jenesis.BuildExecutor;
import build.jenesis.BuildExecutorModule;
import build.jenesis.BuildModuleFactory;
import build.jenesis.Environment;
import build.jenesis.Permits;
import build.jenesis.Repository;
import build.jenesis.Resolver;

/**
 * Adds the Vaadin frontend build to a module of a Jenesis build.
 * <p>
 * Declared in {@code jenesis.plugins.properties} at the {@code binary/transform}
 * hook point, where the step gets the compiled classes of the module and its
 * dependencies:
 *
 * <pre>
 * vaadin+binary/transform=maven/com.example.jpms/jenesis-vaadin-plugin
 * </pre>
 *
 * It runs in the modules that have a {@code build.jenesis/plugin-vaadin.properties}
 * file, which may set the options of {@link FrontendBuildStep}.
 */
public class VaadinPluginFactory implements BuildModuleFactory {

    @Override
    public BuildExecutorModule create(Environment environment,
            SequencedMap<String, String> properties, Permits permits,
            String group, Map<String, Repository> repositories,
            Map<String, Resolver> resolvers) {
        FrontendBuildStep step = new FrontendBuildStep(
                new LinkedHashMap<>(properties));
        return new BuildExecutorModule() {
            @Override
            public void accept(BuildExecutor executor,
                    SequencedMap<String, Path> inherited) {
                executor.addStep("vaadin-frontend", step,
                        inherited.sequencedKeySet());
            }
        };
    }
}
