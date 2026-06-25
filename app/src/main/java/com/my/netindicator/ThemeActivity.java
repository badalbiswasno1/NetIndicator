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
        String current = prefs.getString("theme", "dark");
        int bgColor = getThemeBg(current);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor);
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(40, 60, 40, 40);
        main.setBackgroundColor(bgColor);
        scroll.addView(main);

        TextView title = new TextView(this);
        title.setText("Theme");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        main.addView(title);

        String[] themes = {"dark", "black", "amoled", "navy", "forest", "midnight"};
        String[] labels = {
            "Dark Gray (#111111)",
            "Black (#000000)",
            "AMOLED Pure Black",
            "Navy Blue (#001133)",
            "Forest Green (#001A00)",
            "Midnight Purple (#0D0019)"
        };

        for (int i = 0; i < themes.length; i++) {
            final String val = themes[i];
            Button btn = new Button(this);
            btn.setText(labels[i] + (val.equals(current) ? "  checkmark" : ""));
            btn.setBackgroundColor(getThemeBg(val));
            btn.setTextColor(Color.WHITE);
            btn.setPadding(20, 20, 20, 20);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 5, 0, 5);
            btn.setLayoutParams(p);
            btn.setOnClickListener(v -> {
                prefs.edit().putString("theme", val).apply();
                Toast.makeText(this, "Theme changed! Restart app.", Toast.LENGTH_SHORT).show();
                recreate();
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

    public static int getThemeBg(String theme) {
        switch (theme) {
            case "black": return Color.parseColor("#000000");
            case "amoled": return Color.parseColor("#000000");
            case "navy": return Color.parseColor("#001133");
            case "forest": return Color.parseColor("#001A00");
            case "midnight": return Color.parseColor("#0D0019");
            default: return Color.parseColor("#111111");
        }
    }
}
