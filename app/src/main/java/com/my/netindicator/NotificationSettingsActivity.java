package com.my.netindicator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class NotificationSettingsActivity extends Activity {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(40, 60, 40, 40);
        main.setBackgroundColor(Color.parseColor("#111111"));
        scroll.addView(main);

        TextView title = new TextView(this);
        title.setText("Notification");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        main.addView(title);

        boolean enabled = prefs.getBoolean("notif_enabled", false);
        Button toggleBtn = new Button(this);
        toggleBtn.setText("Status Bar Grade: " + (enabled ? "ON" : "OFF"));
        toggleBtn.setBackgroundColor(enabled ? Color.parseColor("#00CC44") : Color.parseColor("#E63329"));
        toggleBtn.setTextColor(Color.WHITE);
        toggleBtn.setOnClickListener(v -> {
            boolean newVal = !prefs.getBoolean("notif_enabled", false);
            prefs.edit().putBoolean("notif_enabled", newVal).apply();
            toggleBtn.setText("Status Bar Grade: " + (newVal ? "ON" : "OFF"));
            toggleBtn.setBackgroundColor(newVal ? Color.parseColor("#00CC44") : Color.parseColor("#E63329"));
            Toast.makeText(this, newVal ? "Notification ON" : "Notification OFF", Toast.LENGTH_SHORT).show();
        });
        main.addView(toggleBtn);

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }
}
