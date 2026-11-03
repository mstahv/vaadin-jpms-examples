# Vaadin on the Java module path: examples

Examples that try out Java module (JPMS) support for Vaadin Flow, as discussed in
[vaadin/flow#7482](https://github.com/vaadin/flow/issues/7482). Each one is an application that is a named module
and runs from the module path, built in a different way:

| Example | Build | Runs on |
|---|---|---|
| [`01-jetty`](01-jetty) | Maven | embedded Jetty, plain `java -p … -m …` |
| [`02-jenesis`](02-jenesis) | [Jenesis](https://jenesis.build/), with `module-info.java` as the only build description and the frontend built by [`jenesis-vaadin-plugin`](jenesis-vaadin-plugin) | embedded Jetty |
| [`03-vidocq`](03-vidocq) | Maven, a branch of [VidocqTools/vaadin-on-vidocq](https://codefloe.com/VidocqTools/vaadin-on-vidocq), as a Git submodule | the [Vidocq](https://vidocq.dev) runtime, also as a `jlink` image |
| [`04-quarkus`](04-quarkus) | Maven, Quarkus 4.0.0.Beta2 with `quarkus-jlink` | Quarkus as a `jlink` image, which does not work yet without patches |

They use Vaadin built from these branches, which are not released:

- `feature/jpms` of vaadin/flow: module descriptors for flow-server, flow-data, flow-html-components, flow-dnd,
  flow-lit-template, flow-react and flow-push, and `Automatic-Module-Name` for the other Flow jars.
- `feature/jpms-amn` of vaadin/flow-components: `Automatic-Module-Name` for the component jars.
- `feature/jpms-amn` of vaadin/atmosphere ([vaadin/atmosphere#19](https://github.com/vaadin/atmosphere/pull/19)): the
  module name `com.vaadin.external.atmosphere.runtime` for Vaadin's Atmosphere fork.

### Pull requests

| Repository | Pull request | What |
|---|---|---|
| vaadin/flow | [#26731](https://github.com/vaadin/flow/pull/26731) | `Automatic-Module-Name` for the Flow jars, the first step of `feature/jpms` |
| vaadin/flow | [#26721](https://github.com/vaadin/flow/pull/26721) | removes the gentyref dependency, which `feature/jpms` is based on (merged) |
| vaadin/flow | [#26837](https://github.com/vaadin/flow/pull/26837) | runs Flow without the `java.desktop` module, a related improvement for `jlink` images, see finding 8 |
| vaadin/flow-components | [#10527](https://github.com/vaadin/flow-components/pull/10527) | `Automatic-Module-Name` for the component jars |
| vaadin/atmosphere | [#19](https://github.com/vaadin/atmosphere/pull/19) | the module name of Vaadin's Atmosphere fork |
| vaadin/open | [#19](https://github.com/vaadin/open/pull/19) | `Automatic-Module-Name` for `com.vaadin.open` |
| vaadin/license-checker-vaadin10 | [#268](https://github.com/vaadin/license-checker-vaadin10/pull/268) (a private repository) | `Automatic-Module-Name` for `com.vaadin.pro.licensechecker` |

`feature/jpms` itself, the module descriptors, has no pull request yet.

Clone with the submodule:

```sh
git clone --recurse-submodules https://github.com/mstahv/vaadin-jpms-examples.git
```

In a clone made without it, `git submodule update --init` fetches `03-vidocq`.

## 1. Build Vaadin into the local Maven repository

```sh
./build-snapshots.sh
```

The script clones the branches into `~/.cache/vaadin-jpms-examples`, sets their version to `25.4-jpms-SNAPSHOT` and
installs these modules into `~/.m2/repository`. The Atmosphere fork is installed as `3.0.5.slf4jvaadin1-jpms`, and
Flow is built with it, requiring it by its module name: the two changes Flow makes once the fork is released. A version of their own keeps them apart from other local
`25.4-SNAPSHOT` builds, and the remote `25.4-SNAPSHOT` that Maven downloads once a day can not replace them.

| Artifact | Module name | Kind |
|---|---|---|
| `flow-server` | `com.vaadin.flow.server` | explicit |
| `flow-push` | `com.vaadin.flow.push` | explicit, no packages |
| `flow-data` | `com.vaadin.flow.data` | explicit |
| `flow-html-components` | `com.vaadin.flow.component.html` | explicit |
| `flow-dnd` | `com.vaadin.flow.component.dnd` | explicit |
| `flow-lit-template` | `com.vaadin.flow.component.littemplate` | explicit |
| `flow-react` | `com.vaadin.flow.component.react` | explicit |
| `flow-client` | `com.vaadin.flow.client` | automatic, needed by the frontend build only |
| `flow-bom`, `flow-maven-plugin` and their parents | | |
| `atmosphere-runtime` (`com.vaadin.external.atmosphere`) | `com.vaadin.external.atmosphere.runtime` | automatic |
| `vaadin-flow-components-base` | `com.vaadin.flow.component.shared` | automatic |
| `vaadin-button-flow` | `com.vaadin.flow.component.button` | automatic |
| `vaadin-text-field-flow` | `com.vaadin.flow.component.textfield` | automatic |
| `vaadin-ordered-layout-flow` | `com.vaadin.flow.component.orderedlayout` | automatic |
| `vaadin-lumo-theme`, `vaadin-aura-theme` | `com.vaadin.flow.theme.lumo`, `com.vaadin.flow.theme.aura` | automatic |

The modules the components need for their own build and tests, such as `flow-test-util`, are installed too.
Settings, all optional:

| Variable | Default |
|---|---|
| `FLOW_SOURCE`, `FLOW_BRANCH` | vaadin/flow on GitHub, `feature/jpms` |
| `COMPONENTS_SOURCE`, `COMPONENTS_BRANCH` | vaadin/flow-components on GitHub, `feature/jpms-amn` |
| `ATMOSPHERE_SOURCE`, `ATMOSPHERE_BRANCH`, `ATMOSPHERE_VERSION` | vaadin/atmosphere on GitHub, `feature/jpms-amn`, `3.0.5.slf4jvaadin1-jpms` |
| `SKIP_ATMOSPHERE=1` | build Flow with the released fork, which flow-server then requires as `atmosphere.runtime` |
| `JPMS_VERSION` | `25.4-jpms-SNAPSHOT` |
| `SKIP_FLOW=1`, `SKIP_COMPONENTS=1` | build both |

A `*_SOURCE` may be a local clone, to try a change before it is pushed: `FLOW_SOURCE=~/git/flow ./build-snapshots.sh`
builds the `feature/jpms` branch of that clone.

Requirements: JDK 25 (Jenesis needs it, the others run on 21), Maven 3.9. The frontend builds download Node by
themselves when none is installed.

To remove the builds again:

```sh
rm -rf ~/.m2/repository/com/vaadin/*/25.4-jpms-SNAPSHOT \
    ~/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/3.0.5.slf4jvaadin1-jpms
```

## 2. Run the examples

All of them serve http://localhost:8080 in production mode.

**01-jetty**: `01-jetty/run.sh` builds the application and starts it with `java -p target/modules:… -m …`.

**02-jenesis**: the frontend is built by a Jenesis plugin, a prototype in
[`jenesis-vaadin-plugin`](jenesis-vaadin-plugin) that uses the code of the Vaadin Maven and Gradle plugins. Install
it once, then build and run with Jenesis:

```sh
(cd jenesis-vaadin-plugin && mvn install)
cd 02-jenesis
java build/jenesis/Execute.java     # resolve, compile, build the frontend, package, run
```

**03-vidocq**: a submodule, the branch `feature/vaadin-jpms` of
[mstahv/vaadin-on-vidocq](https://codefloe.com/mstahv/vaadin-on-vidocq), a fork of VidocqTools/vaadin-on-vidocq, meant
to be contributed. Its README tells the rest:

```sh
cd 03-vidocq
./mvnw clean package vidocq:package && target/vaadin-on-vidocq-*/bin/vaadin-on-vidocq.sh
./mvnw -Pjlink clean package vidocq:jlink && JDK_JAVA_OPTIONS=-Dvaadin.productionMode=true target/dist/bin/vaadin-on-vidocq
```

**04-quarkus**: Quarkus 4 packaged with `jlink`, an attempt that documents what is left to solve. Its
[README](04-quarkus/README.md) tells how to build, patch and run the image:

```sh
cd 04-quarkus
mvn package && ./patch-image.sh && target/jlink-output/image/bin/jpms-example-quarkus-1.0-SNAPSHOT
```

## Results

Checked on 2026-11-03 with a browser, against `feature/jpms` at `1eff6ae6ca` and `feature/jpms-amn` of
flow-components at `e6c18200bd`. `03-vidocq` was checked again at `16d0468df7`, without `vidocq.app.layer=all`.

| | 01-jetty | 02-jenesis | 03-vidocq package | 03-vidocq `jlink` |
|---|---|---|---|---|
| Application and Flow run as explicit modules | ✅ | ✅ | ✅ | ✅ |
| Jackson and jsoup need no `requires` of the application | ✅ | ✅ | ✅ | ✅ |
| Server round trip | ✅ | ✅ | ✅ | ✅ |
| Push (long polling) | ✅ | ✅ | not tried | not tried |
| A Vaadin component (`Button`) | not used | ✅ | ✅ | ✅ |
| `AppShellConfigurator` | ✅ | ✅ | ✅ | ✅, no Vidocq Vaadin extension |
| Workarounds | none | module name aliases, as the names are not released | none | `vauban:modularize` for the components and Atmosphere |

The views show the module of each part, so the result is visible on the page.

`04-quarkus` was checked on 2026-11-03 at `16d0468df7`, with Quarkus 4.0.0.Beta2. The `jlink` image starts only with
its module descriptors patched, the Flow modules are explicit modules in it and the server round trip works, but push
does not. The fast-jar of the same application works, push included. See [its README](04-quarkus/README.md).

## Findings

What the examples show about the current changes, and what is left to refine:

1. **Vidocq needs fewer workarounds.** Compared with the released Vaadin, the `jlink` image of `03-vidocq` needs
   neither `vidocq.jlink.mergeSharedResources` nor `vauban.modularize.forceExplicit`. An app shell works without
   `vidocq-runtime-vaadin-extension`, and the limit on Collaboration Engine and LitTemplate is gone. These are
   items 1, 2, 3 and 4 of [Vidocq/vidocq#256](https://codefloe.com/Vidocq/vidocq/issues/256).
2. **File-name based module names break as soon as a tool renames a jar.** With the released Atmosphere fork,
   flow-server requires it as `atmosphere.runtime`. Jenesis stores a jar that declares no module name under its
   coordinates, so the module was not found, and `02-jenesis` needed `@jenesis.alias atmosphere.runtime …`. With
   vaadin/atmosphere#19 and flow-server requiring `com.vaadin.external.atmosphere.runtime`, all three examples run
   without it. Flow can make that change once the fork is released. The same applies to license-checker.
3. **The application resources are found with Flow in a parent layer.** Flow used to look them up through its own
   class loader only, so Vidocq needed `vidocq.app.layer=all`: with Flow in a parent layer, every request answered
   500 with *Unable to find index.html*. Flow now looks them up through the class loader of the servlet context
   first (`16d0468df7` on `feature/jpms`), and `03-vidocq` runs with the application alone in its layer under
   `vidocq:dev`, `vidocq:run` and `vidocq:package`. The same applies to an EAR with Flow in its `lib` folder. This
   is item 5 of Vidocq/vidocq#256.
4. **The frontend build needs `flow-client`.** Every example depends on it although nothing uses it at runtime,
   until [vaadin/flow#25882](https://github.com/vaadin/flow/pull/25882) makes it a dependency of the build.
5. **The frontend build works outside Maven and Gradle.** `jenesis-vaadin-plugin` runs it in a Jenesis build with
   `flow-plugin-base`, the code the Maven and Gradle plugins share, including the npm packages of the components. It
   needed a copy of the orchestration of `PrepareFrontendMojo` and `BuildFrontendMojo`, and a class loader setup
   where Flow's annotation classes are the plugin's own. Moving both into `flow-plugin-base` would make such plugins
   small, see [its README](jenesis-vaadin-plugin/README.md).
6. **The components are still automatic modules**, so `vauban:modularize` generates descriptors for them in a `jlink`
   image. Module descriptors for the components would remove that step.
7. **Not related to the module path:** in the `jlink` image of `03-vidocq`, the theme stylesheet `aura/aura.css`
   answers 404. Vidocq's servlet container serves no `META-INF/resources` from the modules of an image. The same
   happens with the released Vaadin.
8. **A `jlink` image of a Flow application carries `java.desktop`**, which is about a quarter of an image: AWT, Swing,
   `java.beans` and their native libraries, with `java.prefs` and `java.datatransfer`. Flow needs it only for PWA icons
   drawn at run time, but could not start without it, because classes every application loads refer to `java.beans`
   and AWT. [vaadin/flow#26837](https://github.com/vaadin/flow/pull/26837) removes those references. On top of
   `feature/jpms`, with flow-server requiring `java.desktop` as `requires static`, the `jlink` image of `03-vidocq`
   shrank from 71.2 MB to 55.8 MB and runs without the module. To try it, merge the pull request into `feature/jpms`,
   make that change in `flow-server/src/main/java/module-info.java` and pass the clone as `FLOW_SOURCE` to
   `build-snapshots.sh`. The same pull request lists bean properties in the order of their fields, which changes the
   order of the columns of `Grid(Class)`.

9. **Quarkus 4 can package a Vaadin application with `jlink`, but not yet without patches.** `04-quarkus` stops on
   four problems: Quarkus takes the `META-INF/VAADIN` folder of the frontend bundle for a package, the Undertow of
   Quarkus uses packages that its module descriptors leave private, Atmosphere does not read `jakarta.annotation`,
   and the push script of flow-push answers 404. Push without Atmosphere,
   [vaadin/flow#26785](https://github.com/vaadin/flow/pull/26785), would remove the last two. vaadin-quarkus also
   needs a small change for Quarkus 4 and has no module name yet.

## Spring Boot, Quarkus and native images

[ECOSYSTEM.md](ECOSYSTEM.md) looks at what this work means for Spring Boot, Quarkus and GraalVM native images. Spring
stays on the class path, Quarkus is building modular packaging and `jlink` images for Quarkus 4, and native images gain
from dropping the run time use of AWT rather than from module descriptors.

## License

Apache License 2.0, see [LICENSE](LICENSE). `02-jenesis/build/jenesis` holds the sources of
[Jenesis](https://github.com/jenesis/jenesis) 0.18.1, which a Jenesis project carries by design, under the same
license. `03-vidocq` is a separate repository with terms of its own.
