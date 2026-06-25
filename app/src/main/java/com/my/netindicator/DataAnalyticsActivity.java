package com.my.netindicator;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
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
import java.util.concurrent.TimeUnit;

public class DataAnalyticsActivity extends Activity {
    private LanguageManager langManager;
    private NetworkLogger logger;
    private TextView tvResult;
    private String selectedDate = "";
    private int selectedMinutes = 30;

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
        main.setPadding(40, 60, 40, 40);
        scroll.addView(main);

        // Title with back arrow
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, 0, 0, 20);

        TextView backArrow = new TextView(this);
        backArrow.setText("←");
        backArrow.setTextColor(Color.WHITE);
        backArrow.setTextSize(28);
        backArrow.setPadding(0, 0, 20, 0);
        backArrow.setOnClickListener(v -> finish());
        titleRow.addView(backArrow);

        TextView title = new TextView(this);
        title.setText("📊 " + langManager.get("data_analytics"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleRow.addView(title);

        main.addView(titleRow);

        // Date picker
        Button dateBtn = new Button(this);
        dateBtn.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()));
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateBtn.setOnClickListener(v -> showDatePicker(dateBtn));
        main.addView(dateBtn);

        // Timeframe spinner with actual minutes
        String[] timeframes = {"1 minute", "5 minutes", "15 minutes", "30 minutes", "1 hour", "3 hours", "6 hours", "12 hours", "1 day"};
        final int[] minutes = {1, 5, 15, 30, 60, 180, 360, 720, 1440};
        
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeframes);
        spinner.setAdapter(adapter);
        spinner.setSelection(3); // default 30 minutes
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedMinutes = minutes[pos];
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        main.addView(spinner);

        // Analyze button
        Button analyzeBtn = new Button(this);
        analyzeBtn.setText("🔍 Analyze");
        analyzeBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        analyzeBtn.setTextColor(Color.WHITE);
        analyzeBtn.setOnClickListener(v -> performAnalysis());
        main.addView(analyzeBtn);

        // Results
        tvResult = new TextView(this);
        tvResult.setTextColor(Color.parseColor("#CCCCCC"));
        tvResult.setTextSize(13);
        tvResult.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvResult.setText("Select timeframe and click Analyze");
        tvResult.setPadding(0, 20, 0, 20);
        main.addView(tvResult);

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
            JSONArray allLogs = logger.getLogs();
            if (allLogs.length() == 0) {
                tvResult.setText(langManager.get("no_data"));
                return;
            }

            // Filter logs by selected timeframe (last N minutes from now)
            long cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(selectedMinutes);
            StringBuilder sb = new StringBuilder();
            
            sb.append("Date: ").append(selectedDate).append("\n");
            sb.append("Last ").append(selectedMinutes).append(" minutes\n");
            sb.append("Total Records: ").append(allLogs.length()).append("\n");
            sb.append("Filtered: ");
            
            int filteredCount = 0;
            long totalPing = 0;
            long validPingCount = 0;
            String lastGrade = "";
            long lastPing = -1;

            // Show last 30 filtered records (most recent first)
            for (int i = allLogs.length() - 1; i >= 0; i--) {
                JSONObject log = allLogs.getJSONObject(i);
                String timeStr = log.getString("time");
                
                // Parse log time (format: HH:mm:ss or yyyy-MM-dd HH:mm:ss)
                long logTime = parseLogTime(timeStr);
                
                // Only include if within selected timeframe
                if (logTime >= cutoffTime) {
                    if (filteredCount < 30) { // Show max 30 recent
                        String grade = log.getString("grade");
                        long ping = log.getLong("ping");
                        
                        sb.append("\n").append(timeStr.substring(0, Math.min(8, timeStr.length()))).append(" ")
                          .append(String.format("%-6s", grade)).append(" ");
                        
                        if (ping >= 0) {
                            sb.append(ping).append("ms");
                            totalPing += ping;
                            validPingCount++;
                        } else {
                            sb.append("timeout");
                        }
                        
                        lastGrade = grade;
                        lastPing = ping;
                    }
                    filteredCount++;
                }
            }
            
            if (filteredCount == 0) {
                sb.append("\nNo data in last ").append(selectedMinutes).append(" minutes");
            } else {
                sb.append("\n\n--- Summary ---\n");
                sb.append("Records: ").append(filteredCount).append("\n");
                if (validPingCount > 0) {
                    sb.append("Avg Ping: ").append(totalPing / validPingCount).append("ms\n");
                }
                sb.append("Latest: ").append(lastGrade);
                if (lastPing >= 0) sb.append(" (").append(lastPing).append("ms)");
            }

            tvResult.setText(sb.toString());
        } catch (Exception e) {
            tvResult.setText("Error: " + e.getMessage());
        }
    }

    private long parseLogTime(String timeStr) {
        try {
            // Try full format first
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(timeStr);
            if (date != null) return date.getTime();
            
            // Try time only (today)
            SimpleDateFormat timeOnly = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date time = timeOnly.parse(timeStr);
            if (time != null) {
                Calendar today = Calendar.getInstance();
                Calendar logCal = Calendar.getInstance();
                logCal.setTime(time);
                today.set(Calendar.HOUR_OF_DAY, logCal.get(Calendar.HOUR_OF_DAY));
                today.set(Calendar.MINUTE, logCal.get(Calendar.MINUTE));
                today.set(Calendar.SECOND, logCal.get(Calendar.SECOND));
                return today.getTimeInMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis(); // fallback
    }
}
