package dev.cleanmytv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Separate setup screen; launcher appearance and app ordering are unchanged. */
public final class HomeHelperSettingsActivity extends Activity {
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("CleanMyTV Home helper");
        title.setTextSize(24);
        content.addView(title);
        TextView explanation = new TextView(this);
        explanation.setText("Optional Home shortcut. Keep Google TV and SetupWraith enabled. "
            + "The TV must deliver Home events to accessibility services. "
            + "Home holds and double presses are not separately mapped. "
            + "Test held-volume repeats, Input, Settings and app buttons after enabling. "
            + "If any remote behaviour changes, turn this helper off in Android Accessibility settings. "
            + "Startup launching is not part of this test.");
        content.addView(explanation);
        status = new TextView(this);
        status.setPadding(0, padding / 2, 0, padding / 2);
        content.addView(status);
        Button enable = button(content, "Enable Home helper", () -> {
            getSharedPreferences(HomeKeyService.PREFS, MODE_PRIVATE).edit()
                .putBoolean(HomeKeyService.ARMED, true).apply();
            openAccessibility();
            updateStatus();
        });
        button(content, "Turn off Home helper", () -> {
            HomeKeyService.turnOff(this);
            status.postDelayed(this::updateStatus, 300);
        });
        button(content, "Android accessibility settings", this::openAccessibility);
        button(content, "Open CleanMyTV", () -> startActivity(new Intent(this, HomeActivity.class)));
        enable.requestFocus();
        if (getIntent().getBooleanExtra("disable_home_helper", false)) {
            HomeKeyService.turnOff(this);
            updateStatus();
        }
    }

    private Button button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> action.run());
        parent.addView(button);
        return button;
    }

    private void openAccessibility() {
        try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        catch (RuntimeException unavailable) {
            Toast.makeText(this, "Open TV Settings > Accessibility manually", Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus() {
        boolean armed = getSharedPreferences(HomeKeyService.PREFS, MODE_PRIVATE)
            .getBoolean(HomeKeyService.ARMED, false);
        status.setText(!armed ? "Helper is off."
            : HomeKeyService.isConnected() ? "Helper is connected. Test the physical Home button."
            : "Not connected. Enable CleanMyTV Home helper in Android Accessibility settings.");
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
