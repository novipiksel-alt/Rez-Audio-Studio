#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Creates an isolated local source repository and release metadata; never uploads.
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/package-source.sh
python3 scripts/verify-package-security.py dist/Rez-Audio-Studio-1.2-source-DRAFT.zip
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile
import json, re, subprocess
base=Path('dist/github')
repo=base/'Rez-Audio-Studio'
if repo.exists(): raise SystemExit('Local repository already exists; review it before creating a new package.')
base.mkdir(parents=True,exist_ok=True)
with ZipFile('dist/Rez-Audio-Studio-1.2-source-DRAFT.zip') as archive:
    for entry in archive.infolist():
        path=Path(entry.filename)
        if path.is_absolute() or '..' in path.parts or path.parts[0]!='Rez-Audio-Studio':
            raise SystemExit('Unsafe archive entry')
        data=archive.read(entry)
        if path.suffix in {'.jks','.p12','.pfx','.keystore','.apk','.class','.dex'}:
            raise SystemExit('Unexpected private or build artifact')
    archive.extractall(base)
for p in (repo/'scripts').glob('*.sh'): p.chmod(0o755)
subprocess.run(['git','init','-b','main',str(repo)],check=True,stdout=subprocess.DEVNULL)
subprocess.run(['git','-C',str(repo),'add','--all'],check=True)
tracked=subprocess.check_output(['git','-C',str(repo),'ls-files'],text=True).splitlines()
assert tracked and not any(any(part in p.split('/') for part in ['build','dist','.aws','.codex','.agents','fixtures']) for p in tracked)
assert not subprocess.check_output(['git','-C',str(repo),'remote'],text=True).strip()
(base/'release-v1.2.json').write_text(json.dumps({
    'tag_name':'v1.2','name':'Rez Audio Studio v1.2','draft':True,'prerelease':False,
    'body':(repo/'RELEASE-v1.2.md').read_text(),
},ensure_ascii=False,indent=2)+'\n')
print(f'PASS: local main branch with {len(tracked)} staged source files; no commit, remote or upload.')
print('Draft release metadata prepared. Production APK and final approval still required.')
PY
