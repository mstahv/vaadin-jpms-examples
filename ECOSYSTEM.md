# Spring Boot, Quarkus and native images

What the module work of these examples means for the two application frameworks Vaadin runs on most, and for GraalVM
native images. Researched in November 2026; the sources are linked in each section.

In short: Spring stays on the class path and invests in the JVM's AOT cache instead. Quarkus is building modular
packaging and `jlink` images in Quarkus 4, whose betas are out, and names its modules before it gives them descriptors,
as these examples do. Native images do not get smaller from
module descriptors, but they do from dropping the run time use of AWT.

## Spring Boot

**No module descriptors are planned.** The Spring Framework jars are automatic modules with an
`Automatic-Module-Name`. The issue about module descriptors,
[spring-framework#18079](https://github.com/spring-projects/spring-framework/issues/18079), has been open since 2015
and is in the general backlog. Its clearest statement, from 2018, is still the position: _"we can't ship `module-info`
files quite yet since we'd need stable module names for all of our optional dependencies"_. A request to modularize
Spring Boot, [spring-boot#20903](https://github.com/spring-projects/spring-boot/issues/20903), was closed as a
duplicate of it. The only module related work found for Spring Framework 7 is narrow: 7.1 is to read annotated
`module-info` classes for JPA 4.0 persistence units
([spring-framework#37316](https://github.com/spring-projects/spring-framework/issues/37316)).

**The executable jar is not a module path.** Spring Boot loads the nested jars of its executable jar with a class loader
of its own.

**Spring optimizes the class path instead.** For startup, Spring Boot recommends the JVM's AOT cache on Java 25 and
CDS before it, from the extracted application, which runs on the class path
([Spring Boot reference](https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html)). The AOT cache of
JEP 483 holds the classes that the JDK's own class loaders load, from the class path or the module path, and none from
a class loader of an application ([InfoQ on JEP 483](https://www.infoq.com/news/2025/03/java-24-leyden-ships)).

**What it means for Vaadin.** Spring Boot applications will stay on the class path, where module descriptors and
module names make no difference. What helps them is a smaller runtime: `jlink` can build a runtime for a class path
application too, from the modules that `jdeps --print-module-deps` lists. Today that list includes `java.desktop`,
because Flow cannot start without it. With [vaadin/flow#26837](https://github.com/vaadin/flow/pull/26837) the module
can be left out, though `jdeps` still lists it, for the deprecated API and the PWA icon drawing, so it has to be left
out by hand until those are gone.

## Quarkus

**Modular packaging and `jlink` are in the works.** Quarkus has two experimental extensions:

- [`quarkus-modularity`](https://quarkus.io/guides/modularity) builds the module graph of an application at build
  time, _"a superset of the Java Platform Module System"_. A jar without a descriptor becomes an automatic module,
  whose dependencies are scoped to its Maven dependencies.
- [`quarkus-jlink`](https://quarkus.io/guides/jlink), on Java 25 and later, builds a runtime image with _"only the JDK
  modules and application modules that are needed"_. The JDK modules come from that module graph.

A [working group proposal](https://github.com/quarkusio/quarkus/discussions/53223), started in March 2026 and aimed at
Quarkus 4, is to make `jlink` a first-class packaging, eventually the default, and to deprecate the fast-jar. Its
prototype ranks the sizes as _GraalVM native < jlink (modular) < fast-jar < AOT_. It treats modularizing third-party
libraries as _"upstream responsibility; Quarkus handles them as unnamed/automatic modules"_.

**Quarkus 4 is in beta.** Beta 1 came out on 1 October 2026 and
[Beta 2](https://github.com/quarkusio/quarkus/releases/tag/4.0.0.Beta2) on 28 October, with GA planned for November
2026 in the [July progress report](https://github.com/quarkusio/quarkus/discussions/55320), which also said full
JPMS and `jlink` support would come _"most probably"_ with the second beta. Beta 2 has module tree shaking for `jlink`
(#55548) and a long list of fixes that let REST, REST Client, WebSockets Next and Elytron work in a `jlink` image,
but also open issues: an application packaged with `quarkus-jlink` failing to start, Qute with a duplicate module
name, and Hibernate Validator missing `java.money`. The Undertow servlet support, which Vaadin uses on Quarkus, is not
mentioned for `jlink`.

**Names first, descriptors later.** The modularization epic,
[quarkusio/quarkus#51583](https://github.com/quarkusio/quarkus/issues/51583), gives module names to the Quarkus
components and asks the same from their dependencies, from JBoss Logging and SmallRye to Maven, before any module
descriptors, _because Maven's transitive dependencies make automatic-module graphs larger than necessary_. Beta 2
already _"Use[s] the module names that dependencies now provide"_ (#57066). It is the order these examples took for
Vaadin: `Automatic-Module-Name` first, in pull requests of their own, and module descriptors on `feature/jpms`.

Two details matter for libraries such as Vaadin:

- _"Automatic modules are excluded from the boot set because they cannot be linked by `jlink`"_. They are loaded at
  run time from `lib/quarkus/`.
- _"JDK AOT precompilation via Class Data Sharing (CDS) currently only supports modules on the boot module path."_

**What it means for Vaadin.** In the modular packaging of Quarkus, the Vaadin jars are modules of a module graph.

- A stable module name, the `Automatic-Module-Name` of [vaadin/flow#26731](https://github.com/vaadin/flow/pull/26731)
  and [vaadin/flow-components#10527](https://github.com/vaadin/flow-components/pull/10527), is what the other modules
  of the graph refer to Vaadin by.
- Explicit module descriptors, as on `feature/jpms`, are what lets Flow into the boot set: linked into the image and
  covered by the AOT cache. This is the same case as the `jlink` image of [`03-vidocq`](03-vidocq).
- A `jlink` image built from the module graph contains `java.desktop` as long as flow-server requires it, see
  finding 8 of the [README](README.md).
- vaadin-quarkus looks the application resources up through a `ResourceProvider` of its own, with the context class
  loader of the thread. How that works with the class loaders of the modular packaging is not tested yet.
- [`04-quarkus`](04-quarkus) tries it on Quarkus 4.0.0.Beta2. The image starts only with patched module descriptors,
  and push does not work: of the four problems, two are in Quarkus and two in Vaadin's use of Atmosphere. Quarkus
  also lets every automatic module require all of `java.se`, so the image has `java.desktop` whatever flow-server
  requires.

## GraalVM native images

**Module descriptors do not make a native image smaller.** `native-image` analyzes which code is reachable and
compiles only that, from the class path or the module path alike
([options](https://www.graalvm.org/latest/reference-manual/native-image/overview/Options/),
[reachability metadata](https://www.graalvm.org/latest/reference-manual/native-image/metadata/)). No source was found
that measures a gain from modules, and the native builds of Spring Boot and Quarkus use the class path.

**AWT does make it bigger and harder.** A native image that can reach AWT needs reachability metadata for it, and on
Linux it has to ship the JDK's `libawt.so`, `libawt_xawt.so` and `libfontmanager.so` next to the executable
([Quarkus AWT guide](https://docs.quarkiverse.io/quarkus-desktop/dev/awt.html),
[BellSoft](https://bell-sw.com/blog/how-to-turn-awt-applications-into-native-images)). Vaadin has a history with this:
[vaadin/flow#7194](https://github.com/vaadin/flow/issues/7194),
[vaadin/flow#16307](https://github.com/vaadin/flow/issues/16307),
[vaadin/flow#19123](https://github.com/vaadin/flow/issues/19123). For a Spring Boot application with `@PWA`,
vaadin-spring registers 98 AWT and ImageIO classes for JNI in the native build
(`VaadinBeanFactoryInitializationAotProcessor`, with a list taken from the Quarkus AWT extension).

**What it means for Vaadin.** The two pull requests on PWA icons help in different ways:

- [vaadin/flow#26837](https://github.com/vaadin/flow/pull/26837) keeps AWT out of the classes every application
  loads, which is what a JVM without `java.desktop` needs. In a native image, the drawing is still reachable from
  `PwaIcon`, so AWT stays in.
- [vaadin/flow#26647](https://github.com/vaadin/flow/pull/26647) moves the drawing into flow-build-tools, which is not
  on the class path of a production application. Then the drawing is not reachable, and a native image of an
  application with `@PWA` could leave AWT, its metadata and its native libraries out. On its own, it does not yet let
  Flow start without `java.desktop`, as the deprecated AWT methods of `PwaIcon` keep AWT in that class.

Together they would cover both: a `jlink` image without `java.desktop`, and a native image without AWT.
