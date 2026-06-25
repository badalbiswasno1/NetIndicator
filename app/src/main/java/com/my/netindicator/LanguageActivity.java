package com.my.netindicator;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class LanguageActivity extends Activity {
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
        title.setText("🌐 " + langManager.get("language"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 30);
        main.addView(title);

        String[] names = langManager.getLanguageNames();
        String[] codes = langManager.getLanguageCodes();
        String current = langManager.getCurrentLanguage();

        for (int i = 0; i < names.length; i++) {
            final String code = codes[i];
            Button btn = new Button(this);
            btn.setText(names[i] + (code.equals(current) ? "  ✓" : ""));
            btn.setBackgroundColor(code.equals(current) ?
                Color.parseColor("#00CC44") : Color.parseColor("#222222"));
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(15);
            btn.setOnClickListener(v -> {
                langManager.setLanguage(code);
                Toast.makeText(this, "Language changed", Toast.LENGTH_SHORT).show();
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
}
