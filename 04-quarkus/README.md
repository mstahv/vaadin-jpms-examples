# Vaadin on Quarkus 4, packaged with jlink

A Vaadin Flow application on [Quarkus](https://quarkus.io) 4.0.0.Beta2, packaged with the experimental
[`quarkus-jlink`](https://quarkus.io/guides/jlink) extension into a runtime image that contains the JDK modules and
the application modules it needs. Quarkus 4 is to make this packaging first class, see
[ECOSYSTEM.md](../ECOSYSTEM.md).

**It does not work yet.** The image builds, but starts only after [`patch-image.sh`](patch-image.sh) has patched the
module descriptors Quarkus generated, and push still fails then. Each patch is a problem to solve, in Quarkus or in
Vaadin, and they are listed below. As a fast-jar, the same application works on Quarkus 4.0.0.Beta2.

## Run it

Requirements: JDK 25 (`quarkus-jlink` needs it), Maven, and Flow and vaadin-quarkus at `25.4-jpms-SNAPSHOT` in the local
Maven repository, built by [`../build-snapshots.sh`](../build-snapshots.sh), which builds vaadin-quarkus against
Quarkus 4.0.0.Beta2.

```sh
mvn package                        # builds the frontend and the image, target/jlink-output/image
./patch-image.sh                   # patches the module descriptors of the image
target/jlink-output/image/bin/jpms-example-quarkus-1.0-SNAPSHOT
```

http://localhost:8080 then shows the view, but stays blank while it waits for the push script, which answers 404.
Without `@Push` in `AppShell`, the view shows each module and its class loader, and the server round trip works.

The fast-jar for comparison, which works with push:

```sh
mvn package -Dquarkus.jlink.enabled=false
java -jar target/quarkus-app/quarkus-run.jar
```

## What it takes

vaadin-quarkus did not compile against Quarkus 4: `CodeGenProvider.shouldRun` takes the SmallRye `Config` instead of the
MicroProfile one. `build-snapshots.sh` changes the import, and skips the tests of the extension, which do not compile
against the test framework of Quarkus 4 either.

Problems in the image, in the order they stopped the application, with the patch that got past each one:

| # | Problem | Whose | Patch |
|---|---|---|---|
| 1 | The image does not start: _`META-INF.VAADIN.config: Invalid package name`_. Quarkus lists every folder of the application jar as a package of its module, including `META-INF/VAADIN`, where the Vaadin frontend build writes the bundle. A folder whose name is not a Java identifier holds resources, not a package. | Quarkus | the `META-INF` entries removed from the packages of the application module |
| 2 | Undertow, the servlet container of Quarkus, uses packages that the module descriptors Quarkus generated leave private, or that it does not depend on: `io.undertow.security.impl`, `io.undertow.vertx`, and `io.vertx.core.net.impl` of Vert.x. Without them, the application fails to start, then every request fails. | Quarkus | the packages exported and the dependency added |
| 3 | Push fails to initialize: Atmosphere _does not read module `jakarta.annotation`_. Atmosphere is an automatic module, which Quarkus lets read its Maven dependencies only, and `jakarta.annotation` is not one at run time. | Vaadin | the dependency added to the Atmosphere module |
| 4 | Push does not work: the push script, `VAADIN/static/push/vaadinPush-min.js`, answers 404. It is in `META-INF/resources` of flow-push, and the servlet context of the image does not see the `META-INF/resources` of other modules. The static resources of components and themes are likely to fail the same way, which is not tried here. | Quarkus, Vaadin | none, push is left broken |

Problems 3 and 4 are about Atmosphere: push without it, as in [vaadin/flow#26785](https://github.com/vaadin/flow/pull/26785),
would avoid both, and leave out an automatic module. Not tried here, as that pull request is not on `feature/jpms`.

## What the image contains

- **The Vaadin modules are explicit modules, as on the module path.** The view shows `com.vaadin.flow.server`,
  `com.vaadin.flow.component.html` and `com.vaadin.flow.component.react` as explicit modules, each in a
  `ModuleClassLoader` of its own. Quarkus turns their `module-info` into the `module.xml` of its module loader, with
  the same requirements and exports.
- **Module names matter.** Quarkus warns of each artifact without a declared module name and computes one. The Vaadin
  artifacts pass. vaadin-quarkus was the only one without a name, and has `com.vaadin.quarkus` now, in
  [vaadin/flow#26731](https://github.com/vaadin/flow/pull/26731) and on `feature/jpms`. Many Quarkus extensions are
  still in the list in 4.0.0.Beta2.
- **The whole of Java SE.** Quarkus lets every automatic module require `java.se`, which puts all of it in the image,
  `java.desktop` with AWT and its native libraries included. Atmosphere, flow-client and vaadin-quarkus are automatic
  modules, as are many Quarkus extensions, so leaving `java.desktop` out of flow-server
  ([vaadin/flow#26837](https://github.com/vaadin/flow/pull/26837)) does not shrink this image yet.
- **The size.** The image is 172 MB, the JDK included. The fast-jar is 28 MB, plus a JDK, 302 MB for a full JDK 25.
- **flow-client is in the image**, although only the frontend build needs it, see
  [vaadin/flow#25882](https://github.com/vaadin/flow/pull/25882).
