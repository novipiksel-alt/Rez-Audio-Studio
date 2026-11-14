#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/tests tests/fixtures
./scripts/generate-fixtures.sh
python3 - <<'PY'
import wave
with wave.open('tests/fixtures/odd.wav','wb') as f:
    f.setnchannels(1);f.setsampwidth(1);f.setframerate(11025);f.writeframes(bytes(range(256))*345)
PY
javac -encoding UTF-8 -d build/tests app/src/main/java/rs/rez/audio/TimeRange.java app/src/main/java/rs/rez/audio/LosslessAudio.java tests/AudioCoreTest.java
java -cp build/tests rs.rez.audio.AudioCoreTest "$PWD/tests/fixtures"
python3 tests/verify_audio.py

python3 tests/verify_localization.py
