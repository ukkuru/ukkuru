#!/usr/bin/env bash
# Runs the Robolectric smoke test. Screenshots land in target/screens.
set -euo pipefail
cd "$(dirname "$0")"
MVN="mvn -q -B -s settings.xml"
JAR_COORD=local.androidx.test:monitor-from-source:1.8.0
REPO="${HOME}/.m2/repository"
if [ ! -s "$REPO/local/androidx/test/monitor-from-source/1.8.0/monitor-from-source-1.8.0.jar" ]; then
  $MVN dependency:resolve -DexcludeArtifactIds=monitor-from-source >/dev/null 2>&1 || true
  ANDROID_ALL="$(find "$REPO/org/robolectric/android-all/14-robolectric-10818077" -name '*.jar' | head -1)"
  if [ -z "$ANDROID_ALL" ]; then
    mkdir -p target
    curl -sSfL -o target/android-all.jar \
      https://maven-central.storage-download.googleapis.com/maven2/org/robolectric/android-all/14-robolectric-10818077/android-all-14-robolectric-10818077.jar
    ANDROID_ALL=target/android-all.jar
  fi
  mkdir -p target/androidx-test
  python3 tools/androidx_test_from_source.py target/androidx-test "$ANDROID_ALL" target/androidx-test/monitor.jar
  $MVN install:install-file -Dfile=target/androidx-test/monitor.jar -DgroupId=local.androidx.test \
    -DartifactId=monitor-from-source -Dversion=1.8.0 -Dpackaging=jar
fi
$MVN test
