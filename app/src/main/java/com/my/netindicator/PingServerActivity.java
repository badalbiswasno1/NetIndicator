package com.my.netindicator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class PingServerActivity extends Activity {
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
        title.setText("Ping Server");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 30);
        main.addView(title);

        TextView label = new TextView(this);
        label.setText("Current server: " + prefs.getString("ping_server", "8.8.8.8"));
        label.setTextColor(Color.parseColor("#AAAAAA"));
        label.setTextSize(14);
        main.addView(label);

        EditText input = new EditText(this);
        input.setHint("Enter server IP or domain");
        input.setText(prefs.getString("ping_server", "8.8.8.8"));
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setBackgroundColor(Color.parseColor("#222222"));
        input.setPadding(20, 20, 20, 20);
        main.addView(input);

        String[] presets = {"8.8.8.8 (Google)", "1.1.1.1 (Cloudflare)", "208.67.222.222 (OpenDNS)"};
        String[] presetValues = {"8.8.8.8", "1.1.1.1", "208.67.222.222"};
        for (int i = 0; i < presets.length; i++) {
            final String val = presetValues[i];
            Button btn = new Button(this);
            btn.setText(presets[i]);
            btn.setBackgroundColor(Color.parseColor("#222222"));
            btn.setTextColor(Color.WHITE);
            btn.setOnClickListener(v -> input.setText(val));
            main.addView(btn);
        }

        Button saveBtn = new Button(this);
        saveBtn.setText("Save");
        saveBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setOnClickListener(v -> {
            prefs.edit().putString("ping_server", input.getText().toString().trim()).apply();
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
        main.addView(saveBtn);

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }
}
