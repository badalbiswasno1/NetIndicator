package com.my.netindicator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private LanguageManager langManager;
    private FloatingWindowPrefs windowPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        windowPrefs = new FloatingWindowPrefs(this);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.parseColor("#111111"));
        main.setPadding(40, 80, 40, 40);

        TextView title = new TextView(this);
        title.setText("⚙ " + langManager.get("settings"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        main.addView(title);

        TextView langTitle = new TextView(this);
        langTitle.setText(langManager.get("language"));
        langTitle.setTextColor(Color.parseColor("#FFD700"));
        langTitle.setTextSize(16);
        main.addView(langTitle);

        String[] names = langManager.getLanguageNames();
        String[] codes = langManager.getLanguageCodes();
        
        for (int i = 0; i < names.length; i++) {
            final String code = codes[i];
            Button langBtn = new Button(this);
            langBtn.setText(names[i]);
            langBtn.setBackgroundColor(Color.parseColor("#333333"));
            langBtn.setTextColor(Color.WHITE);
            langBtn.setOnClickListener(v -> {
                langManager.setLanguage(code);
                Toast.makeText(this, "Language changed", Toast.LENGTH_SHORT).show();
                recreate();
            });
            main.addView(langBtn);
        }

        Button backBtn = new Button(this);
        backBtn.setText("← Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        scroll.addView(main);
        setContentView(scroll);
    }
}
