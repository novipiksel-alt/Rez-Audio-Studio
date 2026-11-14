#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p tests/fixtures
make_audio() {
  local name="$1" rate="$2" channels="$3"; shift 3
  if [[ ! -f "tests/fixtures/$name" ]]; then
    ffmpeg -hide_banner -loglevel error -f lavfi -i "sine=frequency=523:duration=8:sample_rate=$rate" -ac "$channels" "$@" -y "tests/fixtures/$name"
  fi
}
make_audio cbr.mp3 44100 2 -c:a libmp3lame -b:a 192k
make_audio vbr.mp3 48000 2 -c:a libmp3lame -q:a 2
make_audio mpeg2.mp3 22050 1 -c:a libmp3lame -b:a 48k
make_audio mpeg25.mp3 11025 1 -c:a libmp3lame -b:a 24k
make_audio pcm16.wav 44100 2 -c:a pcm_s16le
make_audio pcm24.wav 48000 2 -c:a pcm_s24le
make_audio float.wav 48000 2 -c:a pcm_f32le
make_audio audio.aac 44100 2 -c:a aac -b:a 160k -f adts
make_audio audio.m4a 44100 2 -c:a aac -b:a 160k
make_audio vorbis.ogg 48000 2 -c:a libvorbis -q:a 4
make_audio opus.ogg 48000 2 -c:a libopus -b:a 128k
make_audio audio.flac 44100 2 -c:a flac
