package dev.cleanmytv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Explicit opt-in, no boot receiver, no window inspection, no system package changes. */
public final class HomeKeyService extends AccessibilityService {
    static final String PREFS = "home_helper";
    static final String ARMED = "armed";
    private static HomeKeyService connected;
    private final HomeButtonPress presses = new HomeButtonPress();

    static boolean isConnected() { return connected != null; }

    static void turnOff(Context context) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(ARMED, false).apply();
        if (connected != null) connected.disableSelf();
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        connected = this;
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(ARMED, false)) disableSelf();
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
                startActivity(new Intent(this, HomeActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP));
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
        if (connected == this) connected = null;
        presses.reset();
        super.onDestroy();
    }
}
