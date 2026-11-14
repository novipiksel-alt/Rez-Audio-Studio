#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/generate-fixtures.sh
./scripts/build.sh
SDK="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
TOOLS="$SDK/build-tools/36.0.0"
ANDROID_JAR="$SDK/platforms/android-36/android.jar"
BUILD="$PWD/build/device-tests"
rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/assets" "$BUILD/dex"
for file in cbr.mp3 vbr.mp3 pcm16.wav audio.aac audio.m4a vorbis.ogg opus.ogg audio.flac; do cp "tests/fixtures/$file" "$BUILD/assets/"; done
"$TOOLS/aapt2" link -o "$BUILD/resources.apk" -I "$ANDROID_JAR" --manifest tests/android/AndroidManifest.xml -A "$BUILD/assets"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -bootclasspath "$ANDROID_JAR:$TOOLS/core-lambda-stubs.jar" -cp build/manual/classes.jar -d "$BUILD/classes" tests/android/PlatformTests.java
jar cf "$BUILD/classes.jar" -C "$BUILD/classes" .
"$TOOLS/d8" --lib "$ANDROID_JAR" --classpath build/manual/classes.jar --min-api 29 --output "$BUILD/dex" "$BUILD/classes.jar"
cp "$BUILD/resources.apk" "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q -j "$BUILD/unsigned.apk" classes*.dex)
"$TOOLS/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
"$TOOLS/apksigner" sign --ks build/manual/debug.keystore --ks-pass pass:android --out dist/Rez-platform-tests.apk "$BUILD/aligned.apk"
"$TOOLS/apksigner" verify dist/Rez-platform-tests.apk
printf 'Test APK: dist/Rez-platform-tests.apk\n'
