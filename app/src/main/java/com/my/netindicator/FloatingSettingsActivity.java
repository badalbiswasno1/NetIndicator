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
import android.widget.TextView;
import android.widget.Toast;

public class FloatingSettingsActivity extends Activity {
    private FloatingWindowPrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new FloatingWindowPrefs(this);
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
        title.setText("📱 Floating Window");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 30);
        main.addView(title);

        // Toggle
        Button toggleBtn = new Button(this);
        toggleBtn.setText(prefs.isVisible() ? "Floating: ON" : "Floating: OFF");
        toggleBtn.setBackgroundColor(prefs.isVisible() ?
            Color.parseColor("#00CC44") : Color.parseColor("#E63329"));
        toggleBtn.setTextColor(Color.WHITE);
        toggleBtn.setOnClickListener(v -> {
            boolean newVal = !prefs.isVisible();
            prefs.setVisible(newVal);
            toggleBtn.setText(newVal ? "Floating: ON" : "Floating: OFF");
            toggleBtn.setBackgroundColor(newVal ?
                Color.parseColor("#00CC44") : Color.parseColor("#E63329"));
        });
        main.addView(toggleBtn);

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
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, positions);
        posSpinner.setAdapter(posAdapter);
        int currentGravity = prefs.getGravity();
        for (int i = 0; i < gravities.length; i++) {
            if (gravities[i] == currentGravity) posSpinner.setSelection(i);
        }
        posSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setGravity(gravities[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        main.addView(posSpinner);

        // Text Size
        final TextView sizeLabel = addLabel(main, "Text Size: " + (int)prefs.getSize());
        SeekBar sizeSeek = new SeekBar(this);
        sizeSeek.setMax(40);
        sizeSeek.setProgress((int)prefs.getSize() - 10);
        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                prefs.setSize(p + 10);
                sizeLabel.setText("Text Size: " + (p + 10));
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        main.addView(sizeSeek);

        // Transparency
        final TextView transLabel = addLabel(main, "Transparency: " + prefs.getTransparency() + "%");
        SeekBar transSeek = new SeekBar(this);
        transSeek.setMax(100);
        transSeek.setProgress(prefs.getTransparency());
        transSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                prefs.setTransparency(p);
                transLabel.setText("Transparency: " + p + "%");
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        main.addView(transSeek);

        // Text Color
        addLabel(main, "Text Color");
        String[] textColors = {"Green", "White", "Yellow", "Cyan"};
        final int[] textColorValues = {
            Color.parseColor("#00CC44"), Color.WHITE,
            Color.parseColor("#FFD700"), Color.CYAN
        };
        Spinner tcSpinner = new Spinner(this);
        ArrayAdapter<String> tcAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, textColors);
        tcSpinner.setAdapter(tcAdapter);
        tcSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setTextColor(textColorValues[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        main.addView(tcSpinner);

        // Background Color
        addLabel(main, "Background Color");
        String[] bgColors = {"Black", "Dark Gray", "Blue", "Transparent"};
        final int[] bgColorValues = {
            Color.BLACK, Color.parseColor("#333333"),
            Color.parseColor("#003366"), Color.TRANSPARENT
        };
        Spinner bgSpinner = new Spinner(this);
        ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, bgColors);
        bgSpinner.setAdapter(bgAdapter);
        bgSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setBackgroundColor(bgColorValues[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        main.addView(bgSpinner);

        Button backBtn = new Button(this);
        backBtn.setText("← Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }

    private TextView addLabel(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#AAAAAA"));
        tv.setTextSize(13);
        tv.setPadding(0, 15, 0, 5);
        parent.addView(tv);
        return tv;
    }
}
