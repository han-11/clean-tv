# CleanMyTV

Native Android TV launcher, tested on a TCL C755 running Android 14.

## Appearance

- Four columns with centred, rounded app images.
- No labels underneath or tile backgrounds.
- An unfilled outline marks the focused image.
- Photo wallpaper with the existing dark overlay and no visible app colour tint.

App order:
1. 爱壹帆/IYF, Bilibili, YouTube, Netflix.
2. Apple TV, Disney+, Spotify, Prime Video.
3. Remaining available apps follow below.

Configure app ordering in app/src/main/res/values/config.xml.

## Wallpapers

Seven bundled photos live in app/src/main/assets/wall-1.jpg through wall-7.jpg.
They rotate in order every 60 seconds while the launcher is visible.
Rotation pauses when another app covers the launcher.
Replace those images and rebuild to change the photos.
The existing overlay and image cropping remain unchanged.

## Build and test

Requires macOS, Java 17, Android SDK platform 34, build-tools 34.0.0, and adb.

Run: bash build.sh

Use adb devices -l to find the current connected device serial.
Run: bash install-test.sh DEVICE_SERIAL

The install script opens CleanMyTV for testing.
It does not change default Home or disable system packages.
Run: bash capture.sh DEVICE_SERIAL
Screenshots are saved under shots/.

## TV behaviour and recovery

The physical Home button currently returns to Google TV on this TCL firmware.
Keep Google TV LauncherX and SetupWraith enabled.
Disabling the stock launcher previously required a factory reset.

The restore-stock-home.sh script re-enables and opens Google TV;
it is not a full device restore or a general debloat undo script.

No debloat changes are established by this project.
Preserve casting, firmware updates, TV inputs, remote/Bluetooth services,
keyboard, Play Store, and Google Play Services.

Weather is not included.

## Development and security

Build tools, caches, generated APKs, screenshots, and local.properties are ignored.
Keep the existing Android debug signing key for compatible app updates.
Use existing checkouts in isolated cloud tasks; do not create a worktree
unless explicitly requested.

When device work is finished, turn off Wireless debugging on the TV
and forget the paired computer if ongoing access is unnecessary.
