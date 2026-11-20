# Vaadin on the Java module path: examples

Examples that try out Java module (JPMS) support for Vaadin Flow, as discussed in
[vaadin/flow#7482](https://github.com/vaadin/flow/issues/7482). Each one is an application that is a named module
and runs from the module path, built in a different way:

| Example | Build | Runs on |
|---|---|---|
| [`01-jetty`](01-jetty) | Maven | embedded Jetty, plain `java -p … -m …` |
| [`02-jenesis`](02-jenesis) | [Jenesis](https://jenesis.build/), with `module-info.java` as the only build description and the frontend built by [`jenesis-vaadin-plugin`](jenesis-vaadin-plugin) | embedded Jetty |
| [`03-vidocq`](03-vidocq) | Maven, a branch of [VidocqTools/vaadin-on-vidocq](https://codefloe.com/VidocqTools/vaadin-on-vidocq), as a Git submodule | the [Vidocq](https://vidocq.dev) runtime, also as a `jlink` image |
| [`04-quarkus`](04-quarkus) | Maven, Quarkus 4.0.0.CR1 with `quarkus-jlink` | Quarkus as a `jlink` image, which does not work yet without patches |

Quarkus CR1 was checked again with platform beta2 on 2026-11-20: the fast-jar works with push; the jlink image still needs the patches
and its push script still returns 404. See the [Quarkus results](04-quarkus/README.md#published-platform-beta-2540-beta2).

## Published beta experiment: 25.4.0-beta2

This branch, `test/vaadin-25.4.0-beta2`, builds on the beta1 experiment and imports **Vaadin platform
25.4.0-beta2 from Maven Central**, without running `build-snapshots.sh`. The platform BOM selects **Flow and
vaadin-quarkus 25.4.0-beta1**, and **Flow Components 25.4.0-beta2**. There is no `flow-server:25.4.0-beta2` release;
setting every artifact to beta2 would fail. The Flow frontend plugin and the Jenesis plugin's Flow API therefore
stay at beta1 too. See the [platform release](https://github.com/vaadin/platform/releases/tag/25.4.0-beta2).

The Maven examples and the Jenesis module descriptor import `vaadin-bom:25.4.0-beta2`. `03-vidocq` remains an
unchanged Git submodule: [`vidocq-mvn.sh`](vidocq-mvn.sh) creates a temporary POM beside its original, imports the
platform BOM, and separates the component and Flow versions. It requires Python 3 and removes that POM on exit.
Application/plugin versions and Vidocq itself still use SNAPSHOT versions; no Vaadin JPMS snapshot is needed.

Checked on 2026-11-20 with JDK 25, Maven 3.10.0 and Chromium. Jetty remains 12.1.14, Quarkus remains 4.0.0.CR1,
and the Vidocq submodule remains at `29f6b083848e7999313bc033afe5b86bb61a7e05` for comparison with beta1.

| Example | Build | Runtime result with the beta |
|---|---|---|
| `01-jetty` | Pass, retaining the beta1 Jackson and jsoup requirements | Browser view, server round trip and long-polling push pass |
| `02-jenesis` | Pass, retaining the beta1 Jackson requirement and pin | Browser view, server round trip, push and Vaadin Button pass |
| `03-vidocq` package | Pass | Default layering returns HTTP 500: `Unable to find index.html`; building with `-Dvidocq.app.layer=all` restores the browser view and server round trip |
| `03-vidocq` jlink | Pass, Vauban generates descriptors for Flow too | Starts, but requesting the view fails: `AppShell: module com.vaadin.flow.server does not declare uses` |
| `04-quarkus` jlink | Pass with the published extension | Unpatched image fails on `META-INF.VAADIN.config: Invalid package name`; existing `patch-image.sh` lets it start, but the push script still returns 404 and the view stays blank |
| `04-quarkus` fast-jar | Pass | Browser view, server round trip and push pass, on the class path |

### What changed since beta1

Published module names and the versions actually consumed by the platform are separate steps. Maven dependency
resolution and inspection of the released jars gave these results:

| Artifact | Platform beta2 selects | Published JPMS improvement and remaining work |
|---|---|---|
| Flow / vaadin-quarkus | `25.4.0-beta1` | Same jars as before: automatic names, no explicit descriptors or new class-loader fixes. |
| Flow Components | `25.4.0-beta2` | Component builds and browser tests pass with the existing automatic module names. |
| `license-checker` | `3.1.5` (beta1 selected `3.1.4`) | Now declares `com.vaadin.pro.licensechecker`. This improvement has reached the platform BOM. |
| Vaadin's Atmosphere fork | `3.0.5.slf4jvaadin1` | Released `3.0.5.slf4jvaadin2` declares `com.vaadin.external.atmosphere.runtime`, but Flow's dependency still selects the previous release. |
| `open`, via license-checker | `8.5.0.5` | Released `8.5.0.6` declares `com.vaadin.open`, but license-checker 3.1.5 still depends on 8.5.0.5. |
| Platform jars | `25.4.0-beta2` | `vaadin-core`, `vaadin-core-internal`, `vaadin-core-components` and `vaadin` declare `com.vaadin.core`, `com.vaadin.core.internal`, `com.vaadin.core.components` and `com.vaadin.platform`. Inspection of the beta1 jars found the same names already there. |

The new Atmosphere, Open and license-checker jars, and the four platform jars, keep their declared names when the
jar file is renamed. They are still automatic modules. The platform and license-checker entries were checked by
artifact inspection and dependency resolution; the minimal application examples do not exercise commercial
component licensing or the full platform dependency graph.

**The separately released Atmosphere update works in the Jetty examples.** With the explicit override to
`3.0.5.slf4jvaadin2`, both Jetty and Jenesis pass browser round-trip and long-polling push checks, and Jenesis also
passes the Vaadin Button check. Its launch arguments now put Atmosphere on the module path as
`com.vaadin.external.atmosphere.runtime`; with the platform default it falls back to the class path. Flow logs a
version-mismatch warning because its expected Atmosphere version is still `3.0.5.slf4jvaadin1`.

Quarkus's image still fails on the invalid `META-INF` package with either Atmosphere release. The existing patch
script lets both images start and serve the index, but the push script still returns 404. The default fast-jar
passes browser round-trip and push checks. Updating the fork's module name does not resolve the resource lookup
or module-access problems of the image.

The other beta1 findings remain reproducible:

- Jetty needs `requires tools.jackson.databind` and `requires org.jsoup`; Jenesis needs the Jackson requirement
  and its `3.1.8` pin. Flow still has no transitive module requirements. Jenesis launches with
  `--add-modules ALL-MODULE-PATH,ALL-DEFAULT`.
- Vidocq's default package returns HTTP 500 for missing `index.html`. Building with `vidocq.app.layer=all` restores
  the view and server round trip with the beta2 components.
- Vidocq's jlink image builds, but requesting the view fails during AppShell service lookup because Flow does not
  declare `uses` for it. The image still contains `java.desktop`, `java.prefs` and `java.datatransfer`.

### Recommended next steps for the Vaadin team

**1. Finish adopting the published module names.** This remains the best return for the remaining work: the
Atmosphere and Open PRs have been merged and named artifacts are available, so the immediate task is updating
consumers. The license-checker name is already included through the platform beta2 BOM.

| Existing PR | Status and next action |
|---|---|
| [vaadin/atmosphere#19](https://github.com/vaadin/atmosphere/pull/19) | Merged; name present in `3.0.5.slf4jvaadin2`. Update Flow's dependency and expected version, then rerun push tests. On the explicit-descriptor branch, use `requires com.vaadin.external.atmosphere.runtime`. |
| [vaadin/open#19](https://github.com/vaadin/open/pull/19) | Merged; name present in `8.5.0.6`. Update license-checker's dependency from `8.5.0.5`, release it and adopt that version in the platform. |
| [vaadin/license-checker-vaadin10#268](https://github.com/vaadin/license-checker-vaadin10/pull/268) (private) | Named `3.1.5` artifact is selected by platform beta2. Add module-path coverage involving a commercial component; that path is not covered by these minimal examples. |

For future updates, check both the manifest and the resolved dependency tree. A merged PR or a release by itself
does not change the artifact used by an application. Keep renamed-jar checks so module identities remain
independent of a tool's artifact naming convention.

**2. Extract the runtime fixes from `feature/jpms`, with regression tests.** The default Vidocq package reproduces
`Unable to find index.html`: resources must be found through the servlet context's class loader when Flow lives in
a parent layer. The jlink example reproduces the dynamic service-lookup failure when creating AppShell. The
snapshot experiments have working fixes for both. Success means the package works without `vidocq.app.layer=all`,
and the image can create the app shell and serve the view. Platform beta2 still ships the same Flow binary, so
these fixes have not arrived through this release.

**3. Continue with explicit Flow module descriptors.** Use Jetty and Jenesis to check that Flow brings its required
modules into the graph without the application's Jackson and jsoup workarounds. Use Vidocq to check that Flow no
longer needs generated descriptors for jlink; components and other automatic dependencies may still need them.
Keep the `java.desktop` reduction as a separate image-size check, as described in finding 8 below.

**4. Track the Quarkus failures with focused upstream reproductions.** CR1 still rejects `META-INF/VAADIN` as a
package name and the patched image still returns 404 for the push script. Keep the resource/package and Undertow
access problems separate from Atmosphere's `jakarta.annotation` read edge. The named Atmosphere release makes no
observable difference to the startup and resource failures tested here.

### Run the beta examples

Run one at a time, since they default to port 8080:

```sh
01-jetty/run.sh

(cd jenesis-vaadin-plugin && mvn clean install)
(cd 02-jenesis && java build/jenesis/Execute.java)

./vidocq-mvn.sh clean package vidocq:package
03-vidocq/target/vaadin-on-vidocq-0.4.0-SNAPSHOT/bin/vaadin-on-vidocq.sh
# The default package reproduces the index.html failure above.

# Working package: put Flow and the application in the same layer.
# This is a Maven build setting, not just a JVM option. Clean when changing the layout.
./vidocq-mvn.sh -Dvidocq.app.layer=all clean package vidocq:package
03-vidocq/target/vaadin-on-vidocq-0.4.0-SNAPSHOT/bin/vaadin-on-vidocq.sh

./vidocq-mvn.sh -Pjlink clean package vidocq:jlink
JDK_JAVA_OPTIONS=-Dvaadin.productionMode=true 03-vidocq/target/dist/bin/vaadin-on-vidocq
# The image reproduces the AppShell uses failure above.

(cd 04-quarkus && mvn clean package && ./patch-image.sh && \
  target/jlink-output/image/bin/jpms-example-quarkus-1.0-SNAPSHOT)
# Push is still broken in this image. The same build also produces the working fast-jar:
(cd 04-quarkus && java -jar target/quarkus-app/quarkus-run.jar)
```

### Try the separately released Atmosphere module name

The default builds above use the platform's original Atmosphere dependency. The `atmosphere-jpms` Maven profile
in `01-jetty` and `04-quarkus` opts into published `3.0.5.slf4jvaadin2` without a local snapshot:

```sh
01-jetty/run.sh -Patmosphere-jpms
(cd 04-quarkus && mvn -Patmosphere-jpms clean package && ./patch-image.sh)
```

Use `clean` when switching dependency versions, so old jars do not remain on the module path. `01-jetty/run.sh`
does this automatically. The Quarkus image still has the limitations above with this profile.

For the equivalent Jenesis experiment, add this pin to the Javadoc of `02-jenesis/sources/module-info.java`, then
run `java build/jenesis/Execute.java` as usual. Remove it to return to the platform default. Both variants were
tested; this branch leaves the pin out by default.

```java
 * @jenesis.pin main/maven/com.vaadin.external.atmosphere/atmosphere-runtime 3.0.5.slf4jvaadin2
```

## Original JPMS snapshot experiments

The remaining sections record the original snapshot setup and its results, for comparison. They are not the
setup instructions or results for this beta branch. Use the commands above for platform 25.4.0-beta2.

The original experiments used Vaadin built from these branches:

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
   needs a small change for Quarkus 4. Its module name, `com.vaadin.quarkus`, is in
   [vaadin/flow#26731](https://github.com/vaadin/flow/pull/26731).

## To follow this autumn

What is left open, and worth following or asking about while Quarkus 4 is in beta, with its GA planned for November:

- **Releases of the module names**, ideally before Quarkus 4 GA, so that Vaadin enters its module graphs with stable
  names: [vaadin/flow#26731](https://github.com/vaadin/flow/pull/26731),
  [vaadin/flow-components#10527](https://github.com/vaadin/flow-components/pull/10527),
  [vaadin/atmosphere#19](https://github.com/vaadin/atmosphere/pull/19), [vaadin/open#19](https://github.com/vaadin/open/pull/19)
  and vaadin/license-checker-vaadin10#268.
- **Quarkus, `META-INF` folders taken for packages**: whether `quarkus-jlink` keeps listing resource folders such as
  `META-INF/VAADIN` as packages of the application module, which stops every Vaadin application from starting.
- **Quarkus, Undertow in a `jlink` image**: whether the module descriptors of Undertow get the packages it uses, as
  4.0.0.Beta2 did for REST, WebSockets Next and Elytron.
- **Quarkus, static resources of other modules**: whether the servlet context of a `jlink` image serves the
  `META-INF/resources` of library modules, which Vaadin push and probably components and themes rely on.
- **Quarkus, `java.se` for automatic modules**: as Quarkus extensions get module names or descriptors, the image can
  leave out `java.desktop`, which [vaadin/flow#26837](https://github.com/vaadin/flow/pull/26837) makes possible for
  Flow.
- **Push without Atmosphere**, [vaadin/flow#26785](https://github.com/vaadin/flow/pull/26785), or else Atmosphere
  reading `jakarta.annotation` on the module path.
- **vaadin-quarkus on Quarkus 4**: the `CodeGenProvider` change and its tests, when Flow moves to Quarkus 4.
- **PWA icons**: which of [vaadin/flow#26837](https://github.com/vaadin/flow/pull/26837) and
  [vaadin/flow#26647](https://github.com/vaadin/flow/pull/26647) carries the drawing of PWA icons out of the classes
  every application loads, see [ECOSYSTEM.md](ECOSYSTEM.md).

## Spring Boot, Quarkus and native images

[ECOSYSTEM.md](ECOSYSTEM.md) looks at what this work means for Spring Boot, Quarkus and GraalVM native images. Spring
stays on the class path, Quarkus is building modular packaging and `jlink` images for Quarkus 4, and native images gain
from dropping the run time use of AWT rather than from module descriptors.

## License

Apache License 2.0, see [LICENSE](LICENSE). `02-jenesis/build/jenesis` holds the sources of
[Jenesis](https://github.com/jenesis/jenesis) 0.18.1, which a Jenesis project carries by design, under the same
license. `03-vidocq` is a separate repository with terms of its own.
