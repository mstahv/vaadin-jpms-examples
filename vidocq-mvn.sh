#!/usr/bin/env bash
# Apply platform beta2 to the unchanged Vidocq submodule. Its original POM uses
# one version for Flow and components, but beta2 ships Flow beta1.
set -euo pipefail
cd "$(dirname "$0")/03-vidocq"
beta_pom=$(mktemp "$PWD/.pom-beta2-XXXXXX")
trap 'rm -f "$beta_pom"' EXIT

# Keep the temporary POM beside the original so source/resource paths and the
# frontend build keep their usual base directory, including during mvn clean.
python3 - "$beta_pom" <<'PY'
import sys
import xml.etree.ElementTree as ET

namespace = 'http://maven.apache.org/POM/4.0.0'
ET.register_namespace('', namespace)
ns = {'m': namespace}
tree = ET.parse('pom.xml')
root = tree.getroot()
properties = root.find('m:properties', ns)
properties.find('m:flow.version', ns).text = '25.4.0-beta1'
ET.SubElement(properties, f'{{{namespace}}}vaadin.version').text = '25.4.0-beta2'
bom = root.find('m:dependencyManagement/m:dependencies/m:dependency', ns)
assert bom.find('m:artifactId', ns).text == 'flow-bom'
bom.find('m:artifactId', ns).text = 'vaadin-bom'
bom.find('m:version', ns).text = '${vaadin.version}'
for dependency in root.findall('m:dependencies/m:dependency', ns):
    if dependency.find('m:groupId', ns).text == 'com.vaadin':
        version = dependency.find('m:version', ns)
        if version is not None:
            dependency.remove(version)
ET.indent(tree, space='    ')
tree.write(sys.argv[1], encoding='UTF-8', xml_declaration=True)
PY

mvn -f "$beta_pom" "$@"
