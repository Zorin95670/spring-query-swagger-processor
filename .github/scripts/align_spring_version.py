#!/usr/bin/env python3
"""
Automatically aligns:
  - the project's <version>
  - the <java.version>/<source>/<target> compiler settings (if present)
with the spring-boot-maven-plugin version declared in pom.xml.

This variant reads the Spring Boot version from the <plugin> declaration
under <build><plugins> (org.springframework.boot:spring-boot-maven-plugin),
rather than from a spring-boot-dependencies BOM in <dependencyManagement>.
Use this version for projects that only depend on the Spring Boot Maven
plugin without importing the full BOM.

Usage:
    python align_spring_version.py pom.xml .github/spring-java-compat.yml

Output (stdout): a text summary + GitHub Actions output variables
(GITHUB_OUTPUT) to drive the rest of the workflow:
    new_version=4.1.0
    new_java=21
    java_bumped=true|false
    compat_unknown=true|false
"""

import sys
import os
import re
import xml.etree.ElementTree as ET
import yaml

NS = {"m": "http://maven.apache.org/POM/4.0.0"}

PROJECT_ARTIFACT_ID = "spring-query-swagger-processor"


def fail(msg: str) -> None:
    print(f"::error::{msg}")
    sys.exit(1)


def get_spring_boot_version(root: ET.Element) -> str:
    """Reads the spring-boot-maven-plugin version from <build><plugins>."""
    plugins = root.findall(".//m:build/m:plugins/m:plugin", NS)
    for plugin in plugins:
        group_id = plugin.find("m:groupId", NS)
        artifact_id = plugin.find("m:artifactId", NS)
        version = plugin.find("m:version", NS)
        if (
            artifact_id is not None
            and version is not None
            and artifact_id.text == "spring-boot-maven-plugin"
            and (group_id is None or group_id.text == "org.springframework.boot")
        ):
            return version.text
    fail("Could not find org.springframework.boot:spring-boot-maven-plugin in build/plugins")


def ensure_clean_release_version(version: str) -> None:
    """
    Ensures the Spring Boot version is a clean release version (e.g. 4.0.5),
    rejecting milestones, release candidates, or snapshots
    (e.g. 4.1.0-RC1, 4.1.0-M1, 4.1.0-SNAPSHOT).
    """
    if not re.match(r"^[0-9]+\.[0-9]+\.[0-9]+$", version):
        fail(
            f"Spring Boot version '{version}' is not a clean release version "
            f"(expected format X.Y.Z). Refusing to align on a milestone, "
            f"release candidate, or snapshot version."
        )


def major_minor(version: str) -> str:
    """4.0.5 -> '4.0'."""
    match = re.match(r"^(\d+)\.(\d+)", version)
    if not match:
        fail(f"Unexpected Spring Boot version: {version}")
    return f"{match.group(1)}.{match.group(2)}"


def load_compat_map(path: str) -> dict:
    if not os.path.exists(path):
        fail(f"Compatibility mapping file not found: {path}")
    with open(path, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}
    # YAML keys like "3.4" may be parsed as float; normalize to str
    return {str(k): int(v) for k, v in data.items()}


def read_current_java(raw: str) -> int:
    """
    Reads the current Java version. This project has no <java.version>
    property; instead it's set directly in maven-compiler-plugin's
    <source>/<target>. Both are expected to match.
    """
    source_match = re.search(r"<source>(\d+)</source>", raw)
    target_match = re.search(r"<target>(\d+)</target>", raw)
    if not source_match or not target_match:
        fail("Could not read <source>/<target> in maven-compiler-plugin configuration")
    source = int(source_match.group(1))
    target = int(target_match.group(1))
    if source != target:
        fail(f"<source> ({source}) and <target> ({target}) differ, refusing to guess")
    return source


def update_pom(pom_path: str, raw: str, new_version: str, new_java: str | None) -> str:
    """
    Updates pom.xml as raw text (no full ElementTree rewrite, to avoid
    breaking existing formatting/comments).
    """
    # 1. Project <version>...</version> — only the first occurrence,
    #    right after the project's own <artifactId> (no <parent> in this case).
    pattern_version = re.compile(
        rf"(<artifactId>{PROJECT_ARTIFACT_ID}</artifactId>\s*<version>)[^<]+(</version>)"
    )
    new_raw, count = pattern_version.subn(rf"\g<1>{new_version}\g<2>", raw, count=1)
    if count != 1:
        fail("Could not locate the project's <version> tag to update")
    raw = new_raw

    # 2. <source>...</source> and <target>...</target> in maven-compiler-plugin
    if new_java is not None:
        pattern_source = re.compile(r"(<source>)\d+(</source>)")
        new_raw, count = pattern_source.subn(rf"\g<1>{new_java}\g<2>", raw, count=1)
        if count != 1:
            fail("Could not locate the <source> tag to update")
        raw = new_raw

        pattern_target = re.compile(r"(<target>)\d+(</target>)")
        new_raw, count = pattern_target.subn(rf"\g<1>{new_java}\g<2>", raw, count=1)
        if count != 1:
            fail("Could not locate the <target> tag to update")
        raw = new_raw

    return raw


def main() -> None:
    if len(sys.argv) != 3:
        fail("Usage: align_spring_version.py <pom.xml> <compat_map.yml>")

    pom_path, compat_path = sys.argv[1], sys.argv[2]

    with open(pom_path, "r", encoding="utf-8") as f:
        raw_pom = f.read()

    root = ET.fromstring(raw_pom)
    spring_version = get_spring_boot_version(root)
    ensure_clean_release_version(spring_version)
    mm = major_minor(spring_version)

    compat_map = load_compat_map(compat_path)
    required_java = compat_map.get(mm)

    current_java = read_current_java(raw_pom)

    compat_unknown = required_java is None
    java_bumped = False
    new_java_value = None

    if compat_unknown:
        print(
            f"::warning::Spring Boot version {spring_version} ({mm}) is missing from "
            f"the Java compatibility mapping. Manual review required."
        )
    else:
        if current_java < required_java:
            java_bumped = True
            new_java_value = str(required_java)
            print(
                f"Java {current_java} -> {required_java} "
                f"(required by Spring Boot {mm})"
            )
        else:
            print(
                f"Java {current_java} is already compatible with Spring Boot {mm} "
                f"(minimum required: {required_java})"
            )

    updated_pom = update_pom(pom_path, raw_pom, spring_version, new_java_value)
    with open(pom_path, "w", encoding="utf-8") as f:
        f.write(updated_pom)

    print(f"Project version aligned with Spring Boot: {spring_version}")

    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"new_version={spring_version}\n")
            f.write(f"new_java={new_java_value or current_java}\n")
            f.write(f"java_bumped={'true' if java_bumped else 'false'}\n")
            f.write(f"compat_unknown={'true' if compat_unknown else 'false'}\n")


if __name__ == "__main__":
    main()