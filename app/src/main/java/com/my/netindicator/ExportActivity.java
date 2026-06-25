package com.my.netindicator;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class ExportActivity extends Activity {
    private NetworkLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger = new NetworkLogger(this);
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
        title.setText("Export Data");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        main.addView(title);

        TextView info = new TextView(this);
        info.setText("Export your network history as CSV file to Downloads folder.");
        info.setTextColor(Color.parseColor("#AAAAAA"));
        info.setTextSize(14);
        info.setPadding(0, 0, 0, 20);
        main.addView(info);

        Button exportBtn = new Button(this);
        exportBtn.setText("Export as CSV");
        exportBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        exportBtn.setTextColor(Color.WHITE);
        exportBtn.setOnClickListener(v -> exportCSV());
        main.addView(exportBtn);

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }

    private void exportCSV() {
        try {
            JSONArray logs = logger.getLogs();
            StringBuilder sb = new StringBuilder();
            sb.append("Date,Time,Grade,Ping(ms),Data(KB)
");
            for (int i = 0; i < logs.length(); i++) {
                JSONObject obj = logs.getJSONObject(i);
                sb.append(obj.optString("date", "")).append(",")
                  .append(obj.optString("time", "")).append(",")
                  .append(obj.optString("grade", "")).append(",")
                  .append(obj.optLong("ping", 0)).append(",")
                  .append(obj.optLong("data", 0)).append("
");
            }

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "TrueNetwork_export.csv");
            FileWriter fw = new FileWriter(file);
            fw.write(sb.toString());
            fw.close();

            Toast.makeText(this, "Exported to Downloads/TrueNetwork_export.csv", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
