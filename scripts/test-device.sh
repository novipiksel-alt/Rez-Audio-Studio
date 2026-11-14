#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/build.sh
./scripts/build-device-tests.sh
adb install -r dist/Rez-1.2.apk
adb install -r dist/Rez-platform-tests.apk
result="$(adb shell am instrument -w rs.rez.audio.tests/rs.rez.audio.PlatformTests)"
printf '%s\n' "$result"
if ! printf '%s\n' "$result" | rg -q 'PASS [0-9]+ Android assertions'; then
  exit 1
fi
