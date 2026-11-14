#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Passwords are requested interactively by apksigner; never pass them as arguments.
set -euo pipefail
umask 077
cd "$(dirname "$0")/.."
: "${REZ_RELEASE_KEYSTORE:?Set absolute path to production keystore outside this project}"
: "${REZ_RELEASE_ALIAS:?Set production key alias}"
: "${REZ_RELEASE_CERT_SHA256:?Set independently recorded production certificate SHA-256 (hex)}"
KEY="$(realpath "$REZ_RELEASE_KEYSTORE")"
case "$KEY" in "$PWD"/*) echo 'Production key must be outside this project' >&2; exit 1;; esac
TOOLS="${ANDROID_SDK_ROOT:-/opt/android-sdk}/build-tools/36.0.0"
./scripts/build.sh --unsigned
TMP="$(mktemp -d "$PWD/build/release.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT
SIGN_ARGS=(--ks "$KEY" --ks-key-alias "$REZ_RELEASE_ALIAS")
if [[ -n "${REZ_RELEASE_PASSWORD_FILE:-}" ]]; then
  PASSWORD_FILE="$(realpath "$REZ_RELEASE_PASSWORD_FILE")"
  case "$PASSWORD_FILE" in "$PWD"/*) echo 'Password file must be outside project' >&2; exit 1;; esac
  SIGN_ARGS+=(--ks-pass "file:$PASSWORD_FILE" --key-pass "file:$PASSWORD_FILE")
fi
"$TOOLS/apksigner" sign "${SIGN_ARGS[@]}" --out "$TMP/release.apk" dist/Rez-1.2-unsigned.apk
"$TOOLS/zipalign" -c -p 4 "$TMP/release.apk"
"$TOOLS/apksigner" verify --verbose --print-certs "$TMP/release.apk" > "$TMP/verification.txt"
EXPECTED="$(printf '%s' "$REZ_RELEASE_CERT_SHA256" | tr -d ':' | tr '[:upper:]' '[:lower:]')"
[[ "$EXPECTED" =~ ^[0-9a-f]{64}$ ]] || { echo 'Invalid SHA-256 fingerprint' >&2; exit 1; }
ACTUAL="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$TMP/verification.txt" | tr '[:upper:]' '[:lower:]')"
[[ "$ACTUAL" == "$EXPECTED" ]] || { echo 'Production certificate mismatch' >&2; exit 1; }
if rg -qi 'Android Debug|Rez Development' "$TMP/verification.txt"; then
  echo 'Development certificate rejected' >&2; exit 1
fi
rg -qx 'Signer #1 certificate DN: CN=Rez Audio Studio, O=Rez Audio Studio, C=BA' "$TMP/verification.txt" || { echo 'Unexpected production identity' >&2; exit 1; }
python3 tests/verify_localization.py "$TMP/release.apk"
"$TOOLS/aapt2" dump badging "$TMP/release.apk" | rg -q "^package: name='rs.rez.audio' versionCode='3' versionName='1.2'" || { echo 'Unexpected APK version' >&2; exit 1; }
mv "$TMP/release.apk" dist/Rez-Audio-Studio-v1.2.apk
cp "$TMP/verification.txt" dist/Rez-Audio-Studio-v1.2-verification.txt
(cd dist && sha256sum Rez-Audio-Studio-v1.2.apk > Rez-Audio-Studio-v1.2.apk.sha256)
echo 'Production candidate ready for device testing and final approval; nothing published.'
