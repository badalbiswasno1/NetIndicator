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

public class HistoryLimitActivity extends Activity {
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
        title.setText("History Limit");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        main.addView(title);

        int current = prefs.getInt("history_limit", 2000);
        TextView currentLabel = new TextView(this);
        currentLabel.setText("Current: " + current + " records");
        currentLabel.setTextColor(Color.parseColor("#AAAAAA"));
        currentLabel.setTextSize(14);
        currentLabel.setPadding(0, 0, 0, 20);
        main.addView(currentLabel);

        int[] limits = {500, 1000, 2000, 5000, 10000};
        for (int limit : limits) {
            Button btn = new Button(this);
            btn.setText(limit + " records" + (limit == current ? "  ✓" : ""));
            btn.setBackgroundColor(limit == current ? Color.parseColor("#00CC44") : Color.parseColor("#222222"));
            btn.setTextColor(Color.WHITE);
            final int val = limit;
            btn.setOnClickListener(v -> {
                prefs.edit().putInt("history_limit", val).apply();
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
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
