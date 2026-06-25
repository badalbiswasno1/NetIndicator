package com.my.netindicator;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NetworkLogger {
    private static final String PREFS_NAME = "NetworkLogs";
    private static final String KEY_LOGS = "logs";
    private static final int MAX_LOGS = 2000;
    private SharedPreferences prefs;

    public NetworkLogger(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void log(String grade, long ping, long dataKB) {
        try {
            JSONArray logs = getLogs();
            JSONObject entry = new JSONObject();
            entry.put("timestamp", System.currentTimeMillis());
            entry.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            entry.put("time", new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            entry.put("grade", grade);
            entry.put("network", grade.substring(0, grade.indexOf('.')));
            entry.put("ping", ping);
            entry.put("data", dataKB);

            logs.put(entry);

            if (logs.length() > MAX_LOGS) {
                JSONArray trimmed = new JSONArray();
                for (int i = logs.length() - MAX_LOGS; i < logs.length(); i++) {
                    trimmed.put(logs.get(i));
                }
                logs = trimmed;
            }

            prefs.edit().putString(KEY_LOGS, logs.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONArray getLogs() {
        try {
            String data = prefs.getString(KEY_LOGS, "[]");
            return new JSONArray(data);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public void clear() {
        prefs.edit().remove(KEY_LOGS).apply();
    }
}
