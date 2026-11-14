#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p dist
# Explicit list: never archive the whole working directory or local metadata.
python3 - <<'PY'
from pathlib import Path
import zipfile
root = Path.cwd()
files = [Path(n) for n in ['.gitignore', 'README.md', 'CHANGELOG-1.1.md', 'RELEASE-v1.1.md', 'CHANGELOG-1.2.md', 'RELEASE-v1.2.md', 'build.gradle', 'settings.gradle', 'LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md']]
for folder, suffixes in [('app/src', {'.java', '.xml'}), ('scripts', {'.sh', '.py'}), ('docs', {'.md'}), ('tests', {'.java', '.py', '.xml'})]:
    files.extend(p for p in Path(folder).rglob('*') if p.is_file() and p.suffix in suffixes)
files.append(Path('app/build.gradle'))
with zipfile.ZipFile('dist/Rez-Audio-Studio-1.2-source-DRAFT.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for p in sorted(set(files)):
        if p.is_symlink() or not p.resolve().is_relative_to(root):
            raise SystemExit(f'Unsafe source path: {p}')
        archive.write(p, 'Rez-Audio-Studio/' + str(p))
print('Draft source archive ready; rights verification and final approval required before publication.')
PY
