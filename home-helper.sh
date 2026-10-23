#!/bin/bash
set -euo pipefail
: "${1:?Usage: bash home-helper.sh DEVICE_SERIAL [off]}"
if [[ "${2:-}" == "off" ]]; then
    adb -s "$1" shell am start -n dev.cleanmytv.launcher/.HomeHelperSettingsActivity \
        --ez disable_home_helper true
elif [[ -z "${2:-}" ]]; then
    adb -s "$1" shell am start -n dev.cleanmytv.launcher/.HomeHelperSettingsActivity
else
    echo 'Usage: bash home-helper.sh DEVICE_SERIAL [off]' >&2
    exit 2
fi
