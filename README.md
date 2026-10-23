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

### Optional Home helper (device test required)

This branch adds an opt-in accessibility service inside CleanMyTV. Installation
alone does not enable it, change default Home, or disable any packages. The
launcher layout, fonts, app order, and wallpaper rotation are unchanged.

After building and installing with the existing scripts, run:

    bash home-helper.sh DEVICE_SERIAL

Before enabling anything, briefly hold Volume Up on the physical remote and
note whether it changes continuously. In the helper screen, select Enable Home
helper, then enable CleanMyTV Home helper in Android Accessibility settings.
If sideloaded-app restricted settings block access, inspect the TV's app-info
screen for its supported Allow restricted settings action. Do not change secure
settings or grant broad shell permissions as a workaround.

Test physical Home from YouTube, Netflix, TV Settings, and an HDMI input. Verify
it opens CleanMyTV and that Google TV remains available in system settings.
Also test Back, D-pad, Input, Settings, dedicated app buttons, and held-volume
repeats. Injected adb key events do not replace these physical-remote checks.

The helper handles Home presses, opening once on release; Home holds and double
presses also return to CleanMyTV instead of retaining their stock actions.
Other keys pass through unchanged
in the code, but TCL firmware can still lose held-key repeats when an
accessibility key filter is active. If behaviour changes, stop the test and
turn the service off in Android Accessibility settings.

If Home still opens Google TV, first check the helper setup screen says it is
connected. If connected, the firmware may reserve Home before accessibility;
do not disable LauncherX or SetupWraith to work around it. No device success is
claimed until the actual remote has been tested.

To turn it off, use the helper screen or Android Accessibility settings. While
adb is connected, the helper screen's off action is also available with:

    bash home-helper.sh DEVICE_SERIAL off

The off action disarms interception and asks the connected service to disable
itself. Confirm the Android accessibility toggle is off and Home/volume work as
before. Uninstalling only CleanMyTV through TV Settings also removes the helper.
The existing restore-stock-home.sh does not disable this helper by itself.

The service cannot read window contents, takes no screenshots, uses no network
access, and makes no system package changes. It receives key events solely to
handle Home; all other keys are forwarded.

Automatic launching after TV startup is deliberately a separate, unimplemented
step. Confirm Home and remote behaviour first. Then test a reboot with this
helper enabled to establish that Home access and normal TV controls recover.

Development checks: run :app:testDebugUnitTest and :app:assembleDebug with
Gradle 8.7. Unit tests exercise key forwarding, cancelled presses, and repeats;
they cannot establish how TCL firmware routes physical Home events.
The debug build and seven unit tests passed in the cloud development environment.
Lint reports three pre-existing errors, verified against unchanged main:
MissingTvBanner and two WrongConstant font-style errors in HomeActivity.
Those are not introduced or suppressed by this helper.

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
