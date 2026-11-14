# Dependencies and third-party notices

Review date: 2026-11-14. Project license: Apache-2.0 (see LICENSE).
This inventory describes the available source tree and documented manual build;
it is not a claim that third-party tools are relicensed under Apache-2.0.

## Included project materials

The six Java files in `app/src/main/java/rs/rez/audio`, the Android XML manifests
and resources (including the vector launcher icon), the Java/Python tests,
shell scripts and Gradle configuration contain no identified third-party license
headers, vendored modules or imported third-party runtime libraries. Imports
refer only to Java and Android platform APIs. No JAR, AAR, native library, font,
Seal source or Seal APK is supplied in the draft source archive.

This is a source inspection result, not proof of authorship. The available
`.git` directory has no history. The owner must confirm source/icon provenance
and permission from any prior contributors before public distribution. If a
copied component is identified, preserve its actual license and attribution;
do not assume this project's SPDX headers override it.

## External components

| Component | Use and distribution scope | License information |
| --- | --- | --- |
| Android platform APIs (API 29+, compile SDK 36) | Provided by the Android device; android.jar is a compile-time input, not packaged in APK | AOSP includes multiple licenses; see [AOSP licensing](https://source.android.com/docs/setup/about/licenses). No blanket relicensing claim is made for the platform. |
| Android SDK Build Tools 36.0.0: aapt2, D8, zipalign, apksigner | Local build/sign tools; executables and jars are excluded from source archive and APK | Installed SDK supplies `build-tools/36.0.0/NOTICE.txt`; retain that file if redistributing the tools. [D8/R8 license](https://r8.googlesource.com/r8/+/refs/heads/main/LICENSE) is BSD-3-Clause; its repository also has third-party notices. |
| Android Gradle plugin 8.13.0 | Declared in root build.gradle; not used by the validated manual build | [Android tools source](https://android.googlesource.com/platform/tools/base/); the resolved artifact and transitive dependency licenses have not been audited here. No Gradle wrapper or tool binaries are distributed. |
| JDK 17 / Java standard library | javac/jar/keytool tools; runtime platform APIs, not a bundled JDK | License varies by JDK distribution; consult the installed JDK's legal files. JDK binaries are not included. |
| FFmpeg / ffprobe (including local MP3/Vorbis/Opus encoders) | Only generates synthetic test audio and validates exports; not linked or bundled with Rez | [FFmpeg legal information](https://ffmpeg.org/legal.html): LGPL/GPL status depends on build options. Consult local build license/configuration before redistributing FFmpeg itself. |
| Python 3, Bash, zip, ripgrep, Android platform-tools/adb | Local test/build/check utilities | Not bundled. Their own licenses remain applicable if separately redistributed. |
| Seal (`com.junkfood.seal`) | Separately installed app opened with an explicit Android SEND intent; not linked, copied or bundled | Upstream [GPLv3 license](https://github.com/JunkFood02/Seal/blob/main/LICENSE). The observed separation supplies no Seal code to relicense under Apache-2.0. Any future copied code or bundling needs a fresh review. |

## Test audio and generated files

`tests/fixtures/` is excluded from Git and the draft source archive. Existing
binaries have no recorded provenance and are not authorized for publication by
this review. Tests regenerate synthetic sine waves locally using
`scripts/generate-fixtures.sh`; `tests/AudioCoreTest.java` constructs malformed
and derived cases. Tests should be run from the clean archive to avoid relying
on old binary fixtures. No music or downloaded audio is required.

Generated R classes and D8 lambda implementations are build output. The final
APK must be inspected for unexpected packaged code. Local inspection found only
`rs.rez.audio` class descriptors in the manual APK, including generated lambda
classes; the build does not request bundled desugared standard libraries.

## Distribution

Keep LICENSE, NOTICE and this file in source distributions and alongside the
production APK (for example as release attachments). They are also packaged as
APK assets by both the manual and Gradle build paths. Preserve any applicable
third-party notices if dependencies or build inputs change. No mandatory
third-party attribution for copied source has been identified in the current
project source; provenance confirmation remains outstanding.
