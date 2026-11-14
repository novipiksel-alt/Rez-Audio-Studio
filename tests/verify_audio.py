# SPDX-License-Identifier: Apache-2.0
"""Independent FFmpeg checks: actual decoding, timing and compressed packet hashes."""
import hashlib
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parent / 'fixtures'

def probe(path):
    return json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_format', '-show_streams', '-show_packets', '-show_data_hash', 'sha256', '-of', 'json', str(path)]))

checks = 0
for name in ['cbr.mp3', 'vbr.mp3', 'mpeg2.mp3', 'mpeg25.mp3', 'pcm16.wav', 'pcm24.wav', 'float.wav', 'odd.wav', 'audio.aac']:
    src, cut = root / name, root / ('cut-' + name)
    a, b = probe(src), probe(cut)
    assert a['streams'][0]['codec_name'] == b['streams'][0]['codec_name'], name
    assert a['streams'][0]['sample_rate'] == b['streams'][0]['sample_rate'], name
    assert a['streams'][0]['channels'] == b['streams'][0]['channels'], name
    duration = float(b['format']['duration'])
    assert abs(duration - 2.554) < .12, (name, duration)
    # FFmpeg decodes every sample independently of the implementation under test.
    decoded = subprocess.run(['ffmpeg', '-v', 'error', '-i', str(cut), '-f', 'null', '-'], capture_output=True)
    assert decoded.returncode == 0 and not decoded.stderr, (name, decoded.stderr.decode())
    if name.endswith(('.mp3', '.aac')):
        original = [p['data_hash'] for p in a['packets']]
        exported = [p['data_hash'] for p in b['packets']]
        assert any(original[i:i+len(exported)] == exported for i in range(len(original))), name + ': packet payload changed'
    else:
        def pcm(path):
            return subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(path), '-f', 'f32le', '-acodec', 'pcm_f32le', '-'])
        original, exported = pcm(src), pcm(cut)
        sample = 1_235_000 * int(a['streams'][0]['sample_rate']) // 1_000_000
        byte_offset = sample * int(a['streams'][0]['channels']) * 4
        assert original[byte_offset:byte_offset + len(exported)] == exported, name + ': decoded PCM changed'
    checks += 6
    print(f'PASS FFmpeg: {name}, {duration:.3f} s, decodes cleanly; same codec, rate and channels')
print(f'PASS {checks} independent format/decoder/payload checks')
