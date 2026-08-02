package com.my.netindicator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private LanguageManager langManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.parseColor("#111111"));
        main.setPadding(40, 60, 40, 40);
        scroll.addView(main);

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 30);
        main.addView(title);

        addMenuItem(main, R.drawable.ic_language, langManager.get("language"), LanguageActivity.class);
        addMenuItem(main, R.drawable.ic_phone, "Floating Window", FloatingSettingsActivity.class);
        addMenuItem(main, R.drawable.ic_language, "Ping Server", PingServerActivity.class);
        addMenuItem(main, R.drawable.ic_bar_chart, "History Limit", HistoryLimitActivity.class);
        addMenuItem(main, R.drawable.ic_palette, "Theme", ThemeActivity.class);
        addMenuItem(main, R.drawable.ic_notifications, "Notification", NotificationSettingsActivity.class);
        addMenuItem(main, R.drawable.ic_rocket, "Auto Start", AutoStartActivity.class);
        addMenuItem(main, R.drawable.ic_save, "Export Data", ExportActivity.class);
        addMenuItem(main, R.drawable.ic_backup, "Backup & Restore", BackupRestoreActivity.class);
        addMenuItem(main, R.drawable.ic_info, "About & Feedback", AboutActivity.class);

        TextView about = new TextView(this);
        about.setText("True Network v3.0\nDeveloped by: Badal Biswas");
        about.setTextColor(Color.parseColor("#AAAAAA"));
        about.setTextSize(13);
        about.setGravity(Gravity.CENTER);
        about.setPadding(0, 40, 0, 10);
        main.addView(about);

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }

    private void addMenuItem(LinearLayout parent, int iconRes, String label, Class<?> target) {
        Button btn = new Button(this);
        btn.setText("  " + label + "  >");
        btn.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        btn.setCompoundDrawablePadding(20);
        btn.setBackgroundColor(Color.parseColor("#1A1A1A"));
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(15);
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setPadding(30, 25, 30, 25);
        btn.setOnClickListener(v -> startActivity(new Intent(this, target)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 4, 0, 4);
        btn.setLayoutParams(params);
        parent.addView(btn);
    }
}
