package com.my.netindicator;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DataAnalyticsActivity extends Activity {
    private LanguageManager langManager;
    private NetworkLogger logger;
    private TextView tvResult;
    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        logger = new NetworkLogger(this);
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
        title.setText("📊 " + langManager.get("data_analytics"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        main.addView(title);

        Button dateBtn = new Button(this);
        dateBtn.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()));
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateBtn.setOnClickListener(v -> showDatePicker(dateBtn));
        main.addView(dateBtn);

        String[] timeframes = {"1 minute", "5 minutes", "15 minutes", "30 minutes", "1 hour", "3 hours", "1 day", "1 week", "1 month"};
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeframes);
        spinner.setAdapter(adapter);
        main.addView(spinner);

        Button analyzeBtn = new Button(this);
        analyzeBtn.setText("🔍 Analyze");
        analyzeBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        analyzeBtn.setTextColor(Color.WHITE);
        analyzeBtn.setOnClickListener(v -> performAnalysis());
        main.addView(analyzeBtn);

        tvResult = new TextView(this);
        tvResult.setTextColor(Color.parseColor("#CCCCCC"));
        tvResult.setTextSize(13);
        tvResult.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvResult.setText(langManager.get("no_data"));
        main.addView(tvResult);

        Button backBtn = new Button(this);
        backBtn.setText("← Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        scroll.addView(main);
        setContentView(scroll);
    }

    private void showDatePicker(Button dateBtn) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime());
            dateBtn.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selected.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void performAnalysis() {
        try {
            JSONArray logs = logger.getLogs();
            if (logs.length() == 0) {
                tvResult.setText(langManager.get("no_data"));
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Date: ").append(selectedDate).append("\n");
            sb.append("Total Records: ").append(logs.length()).append("\n");
            
            for (int i = 0; i < logs.length(); i++) {
                JSONObject log = logs.getJSONObject(i);
                sb.append(log.getString("time")).append(" ")
                  .append(log.getString("grade")).append("\n");
            }
            
            tvResult.setText(sb.toString());
        } catch (Exception e) {
            tvResult.setText("Error: " + e.getMessage());
        }
    }
}
