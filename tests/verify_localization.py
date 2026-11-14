# SPDX-License-Identifier: Apache-2.0
"""Catalog completeness, placeholders, diagnostic coverage and packaged locales."""
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
res = root / 'app/src/main/res'

def catalog(folder):
    entries = ET.parse(res / folder / 'strings.xml').getroot().findall('string')
    assert len(entries) == len({e.attrib['name'] for e in entries}), folder
    return {e.attrib['name']: ''.join(e.itertext()).replace("\\'", "'").replace('\\"', '"') for e in entries}

base = catalog('values')
assert base['app_version'] == '1.2'
fixed = {'app_version'}
print(f'PASS en: {len(base)} default strings')
pattern = r'%(\d+)\$[-+0-9.]*[sdf]'
for locale in ['bs', 'de', 'it', 'es']:
    translated = catalog('values-' + locale)
    assert translated.keys() == base.keys() - fixed, locale
    for key, value in translated.items():
        assert value.strip(), (locale, key)
        assert sorted(re.findall(pattern, base[key])) == sorted(re.findall(pattern, value)), (locale, key)
        assert sorted(re.findall(r'%\d+\$[-+0-9.]*([sdf])', base[key])) == sorted(re.findall(r'%\d+\$[-+0-9.]*([sdf])', value)), (locale, key)
    print(f'PASS {locale}: {len(translated)} translated strings; complete keys and matching placeholders')

bs = catalog('values-bs')
adapter = (root / 'app/src/main/java/rs/rez/audio/LocalizedErrors.java').read_text()
known = {bs[key] for key in re.findall(r'R.string.(\w+)', adapter)}
for name in ['AudioEngine', 'LosslessAudio', 'TimeRange']:
    source = (root / f'app/src/main/java/rs/rez/audio/{name}.java').read_text()
    for expression in re.findall(r'throw new (?:IOException|IllegalArgumentException)\(([^;]+)\);', source):
        parts = re.findall(r'"([^"\\]*)"', expression)
        if not parts: continue
        if name == 'AudioEngine' and parts[0] == 'Ovaj kodek (':
            expected = parts[0] + '%1$s' + parts[1]
        elif name == 'LosslessAudio' and parts[0].endswith('poziciji '):
            expected = parts[0] + '%1$s' + parts[1]
        else: expected = parts[0]
        assert expected in known, (name, expected)
print('PASS all unchanged engine/range diagnostic messages have localized UI mappings')

ui = (root / 'app/src/main/java/rs/rez/audio/MainActivity.java').read_text()
assert 'Locale.setDefault' not in ui and 'setLocales' not in ui
assert 'e.getMessage()' not in ui
# User-facing calls must use resources, dynamic values or empty text.
for literal in re.findall(r'"((?:[^"\\]|\\.)*)"', ui):
    assert not any(ord(c) > 127 for c in literal), literal
assert "versionCode 3; versionName '1.2'" in (root / 'app/build.gradle').read_text()
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
assert manifest.attrib[android+'versionCode'] == '3'
assert manifest.attrib[android+'versionName'] == '1.2'
assert manifest.find('application').attrib[android+'label'] == '@string/app_name'
print('PASS system-driven locale selection, localized UI calls and version 1.2 (3)')

if len(sys.argv) > 1:
    sdk = Path(__import__('os').environ.get('ANDROID_SDK_ROOT', '/opt/android-sdk'))
    dump = subprocess.check_output([str(sdk/'build-tools/36.0.0/aapt2'), 'dump', 'resources', sys.argv[1]], text=True)
    for key in base:
        match = re.search(r' string/'+key+r'\n(.*?)(?=    resource |  type |\Z)', dump, re.S)
        assert match, key
        for locale in ([''] if key in fixed else ['', 'bs', 'de', 'it', 'es']):
            assert '('+locale+')' in match.group(1), (key, locale)
    print(f'PASS APK: {len(base)} strings compiled; all five language configurations present')
