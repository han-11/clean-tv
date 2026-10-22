#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
: "${1:?Usage: bash install-test.sh TV_IP:PORT}"
APK=app/build/outputs/apk/debug/app-debug.apk
test -f "$APK" || { echo 'Run bash build.sh first.' >&2; exit 1; }
adb -s "$1" push "$APK" /data/local/tmp/cleanmytv-launcher.apk
adb -s "$1" shell pm install -r /data/local/tmp/cleanmytv-launcher.apk
adb -s "$1" shell am start -n dev.cleanmytv.launcher/.HomeActivity
echo 'Opened for testing only. No default Home or package enabled state was changed.'
