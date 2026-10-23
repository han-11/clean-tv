package dev.cleanmytv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Explicit opt-in, no boot receiver, no window inspection, no system package changes. */
public final class HomeKeyService extends AccessibilityService {
    static final String PREFS = "home_helper";
    static final String ARMED = "armed";
    static final String STARTUP = "startup";
    private static final String HANDLED_BOOT = "handled_boot";
    private final Handler startupHandler = new Handler(Looper.getMainLooper());
    private final Runnable startupAttempt = this::attemptStartup;
    private int startupAttempts;
    private static HomeKeyService connected;
    private final HomeButtonPress presses = new HomeButtonPress();

    static boolean isConnected() { return connected != null; }

    static int bootCount(Context context) {
        try { return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1); }
        catch (RuntimeException unavailable) { return -1; }
    }

    static boolean setStartup(Context context, boolean enabled) {
        int boot = bootCount(context);
        if (enabled && boot < 0) return false;
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(STARTUP, enabled).putInt(HANDLED_BOOT, boot).apply();
        if (connected != null) connected.startupHandler.removeCallbacks(connected.startupAttempt);
        return true;
    }

    static void turnOff(Context context) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(ARMED, false).apply();
        if (connected != null) {
            connected.startupHandler.removeCallbacks(connected.startupAttempt);
            connected.disableSelf();
        }
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        connected = this;
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(ARMED, false)) {
            disableSelf();
            return;
        }
        startupAttempts = 0;
        startupHandler.removeCallbacks(startupAttempt);
        startupHandler.postDelayed(startupAttempt, 10000L);
    }

    private Intent homeIntent() {
        return new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    private void markStartupHandled() {
        int boot = bootCount(this);
        if (boot >= 0) getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putInt(HANDLED_BOOT, boot).apply();
        startupHandler.removeCallbacks(startupAttempt);
    }

    private void attemptStartup() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(ARMED, false)
                || !BootLaunchPolicy.eligible(prefs.getBoolean(STARTUP, false), bootCount(this),
                    prefs.getInt(HANDLED_BOOT, -1), SystemClock.elapsedRealtime())
                || startupAttempts++ >= 24) return;
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (power != null && power.isInteractive()
                && (keyguard == null || !keyguard.isKeyguardLocked())) {
            try {
                startActivity(homeIntent());
                markStartupHandled();
                return;
            } catch (RuntimeException failure) {
                Log.w("CleanMyTVHome", "Startup launch rejected; Home helper remains available", failure);
            }
        }
        startupHandler.postDelayed(startupAttempt, 5000L);
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        // Forward volume, Input, Back, app buttons, and repeats without extra work.
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(ARMED, false)) {
            presses.reset();
            return false;
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (power == null || !power.isInteractive()
                || (keyguard != null && keyguard.isKeyguardLocked())) {
            presses.reset();
            return false;
        }
        HomeButtonPress.Result result = presses.handle(event.getKeyCode() == KeyEvent.KEYCODE_HOME,
            event.getAction() == KeyEvent.ACTION_DOWN, event.getAction() == KeyEvent.ACTION_UP,
            event.getRepeatCount(), event.getDownTime(), event.isCanceled());
        if (result == HomeButtonPress.Result.OPEN_HOME) {
            try {
                startActivity(homeIntent());
                markStartupHandled();
            } catch (RuntimeException failure) {
                Log.w("CleanMyTVHome", "Home launch rejected; turning helper off", failure);
                turnOff(this);
            }
        }
        return result != HomeButtonPress.Result.PASS;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { presses.reset(); }
    @Override public void onDestroy() {
        startupHandler.removeCallbacks(startupAttempt);
        if (connected == this) connected = null;
        presses.reset();
        super.onDestroy();
    }
}
