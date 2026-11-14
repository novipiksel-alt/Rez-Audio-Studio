#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Scan publishable source content; report locations, never matched secret values."""
from pathlib import Path
from zipfile import ZipFile
import re
import subprocess
import sys

RULES = [
    ('personal home path', re.compile(rb'\x2f(?:home|Users)\x2f[^/\s]+/|[A-Za-z]:\\Users\\[^\\\s]+\\')),
    ('private identity', re.compile(re.escape(b'Sn' + b'1p' + b'3r'), re.I)),
    ('private key', re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH |DSA |ENCRYPTED )?PRIVATE KEY-----')),
    ('AWS access key', re.compile(rb'(?:AKIA|ASIA)[A-Z0-9]{16}')),
    ('GitHub token', re.compile(rb'gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}')),
]
PRIVATE_SUFFIXES = {'.jks', '.p12', '.pfx', '.keystore', '.key', '.pem', '.pass', '.passwd', '.secret'}
BUILD_SUFFIXES = {'.apk', '.class', '.dex', '.aar', '.jar'}
PRIVATE_PARTS = {'.aws', '.codex', '.agents', '.gradle', 'build', 'dist', 'fixtures', '.idea'}


def check(name, data):
    path = Path(name)
    findings = []
    if path.is_absolute() or '..' in path.parts:
        findings.append((name, 1, 'unsafe package path'))
    if (path.suffix.lower() in PRIVATE_SUFFIXES | BUILD_SUFFIXES
            or any(p in PRIVATE_PARTS for p in path.parts)
            or path.name.startswith('.env')
            or re.search(r'(?:password|credential|secret).*\.(?:txt|json|properties)$', path.name, re.I)):
        findings.append((name, 1, 'private or generated file'))
    for label, pattern in RULES:
        for match in pattern.finditer(data):
            line = data[:match.start()].count(b'\n') + 1
            findings.append((name, line, label))
    return findings


def scan(target):
    if target.is_file():
        with ZipFile(target) as archive:
            return [(item.filename, archive.read(item)) for item in archive.infolist() if not item.is_dir()]
    names = subprocess.check_output(['git', '-C', str(target), 'ls-files', '-z']).decode().split('\0')
    entries = []
    for name in filter(None, names):
        path = target / name
        if path.is_symlink():
            raise SystemExit('FAIL: symlink in source repository: ' + name)
        entries.append((name, path.read_bytes()))
    return entries


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise SystemExit('Usage: verify-package-security.py SOURCE_ZIP_OR_LOCAL_GIT_DIRECTORY')
    entries = scan(Path(sys.argv[1]))
    if not entries:
        raise SystemExit('FAIL: empty source package')
    findings = [finding for name, data in entries for finding in check(name, data)]
    for name, line, label in findings:
        print(f'FAIL {name}:{line}: {label}')
    if findings:
        raise SystemExit(1)
    print(f'PASS security: {len(entries)} source files; no personal paths, recognized secret patterns, key files or build artifacts')
