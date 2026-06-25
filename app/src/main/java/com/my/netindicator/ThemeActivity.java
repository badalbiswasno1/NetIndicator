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

public class ThemeActivity extends Activity {
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
        title.setText("Theme");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        main.addView(title);

        String current = prefs.getString("theme", "dark");
        String[] themes = {"dark", "black", "amoled"};
        String[] labels = {"Dark (#111111)", "Black (#000000)", "AMOLED (Pure Black)"};
        for (int i = 0; i < themes.length; i++) {
            final String val = themes[i];
            Button btn = new Button(this);
            btn.setText(labels[i] + (val.equals(current) ? "  ✓" : ""));
            btn.setBackgroundColor(val.equals(current) ? Color.parseColor("#00CC44") : Color.parseColor("#222222"));
            btn.setTextColor(Color.WHITE);
            btn.setOnClickListener(v -> {
                prefs.edit().putString("theme", val).apply();
                Toast.makeText(this, "Theme changed! Restart app.", Toast.LENGTH_SHORT).show();
                finish();
            });
            main.addView(btn);
        }

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }
}
