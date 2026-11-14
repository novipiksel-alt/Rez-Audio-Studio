#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
TOOLS="$SDK/build-tools/36.0.0"
ANDROID_JAR="$SDK/platforms/android-36/android.jar"
BUILD="$PWD/build/manual"
rm -rf "$BUILD/classes" "$BUILD/gen" "$BUILD/dex"
mkdir -p "$BUILD/classes" "$BUILD/gen" "$BUILD/dex" dist
rm -rf "$BUILD/legal"
mkdir -p "$BUILD/legal"
cp LICENSE NOTICE THIRD_PARTY_NOTICES.md "$BUILD/legal/"
"$TOOLS/aapt2" compile --dir app/src/main/res -o "$BUILD/resources.zip"
"$TOOLS/aapt2" link -o "$BUILD/resources.apk" -I "$ANDROID_JAR" --manifest app/src/main/AndroidManifest.xml --java "$BUILD/gen" -A "$BUILD/legal" "$BUILD/resources.zip"
find app/src/main/java "$BUILD/gen" -name '*.java' -print > "$BUILD/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -bootclasspath "$ANDROID_JAR:$TOOLS/core-lambda-stubs.jar" -d "$BUILD/classes" @"$BUILD/sources.txt"
jar cf "$BUILD/classes.jar" -C "$BUILD/classes" .
"$TOOLS/d8" --lib "$ANDROID_JAR" --min-api 29 --output "$BUILD/dex" "$BUILD/classes.jar"
cp "$BUILD/resources.apk" "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q -j "$BUILD/unsigned.apk" classes*.dex)
"$TOOLS/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
if [[ "${1:-}" == "--unsigned" ]]; then
  cp "$BUILD/aligned.apk" dist/Rez-1.2-unsigned.apk
  exit 0
fi
if [[ $# -ne 0 ]]; then echo "Usage: $0 [--unsigned]" >&2; exit 1; fi
umask 077
if [[ ! -f "$BUILD/debug.keystore" ]]; then
  keytool -genkeypair -keystore "$BUILD/debug.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Rez Development,O=Rez,C=RS' -keyalg RSA -keysize 2048 -validity 10000 -noprompt >/dev/null 2>&1
fi
"$TOOLS/apksigner" sign --ks "$BUILD/debug.keystore" --ks-pass pass:android --key-pass pass:android --out dist/Rez-1.2.apk "$BUILD/aligned.apk"
"$TOOLS/apksigner" verify --verbose dist/Rez-1.2.apk
sha256sum dist/Rez-1.2.apk > dist/Rez-1.2.apk.sha256
printf '\nAPK: %s/dist/Rez-1.2.apk\n' "$PWD"
