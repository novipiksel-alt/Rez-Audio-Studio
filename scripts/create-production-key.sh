#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Run locally with two dedicated permanent directories outside the source tree.
set -euo pipefail
set +x
[[ $# == 2 ]] || { echo 'Usage: create-production-key.sh KEY_DIRECTORY BACKUP_DIRECTORY' >&2; exit 1; }
python3 - "$1" "$2" "$(cd "$(dirname "$0")/.." && pwd)" <<'PY'
from pathlib import Path
import hashlib, os, secrets, shutil, subprocess, sys
os.umask(0o077)
primary, backup, project = (Path(p).expanduser().resolve() for p in sys.argv[1:])
for p in (primary, backup):
    if p == project or project in p.parents or p == Path('/tmp') or Path('/tmp') in p.parents:
        raise SystemExit('Use permanent directories outside the project and temporary storage.')
if primary == backup or primary in backup.parents or backup in primary.parents:
    raise SystemExit('Primary and backup directories must be separate.')
for p in (primary, backup):
    p.mkdir(parents=True, exist_ok=True)
    if any(p.iterdir()): raise SystemExit('Use empty dedicated signing directories; existing files will not be overwritten.')
    p.chmod(0o700)
password = primary/'store-password.txt'
key = primary/'rez-release.p12'
with password.open('x') as f: f.write(secrets.token_urlsafe(48)+'\n')
try:
    subprocess.run(['keytool','-genkeypair','-storetype','PKCS12','-keystore',str(key),
        '-storepass:file',str(password),'-keypass:file',str(password),'-alias','rez-release',
        '-dname','CN=Rez Audio Studio, O=Rez Audio Studio, C=BA','-keyalg','RSA',
        '-keysize','3072','-validity','10000','-noprompt'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
    certificate=primary/'production-cert.der'
    subprocess.run(['keytool','-exportcert','-keystore',str(key),'-storepass:file',str(password),
        '-alias','rez-release','-file',str(certificate)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
    (primary/'certificate-sha256.txt').write_text(hashlib.sha256(certificate.read_bytes()).hexdigest()+'\n')
    shutil.copyfile(key,backup/key.name)
    (backup/key.name).chmod(0o600)
    if key.read_bytes() != (backup/key.name).read_bytes(): raise RuntimeError('Backup mismatch')
    subprocess.run(['keytool','-list','-keystore',str(backup/key.name),'-storepass:file',str(password)],
        check=True,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
except Exception:
    raise SystemExit('Signing setup did not complete. Preserve any created key and credential files; inspect locally without sharing secrets.')
print('Production key and verified encrypted-keystore backup created. No secrets printed.')
print('Store the credential file in a password manager or separate secure backup; never upload it.')
PY
