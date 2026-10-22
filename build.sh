#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export GRADLE_USER_HOME="$PWD/.gradle-cache"
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p .tools
if [ ! -x .tools/gradle-8.7/bin/gradle ]; then
    curl --fail --location --retry 2 https://services.gradle.org/distributions/gradle-8.7-bin.zip -o .tools/gradle-8.7-bin.zip
    curl --fail --location --retry 2 https://services.gradle.org/distributions/gradle-8.7-bin.zip.sha256 -o .tools/gradle-8.7-bin.zip.sha256
    EXPECTED="$(tr -d '[:space:]' < .tools/gradle-8.7-bin.zip.sha256)"
    [[ "$EXPECTED" =~ ^[0-9a-fA-F]{64}$ ]] || { echo 'Invalid checksum response' >&2; exit 1; }
    ACTUAL="$(shasum -a 256 .tools/gradle-8.7-bin.zip | awk '{print $1}')"
    [ "$EXPECTED" = "$ACTUAL" ] || { echo 'Gradle checksum mismatch; stopping.' >&2; exit 1; }
    unzip -q .tools/gradle-8.7-bin.zip -d .tools
fi
.tools/gradle-8.7/bin/gradle --no-daemon --console=plain :app:assembleDebug
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify app/build/outputs/apk/debug/app-debug.apk
echo "Built: $PWD/app/build/outputs/apk/debug/app-debug.apk"
