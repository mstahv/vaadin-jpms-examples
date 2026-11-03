#!/usr/bin/env bash
#
# Patches the module descriptors (module.xml) of the jlink image that
# Quarkus 4.0.0.Beta2 builds, so that the Vaadin application starts. Each patch
# is a problem to solve in Quarkus or in Vaadin, see README.md.
#
# Run after mvn package, before starting the image.
set -euo pipefail

QUARKUS_MODULES=$(cd "${1:-target/jlink-output/image/lib/quarkus}" && pwd)

# patch <module> <sed expression>: edits the module.xml of a module
patch() {
    local jar work
    jar=$(ls "$QUARKUS_MODULES/$1"/*.jar)
    work=$(mktemp -d)
    chmod u+w "$jar"
    (cd "$work" && unzip -q -o "$jar" module.xml && chmod 644 module.xml \
        && sed -i.bak "$2" module.xml && zip -q "$jar" module.xml)
    rm -rf "$work"
}

# export <module> <package>: exports a package that Quarkus made private
export_package() {
    patch "$1" "s#<private package=\"$2\"/>#<export package=\"$2\"/>#"
}

# depend <module> <dependency>: adds a dependency to a module
depend() {
    patch "$1" "0,/    <dependencies>/s##    <dependencies>\n        <dependency name=\"$2\"/>#"
}

# 1. Vaadin, Quarkus: the frontend build writes META-INF/VAADIN into the
#    application, and Quarkus lists the folders as packages of the application
#    module, which are not valid package names
patch com.example.jpms.example.quarkus '/package="META-INF/d'

# 2. Quarkus: Undertow of Quarkus uses packages that its module descriptors
#    leave private, or does not depend on
export_package io.quarkus.http.undertow io.undertow.security.impl
export_package io.quarkus.http.undertow.vertx io.undertow.vertx
depend io.quarkus.undertow io.quarkus.http.undertow.vertx
export_package io.vertx.core io.vertx.core.net.impl

# 3. Vaadin: Atmosphere, an automatic module, reads only its Maven
#    dependencies, and jakarta.annotation is not one at run time
depend com.vaadin.external.atmosphere.runtime jakarta.annotation

echo "Patched the module descriptors in $QUARKUS_MODULES"
