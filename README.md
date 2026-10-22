# CleanMyTV — TCL C755 first test build

This is new launcher source, not the reference launcher mentioned in the original prompt.
It has not yet been compiled or tested on your Mac or TV. Keep Google TV enabled and
the default until installation, screenshots, navigation, inputs, and app launches pass.

## Build and open for testing

Java 17, Android platform 34, build-tools 34.0.0, and adb are required.
The build downloads Gradle 8.7 over HTTPS, verifies its published SHA-256, and
downloads Android Gradle Plugin and AndroidX RecyclerView dependencies from their repositories.
In Terminal, `cd` into this extracted directory, then:

```sh
bash build.sh
bash install-test.sh 192.168.1.232:34643
```

Use the current Wireless debugging connection port if it changes. Accept any
installation confirmation shown on the TV. The installation script opens the app
directly; it does not change default Home or disable packages.

The APK is `app/build/outputs/apk/debug/app-debug.apk`. Keep your Mac's Android debug
signing key for subsequent updates. No custom signing keys or credentials are bundled.

## Check on the actual TV

Capture the launcher before pressing Home. Confirm the dock order, then test each
app, return by directly opening the launcher again, and check grid movement with
the physical remote. Up/Down moves a row; Up from the dock reaches the top buttons.
Top-bar Left/Right stays within that bar. Test Inputs and Wi-Fi/Settings buttons.
Pressing Home during this phase should still open Google TV.

```sh
adb -s 192.168.1.232:34643 shell am start -n dev.cleanmytv.launcher/.HomeActivity
bash capture.sh 192.168.1.232:34643
```

The reduced screenshot is `shots/launcher-small.png`; share that for review. Secure
video playback may not produce useful screenshots; verify playback on the TV itself.

## Configure and rebuild

Edit `app/src/main/res/values/config.xml`: `columns`, `dock_apps`, and corresponding
`dock_labels` control the grid and dock. Current order:
爱壹帆 (`tv.ifvod.classic`), Bilibili (`com.xiaodianshi.tv.yst`),
YouTube (`com.google.android.youtube.tv`), Netflix (`com.netflix.ninja`).
Downloader stays in the remaining app grid. A missing dock app gets a placeholder
instead of silently moving another app into its slot.

The first build uses black backgrounds and the Android system thin/light font.
A downloaded Outfit font, custom wallpaper controls, visible Wi-Fi SSID, a unified
dock band, and custom glass input popup are not implemented in this first version.
The input list depends on inputs exposed by TCL's firmware. App listings refresh
when the launcher activity is recreated; last focused app is saved between launches.
No screenshot or physical-remote test has yet established layout correctness.

For a source-level wallpaper change, replace `stage.setBackgroundColor(Color.BLACK)`
in `HomeActivity.java` with a local drawable background, then rebuild/install.
There is no wallpaper picker yet. This should be refined after the baseline screenshot.

## Recovery

No package disables or Home changes are made by the included build/install scripts.
`restore-stock-home.sh` re-enables and opens Google TV if a later handover needs undoing:

```sh
bash restore-stock-home.sh 192.168.1.232:34643
```

Verify with the physical Home button: a `set-home-activity` success message alone
does not prove the firmware honored it. Never disable setupwraith without first
checking saved Wi-Fi network ownership and preparing recovery.

The only debloat change confirmed so far is TCL Logkit. Its original state was
default; restore it with a separate shell call:

```sh
adb -s 192.168.1.232:34643 shell pm default-state --user 0 com.tcl.logkit
```

For another package intentionally disabled later, `pm enable --user 0 PACKAGE`
re-enables it; consult the saved pre-change state for exact restoration. Do not
apply a blanket enable to packages that were already disabled before this session.

Do not disable streaming/casting/voice/update components, TV inputs, keyboard,
Bluetooth/remote services, Play Store, or Play Services. Factory activities must
never be launched just to inspect them.

Private DNS is unchanged. Optional ad-blocking DNS needs a separate preference
decision and playback test; it does not block YouTube ads. If enabled later, the
usual automatic-DNS undo is `settings put global private_dns_mode opportunistic`;
preserve original settings for exact restoration.

When the entire session is complete, turn Wireless debugging off and forget the
paired Mac on the TV. Verify ADB cannot reconnect at the current connection port.
USB/Network debugging should remain off; port 5555 has not been enabled here.
Processes and network sessions are not part of this source bundle. Use existing
checkouts in isolated cloud tasks; do not create a Git worktree unless requested.
