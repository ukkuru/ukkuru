#!/usr/bin/env bash
# Builds a signed, installable Maze Makers test APK without the Android SDK.
#
# Needs: JDK 17+, python3, curl. Downloads android.jar (compile only), D8 and
# apksig into app/.tools on first run. Output: app/build/maze-makers-<version>.apk
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$HERE/.tools"
OUT="$HERE/build"
VERSION="$(grep -o 'VERSION = "[^"]*"' "$HERE/src/com/ukkuru/mazemakers/BuildInfo.java" | cut -d'"' -f2)"
VERSION_CODE=1
PACKAGE=com.ukkuru.mazemakers
MIN_SDK=29     # Android 10 (PRD section 6)
TARGET_SDK=34

mkdir -p "$TOOLS"
fetch() { [ -s "$TOOLS/$1" ] || curl -sSfL -o "$TOOLS/$1" "$2"; }
fetch android.jar https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar
fetch r8lib.jar https://storage.googleapis.com/r8-releases/raw/8.5.35/r8lib.jar
fetch apksig.jar https://repo1.maven.org/maven2/com/android/tools/build/apksig/2.3.0/apksig-2.3.0.jar

rm -rf "$OUT" && mkdir -p "$OUT/host" "$OUT/classes" "$OUT/dex" "$OUT/tools"

echo "== Core tests (shared simulation, host JVM)"
javac -nowarn -d "$OUT/host" "$HERE"/src/com/ukkuru/mazemakers/core/*.java "$HERE/test/CoreTest.java"
java -cp "$OUT/host" CoreTest

echo "== Compile app"
mkdir -p "$OUT/stubs"
javac -nowarn -source 8 -target 8 -Xlint:-options -bootclasspath "$TOOLS/android.jar" \
  -d "$OUT/stubs" "$HERE/tools/stubs/java/lang/invoke/LambdaMetafactory.java"
javac -nowarn -source 8 -target 8 -Xlint:-options -encoding UTF-8 \
  -bootclasspath "$TOOLS/android.jar" -classpath "$OUT/stubs" -d "$OUT/classes" \
  $(find "$HERE/src" -name '*.java')

echo "== Dex"
java -cp "$TOOLS/r8lib.jar" com.android.tools.r8.D8 --release --min-api $MIN_SDK \
  --lib "$TOOLS/android.jar" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

echo "== Manifest and package"
python3 "$HERE/tools/axml.py" "$OUT/AndroidManifest.xml" $PACKAGE $VERSION_CODE "$VERSION" \
  $MIN_SDK $TARGET_SDK "Maze Makers" $PACKAGE.MainActivity
python3 - "$OUT" <<'EOF'
import sys, zipfile
out = sys.argv[1]
with zipfile.ZipFile(f"{out}/unsigned.apk", "w") as z:
    z.write(f"{out}/AndroidManifest.xml", "AndroidManifest.xml", zipfile.ZIP_DEFLATED)
    z.write(f"{out}/dex/classes.dex", "classes.dex", zipfile.ZIP_DEFLATED)
EOF

echo "== Sign"
KEYSTORE="$HERE/.tools/test-signing.p12"
if [ ! -s "$KEYSTORE" ]; then
  keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -storepass mazemakers -keypass mazemakers \
    -alias tester -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Maze Makers Test, O=Maze Makers" >/dev/null 2>&1
fi
javac -nowarn -cp "$TOOLS/apksig.jar" -d "$OUT/tools" "$HERE/tools/Sign.java"
APK="$OUT/maze-makers-$VERSION.apk"
# The Maven Central apksig (2.3.0) touches a JDK internal class while loading.
java --add-exports java.base/sun.security.x509=ALL-UNNAMED -cp "$TOOLS/apksig.jar:$OUT/tools" Sign "$OUT/unsigned.apk" "$APK" "$KEYSTORE" mazemakers tester
ls -l "$APK"
