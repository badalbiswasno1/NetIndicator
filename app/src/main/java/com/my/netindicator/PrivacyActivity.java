package com.my.netindicator;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class PrivacyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        title.setText("Privacy");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        main.addView(title);

        TextView info = new TextView(this);
        info.setText(
            "True Network stores all network history and settings locally on your device only.\n\n" +
            "No data is sent to any server except for standard ping and DNS lookup tests used to measure your network quality " +
            "(these only send small test packets, no personal data).\n\n" +
            "Location permission is used solely to read cellular signal information as required by Android, and is never shared."
        );
        info.setTextColor(Color.parseColor("#AAAAAA"));
        info.setTextSize(14);
        info.setPadding(0, 0, 0, 30);
        main.addView(info);

        Button clearBtn = new Button(this);
        clearBtn.setText("Clear All App Data");
        clearBtn.setBackgroundColor(Color.parseColor("#E63329"));
        clearBtn.setTextColor(Color.WHITE);
        clearBtn.setOnClickListener(v -> clearAllData());
        main.addView(clearBtn);

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = 20;
        main.addView(backBtn, p);

        setContentView(scroll);
    }

    private void clearAllData() {
        try {
            getSharedPreferences("NetworkLogs", MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("floating_prefs", MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("AppSettings", MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("widget_prefs", MODE_PRIVATE).edit().clear().apply();
            Toast.makeText(this, "All app data cleared", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to clear data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
