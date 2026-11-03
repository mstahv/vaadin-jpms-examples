#!/usr/bin/env bash
#
# Builds the Flow and Flow Components modules of the JPMS work and installs
# them into the local Maven repository with a version of their own,
# 25.4-jpms-SNAPSHOT.
#
# The version of their own keeps them apart from other local 25.4-SNAPSHOT
# builds, and the remote 25.4-SNAPSHOT that Maven downloads once a day can not
# replace them.
#
# Settings, all optional. A *_SOURCE may also be a local clone, such as the
# one a change is made in:
#   FLOW_SOURCE        Flow git repository
#                      (default: https://github.com/vaadin/flow.git)
#   FLOW_BRANCH        Flow branch (default: feature/jpms)
#   COMPONENTS_SOURCE  Flow Components git repository
#                      (default: https://github.com/vaadin/flow-components.git)
#   COMPONENTS_BRANCH  Flow Components branch (default: feature/jpms-amn)
#   ATMOSPHERE_SOURCE  Atmosphere fork git repository
#                      (default: https://github.com/vaadin/atmosphere.git)
#   ATMOSPHERE_BRANCH  Atmosphere fork branch (default: feature/jpms-amn,
#                      vaadin/atmosphere#19)
#   ATMOSPHERE_VERSION version the fork is installed as, and Flow is built
#                      with (default: 3.0.5.slf4jvaadin1-jpms)
#   JPMS_VERSION       version to install (default: 25.4-jpms-SNAPSHOT)
#   WORK_DIR           where the sources are cloned (default:
#                      ~/.cache/vaadin-jpms-examples)
#   SKIP_ATMOSPHERE=1  build Flow with the released Atmosphere fork, which
#                      declares no module name
#   SKIP_FLOW=1        do not build Flow
#   SKIP_COMPONENTS=1  do not build Flow Components
set -euo pipefail

