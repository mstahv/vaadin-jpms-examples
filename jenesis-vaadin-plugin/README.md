# Vaadin frontend build for Jenesis

A [Jenesis](https://jenesis.build/) build plugin that builds the production frontend of a Vaadin module. It is a
prototype: it does what the `prepare-frontend` and `build-frontend` goals of the Vaadin Maven plugin do, with the
code the Maven and Gradle plugins share, `flow-plugin-base`.

```sh
mvn install          # installs com.example.jpms:jenesis-vaadin-plugin:1.0-SNAPSHOT
```

## Using it

`jenesis.plugins.properties` adds the plugin to the module build at the `binary/transform` hook point, where a step
gets the compiled classes of the module and the jars it depends on:

```properties
vaadin+binary/transform=maven/com.example.jpms/jenesis-vaadin-plugin
```

It runs in the modules that have `build.jenesis/plugin-vaadin.properties`. The file may be empty, or set:

| Option | Default |
|---|---|
| `react` | whether React Router is needed, as in the Maven plugin |
| `pnpm` | `false`: npm installs the frontend packages |
| `node.version` | the Node.js version of Flow, installed when none is found |
| `node.download.root` | nodejs.org |
| `debug` | `false` |

A SNAPSHOT of the plugin has to be pinned in `module-info.java`:

```java
@jenesis.pin plugin.vaadin/maven/com.example.jpms/jenesis-vaadin-plugin 1.0-SNAPSHOT
```

See [`../02-jenesis`](../02-jenesis) for a complete project.

## How it works

- `VaadinPluginFactory` is found through `META-INF/services` and adds one step, `FrontendBuildStep`, to the module.
- `FrontendBuildStep` collects the compiled classes and the dependency jars, and runs `FrontendBuild` with a
  `JenesisPluginAdapter`, the `PluginAdapterBuild` implementation that maps the Jenesis folders to the ones the
  frontend build expects.
- The project folder of the frontend build is the step's persistent folder. The generated frontend files and
  `node_modules` stay there between builds, so the Maven plugin's cleanup of generated files is left out.
- The output goes under `classes/META-INF/VAADIN` and `classes/META-INF/resources`, which join the module's jar.
- Jenesis runs the step again only when its inputs change. When the frontend is unchanged, as after a change to Java
  code only, Flow's bundle validation keeps the previous bundle: about 0.5 s instead of 7 s.
- `flow-client` is a dependency of the plugin, not of the application, as the application needs it only for the
  frontend build ([vaadin/flow#25882](https://github.com/vaadin/flow/pull/25882)).

## What it took

- **The same classes for Flow.** The frontend build finds classes by Flow's annotations, for example
  `@JsDefinition` and `@JsExpression`, and compares them by identity. With the module's class path in a class
  loader of its own, Flow was loaded twice, the JS definitions were missing from the bundle, and the page stayed
  blank. The module's class path is now on top of the plugin's class loader, so Flow comes from the plugin. The
  Maven plugin solves the same problem by running the build in the project's class loader (`Reflector`).
- **Absolute folders.** Jenesis hands a step relative folders, which the frontend build resolves against its project
  folder a second time.
- **A logging backend.** The frontend build logs through slf4j, so the plugin brings `slf4j-simple`. Without it,
  the output of npm and Vite is lost.

## What would belong in Flow

- `FrontendBuild` is copied from `PrepareFrontendMojo` and `BuildFrontendMojo`, and the Gradle plugin has its own copy
  too. One method in `flow-plugin-base`, such as `BuildFrontendUtil.buildFrontend(adapter)`, would let all three
  share it.
- Most of `JenesisPluginAdapter` are the Maven plugin's defaults. Default methods in `PluginAdapterBase` would leave
  only the folders to an adapter.
- Development mode is not covered.
