#!/bin/bash
set -euo pipefail
: "${1:?Usage: bash restore-stock-home.sh TV_IP:PORT}"
adb -s "$1" shell pm enable --user 0 com.google.android.apps.tv.launcherx
adb -s "$1" shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
adb -s "$1" shell am start -n com.google.android.apps.tv.launcherx/.home.HomeActivity
echo 'Stock Home opened. Press the physical Home button and verify it returns to Google TV.'