FLOW_SOURCE=${FLOW_SOURCE:-https://github.com/vaadin/flow.git}
FLOW_BRANCH=${FLOW_BRANCH:-feature/jpms}
COMPONENTS_SOURCE=${COMPONENTS_SOURCE:-https://github.com/vaadin/flow-components.git}
COMPONENTS_BRANCH=${COMPONENTS_BRANCH:-feature/jpms-amn}
ATMOSPHERE_SOURCE=${ATMOSPHERE_SOURCE:-https://github.com/vaadin/atmosphere.git}
ATMOSPHERE_BRANCH=${ATMOSPHERE_BRANCH:-feature/jpms-amn}
ATMOSPHERE_VERSION=${ATMOSPHERE_VERSION:-3.0.5.slf4jvaadin1-jpms}
RELEASED_ATMOSPHERE=3.0.5.slf4jvaadin1
JPMS_VERSION=${JPMS_VERSION:-25.4-jpms-SNAPSHOT}
WORK_DIR=${WORK_DIR:-$HOME/.cache/vaadin-jpms-examples}

MVN_FLAGS=(-B -nsu -DskipTests -Dinvoker.skip=true -Dspotless.skip=true
    -Dcheckstyle.skip=true -Djacoco.skip=true)

# Clones the given branch into $WORK_DIR/<name> and sets the version
checkout() {
    local name=$1 source=$2 branch=$3
    echo "Building $branch of $source as $JPMS_VERSION in $WORK_DIR/$name"
    rm -rf "${WORK_DIR:?}/$name"
    mkdir -p "$WORK_DIR"
    git clone --quiet --branch "$branch" --single-branch "$source" \
        "$WORK_DIR/$name"
    cd "$WORK_DIR/$name"
    git log -1 --format='Commit: %h %s'
    mvn -B -q versions:set -DnewVersion="$JPMS_VERSION" \
        -DprocessAllModules=true -DgenerateBackupPoms=false
}

if [ -z "${SKIP_ATMOSPHERE:-}" ] && [ -z "${SKIP_FLOW:-}" ]; then
    echo "Building $ATMOSPHERE_BRANCH of $ATMOSPHERE_SOURCE as $ATMOSPHERE_VERSION"
    rm -rf "${WORK_DIR:?}/atmosphere"
    mkdir -p "$WORK_DIR"
    git clone --quiet --branch "$ATMOSPHERE_BRANCH" --single-branch \
        "$ATMOSPHERE_SOURCE" "$WORK_DIR/atmosphere"
    cd "$WORK_DIR/atmosphere"
    git log -1 --format='Commit: %h %s'
    mvn -B -q -DskipTests package -pl modules/cpr
    # The release build publishes the fork under com.vaadin.external.atmosphere.
    # The released POM, with the version changed, gives the same dependencies.
    mvn -B -q dependency:get \
        -Dartifact="com.vaadin.external.atmosphere:atmosphere-runtime:$RELEASED_ATMOSPHERE:pom"
    released_pom="$HOME/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/$RELEASED_ATMOSPHERE/atmosphere-runtime-$RELEASED_ATMOSPHERE.pom"
    python3 - "$released_pom" "$WORK_DIR/atmosphere/runtime.pom" \
        "$ATMOSPHERE_VERSION" <<'PY'
import re, sys
source, target, version = sys.argv[1:]
pom = open(source).read()
# The version of the artifact itself, not the one of its parent
pom = re.sub(r'(<artifactId>atmosphere-runtime</artifactId>\s*<version>)[^<]+',
             r'\g<1>' + version, pom, count=1)
open(target, 'w').write(pom)
PY
    mvn -B -q install:install-file -DpomFile=runtime.pom \
        -Dfile="$(ls modules/cpr/target/atmosphere-runtime-*.jar | grep -v sources | head -1)"
fi

if [ -z "${SKIP_FLOW:-}" ]; then
    checkout flow "$FLOW_SOURCE" "$FLOW_BRANCH"
    if [ -z "${SKIP_ATMOSPHERE:-}" ]; then
        # What Flow changes once the fork is released with a module name
        sed -i.bak "s#<atmosphere.runtime.version>[^<]*<#<atmosphere.runtime.version>$ATMOSPHERE_VERSION<#" \
            pom.xml
        rm pom.xml.bak
        grep -q "<atmosphere.runtime.version>$ATMOSPHERE_VERSION<" pom.xml
        sed -i.bak 's/requires atmosphere.runtime;/requires com.vaadin.external.atmosphere.runtime;/' \
            flow-server/src/main/java/module-info.java
        rm flow-server/src/main/java/module-info.java.bak
        grep -q 'requires com.vaadin.external.atmosphere.runtime;' \
            flow-server/src/main/java/module-info.java
    fi
    # Tests are run by the CI of the branch. They are compiled, because other
    # modules depend on their test-jars.
    mvn "${MVN_FLAGS[@]}" install \
        -pl flow-bom,flow-server,flow-push,flow-client,flow-data,flow-html-components,flow-dnd,flow-lit-template,flow-react,flow-test-generic,flow-test-util,flow-plugins/flow-maven-plugin \
        -am
fi

if [ -z "${SKIP_COMPONENTS:-}" ]; then
    checkout flow-components "$COMPONENTS_SOURCE" "$COMPONENTS_BRANCH"
    mvn -B -q versions:set-property -Dproperty=flow.version \
        -DnewVersion="$JPMS_VERSION" -DgenerateBackupPoms=false
    # The components the examples use, and the themes
    mvn "${MVN_FLAGS[@]}" install \
        -pl vaadin-button-flow-parent/vaadin-button-flow,vaadin-text-field-flow-parent/vaadin-text-field-flow,vaadin-ordered-layout-flow-parent/vaadin-ordered-layout-flow,vaadin-lumo-theme-flow-parent/vaadin-lumo-theme-flow,vaadin-aura-theme-flow-parent/vaadin-aura-theme-flow \
        -am
fi

echo
echo "Module names of $JPMS_VERSION:"
atmosphere_jar="$HOME/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/$ATMOSPHERE_VERSION/atmosphere-runtime-$ATMOSPHERE_VERSION.jar"
if [ -f "$atmosphere_jar" ]; then
    echo "  atmosphere-runtime $ATMOSPHERE_VERSION: $(jar --describe-module --file "$atmosphere_jar" | grep -v '^No module' | grep -m1 . | cut -d' ' -f1)"
fi
for module in flow-server flow-push flow-data flow-html-components flow-react \
    vaadin-flow-components-base vaadin-button-flow vaadin-text-field-flow \
    vaadin-ordered-layout-flow vaadin-lumo-theme vaadin-aura-theme; do
    jar="$HOME/.m2/repository/com/vaadin/$module/$JPMS_VERSION/$module-$JPMS_VERSION.jar"
    if [ -f "$jar" ]; then
        echo "  $module: $(jar --describe-module --file "$jar" | grep -v '^No module' | grep -m1 . | cut -d' ' -f1)"
    fi
done
