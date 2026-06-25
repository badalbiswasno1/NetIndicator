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

        // Title
        TextView title = new TextView(this);
        title.setText("⚙ " + langManager.get("settings"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 40);
        main.addView(title);

        // Language folder button
        Button langBtn = new Button(this);
        langBtn.setText("🌐  " + langManager.get("language") + "  ›");
        langBtn.setBackgroundColor(Color.parseColor("#222222"));
        langBtn.setTextColor(Color.WHITE);
        langBtn.setTextSize(16);
        langBtn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        langBtn.setPadding(30, 20, 30, 20);
        langBtn.setOnClickListener(v -> startActivity(new Intent(this, LanguageActivity.class)));
        main.addView(langBtn);

        // Divider
        addDivider(main);

        // Floating Window folder button
        Button floatBtn = new Button(this);
        floatBtn.setText("📱  Floating Window  ›");
        floatBtn.setBackgroundColor(Color.parseColor("#222222"));
        floatBtn.setTextColor(Color.WHITE);
        floatBtn.setTextSize(16);
        floatBtn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        floatBtn.setPadding(30, 20, 30, 20);
        floatBtn.setOnClickListener(v -> startActivity(new Intent(this, FloatingSettingsActivity.class)));
        main.addView(floatBtn);

        addDivider(main);

        // About
        TextView about = new TextView(this);
        about.setText("True Network v3.0\nDeveloped by: Badal Biswas");
        about.setTextColor(Color.parseColor("#AAAAAA"));
        about.setTextSize(13);
        about.setGravity(Gravity.CENTER);
        about.setPadding(0, 40, 0, 0);
        main.addView(about);

        // Back button
        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }

    private void addDivider(LinearLayout parent) {
        TextView divider = new TextView(this);
        divider.setBackgroundColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(0, 5, 0, 5);
        divider.setLayoutParams(params);
        parent.addView(divider);
    }
}
