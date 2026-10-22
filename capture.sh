#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
: "${1:?Usage: bash capture.sh TV_IP:PORT}"
mkdir -p shots
adb -s "$1" exec-out screencap -p > shots/launcher.png
test -s shots/launcher.png || { echo 'TV returned an empty screenshot.' >&2; exit 1; }
sips -Z 1280 shots/launcher.png --out shots/launcher-small.png
echo "Screenshot: $PWD/shots/launcher-small.png"
