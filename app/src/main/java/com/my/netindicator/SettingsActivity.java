package com.my.netindicator;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private LanguageManager langManager;
    private FloatingWindowPrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        prefs = new FloatingWindowPrefs(this);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.parseColor("#111111"));
        main.setPadding(40, 80, 40, 40);
        scroll.addView(main);

        // Title
        TextView title = new TextView(this);
        title.setText("⚙ " + langManager.get("settings"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        main.addView(title);

        // ===== LANGUAGE SECTION =====
        addSectionTitle(main, "🌐 " + langManager.get("language"));

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

        // ===== FLOATING WINDOW SECTION =====
        addSectionTitle(main, "📱 Floating Window");

        // Toggle
        Switch floatSwitch = new Switch(this);
        floatSwitch.setText("Show Floating Window");
        floatSwitch.setTextColor(Color.WHITE);
        floatSwitch.setChecked(prefs.isVisible());
        floatSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.setVisible(checked);
            Toast.makeText(this, checked ? "Floating ON" : "Floating OFF", Toast.LENGTH_SHORT).show();
        });
        main.addView(floatSwitch);

        // Position
        addLabel(main, "Position");
        String[] positions = {"Top Right", "Top Left", "Bottom Right", "Bottom Left"};
        final int[] gravities = {
            Gravity.TOP | Gravity.END,
            Gravity.TOP | Gravity.START,
            Gravity.BOTTOM | Gravity.END,
            Gravity.BOTTOM | Gravity.START
        };
        Spinner posSpinner = new Spinner(this);
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, positions);
        posSpinner.setAdapter(posAdapter);
        int currentGravity = prefs.getGravity();
        int currentPos = 0;
        for (int i = 0; i < gravities.length; i++) {
            if (gravities[i] == currentGravity) currentPos = i;
        }
        posSpinner.setSelection(currentPos);
        posSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                prefs.setGravity(gravities[pos]);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        main.addView(posSpinner);

        // Text Size
        addLabel(main, "Text Size: " + (int)prefs.getSize());
        SeekBar sizeSeek = new SeekBar(this);
        sizeSeek.setMax(40);
        sizeSeek.setProgress((int)prefs.getSize() - 10);
        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                float newSize = p + 10;
                prefs.setSize(newSize);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        main.addView(sizeSeek);

        // Transparency
        addLabel(main, "Transparency: " + prefs.getTransparency() + "%");
        SeekBar transSeek = new SeekBar(this);
        transSeek.setMax(100);
        transSeek.setProgress(prefs.getTransparency());
        transSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                prefs.setTransparency(p);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        main.addView(transSeek);

        // Text Color
        addLabel(main, "Text Color");
        String[] textColors = {"Green", "White", "Yellow", "Cyan", "Red", "Lime"};
        final int[] textColorValues = {
            Color.parseColor("#00CC44"),
            Color.WHITE,
            Color.parseColor("#FFD700"),
            Color.CYAN,
            Color.parseColor("#E63329"),
            Color.parseColor("#00FF00")
        };
        Spinner textColorSpinner = new Spinner(this);
        ArrayAdapter<String> tcAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, textColors);
        textColorSpinner.setAdapter(tcAdapter);
        textColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                prefs.setTextColor(textColorValues[pos]);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        main.addView(textColorSpinner);

        // Background Color
        addLabel(main, "Background Color");
        String[] bgColors = {"Black", "Dark Gray", "Blue", "Purple", "Red", "Transparent"};
        final int[] bgColorValues = {
            Color.BLACK,
            Color.parseColor("#333333"),
            Color.parseColor("#003366"),
            Color.parseColor("#440044"),
            Color.parseColor("#330000"),
            Color.TRANSPARENT
        };
        Spinner bgSpinner = new Spinner(this);
        ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bgColors);
        bgSpinner.setAdapter(bgAdapter);
        bgSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                prefs.setBackgroundColor(bgColorValues[pos]);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        main.addView(bgSpinner);

        // Refresh Interval
        addLabel(main, "Refresh: " + (prefs.getRefreshInterval() / 1000) + " sec");
        SeekBar refreshSeek = new SeekBar(this);
        refreshSeek.setMax(9);
        refreshSeek.setProgress((prefs.getRefreshInterval() / 1000) - 1);
        refreshSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                prefs.setRefreshInterval((p + 1) * 1000);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        main.addView(refreshSeek);

        // ===== ABOUT SECTION =====
        addSectionTitle(main, "ℹ About");

        TextView versionText = new TextView(this);
        versionText.setText("True Network v2.0\nExact Grade: 0.1 step precision\nDeveloped by: Badal Biswas");
        versionText.setTextColor(Color.parseColor("#AAAAAA"));
        versionText.setTextSize(13);
        versionText.setGravity(Gravity.CENTER);
        versionText.setPadding(0, 10, 0, 30);
        main.addView(versionText);

        // Back Button
        Button backBtn = new Button(this);
        backBtn.setText("← Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }

    private void addSectionTitle(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#FFD700"));
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 30, 0, 15);
        parent.addView(tv);
    }

    private void addLabel(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#AAAAAA"));
        tv.setTextSize(13);
        tv.setPadding(0, 10, 0, 5);
        parent.addView(tv);
    }
}
