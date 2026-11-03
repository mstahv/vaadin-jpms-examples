package com.vaadin.flow.plugin.jenesis;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import build.jenesis.BuildStep;
import build.jenesis.BuildStepArgument;
import build.jenesis.BuildStepContext;
import build.jenesis.BuildStepResult;
import build.jenesis.step.Dependencies;

/**
 * Builds the production frontend bundle of a module.
 * <p>
 * The step reads the compiled classes of the module and the jars it depends
 * on, and writes the bundle and the build info file under
 * {@code classes/META-INF/VAADIN}, which joins the module's jar. The frontend
 * tool installation (node_modules) is kept in the step's persistent folder
 * between builds.
 *
 * @param options
 *            the options from {@code plugin-vaadin.properties}, see
 *            {@link JenesisPluginAdapter}
 */
public record FrontendBuildStep(
        LinkedHashMap<String, String> options) implements BuildStep {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public CompletionStage<BuildStepResult> apply(Executor executor,
            BuildStepContext context,
            SequencedMap<String, BuildStepArgument> arguments)
            throws IOException {
        // Jenesis hands relative folders, the frontend build needs absolute
        Path classes = arguments.firstEntry().getValue().folder()
                .resolve(CLASSES).toAbsolutePath();
        List<Path> jars = new ArrayList<>();
        for (BuildStepArgument argument : arguments.values()) {
            if (!argument.removed()) {
                for (Path jar : Dependencies.select(argument.folder(), "main",
                        "compile")) {
                    jars.add(jar.toAbsolutePath());
                }
            }
        }
        jars.add(flowClientJar());
        Path output = Files
                .createDirectories(context.next().resolve(CLASSES))
                .toAbsolutePath();
        Path work = Files.createDirectories(context.persistent())
                .toAbsolutePath();

        try (JenesisPluginAdapter adapter = new JenesisPluginAdapter(work,
                classes, jars, output, options)) {
            FrontendBuild.run(adapter);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("The Vaadin frontend build failed", e);
        }
        return CompletableFuture.completedStage(new BuildStepResult(true));
    }

    /**
     * The flow-client jar of the plugin's own class path: the frontend build
     * needs the client engine, the application does not.
     */
    private static Path flowClientJar() throws IOException {
        URL resource = FrontendBuildStep.class.getClassLoader()
                .getResource("META-INF/frontend/FlowClient.js");
        if (resource == null || !"jar".equals(resource.getProtocol())) {
            throw new IOException("flow-client is not on the class path of"
                    + " the plugin: " + resource);
        }
        String jar = resource.getPath().substring(0,
                resource.getPath().indexOf("!/"));
        return Path.of(URI.create(jar));
    }
}
