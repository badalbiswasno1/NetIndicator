package com.my.netindicator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;

public class BackupRestoreActivity extends Activity {
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
        title.setText("Backup & Restore");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        main.addView(title);

        TextView info = new TextView(this);
        info.setText("Backup your network history and floating window settings to a file, or restore from a previous backup.");
        info.setTextColor(Color.parseColor("#AAAAAA"));
        info.setTextSize(14);
        info.setPadding(0, 0, 0, 20);
        main.addView(info);

        Button backupBtn = new Button(this);
        backupBtn.setText("Create Backup");
        backupBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        backupBtn.setTextColor(Color.WHITE);
        backupBtn.setOnClickListener(v -> createBackup());
        main.addView(backupBtn);

        Button restoreBtn = new Button(this);
        restoreBtn.setText("Restore Backup");
        restoreBtn.setBackgroundColor(Color.parseColor("#0099FF"));
        restoreBtn.setTextColor(Color.WHITE);
        restoreBtn.setOnClickListener(v -> restoreBackup());
        main.addView(restoreBtn);

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }

    private void createBackup() {
        try {
            JSONObject backup = new JSONObject();
            backup.put("logs", logger.getLogs());

            SharedPreferences fp = getSharedPreferences("floating_prefs", MODE_PRIVATE);
            JSONObject floatingPrefs = new JSONObject();
            for (java.util.Map.Entry<String, ?> entry : fp.getAll().entrySet()) {
                floatingPrefs.put(entry.getKey(), entry.getValue());
            }
            backup.put("floating_prefs", floatingPrefs);
            backup.put("backup_version", 1);
            backup.put("backup_time", System.currentTimeMillis());

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "TrueNetwork_backup.json");
            FileWriter fw = new FileWriter(file);
            fw.write(backup.toString(2));
            fw.close();

            Toast.makeText(this, "Backup saved to Downloads/TrueNetwork_backup.json", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void restoreBackup() {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "TrueNetwork_backup.json");
            if (!file.exists()) {
                Toast.makeText(this, "No backup file found in Downloads", Toast.LENGTH_LONG).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject backup = new JSONObject(sb.toString());

            if (backup.has("floating_prefs")) {
                JSONObject fpJson = backup.getJSONObject("floating_prefs");
                SharedPreferences.Editor editor = getSharedPreferences("floating_prefs", MODE_PRIVATE).edit();
                java.util.Iterator<String> keys = fpJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = fpJson.get(key);
                    if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                    else if (val instanceof Integer) editor.putInt(key, (Integer) val);
                    else if (val instanceof Long) editor.putInt(key, ((Long) val).intValue());
                    else if (val instanceof Double) editor.putFloat(key, ((Double) val).floatValue());
                    else if (val instanceof String) editor.putString(key, (String) val);
                }
                editor.apply();
            }

            if (backup.has("logs")) {
                SharedPreferences.Editor logsEditor = getSharedPreferences("NetworkLogs", MODE_PRIVATE).edit();
                logsEditor.putString("logs", backup.getJSONArray("logs").toString());
                logsEditor.apply();
            }

            Toast.makeText(this, "Backup restored successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
