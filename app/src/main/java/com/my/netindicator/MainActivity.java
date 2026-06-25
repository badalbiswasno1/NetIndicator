package com.my.netindicator;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_PHONE_STATE
    };

    private TextView tvGrade, tvPing, tvSignal, tvTime, tvDbm, tvData, tvHistory;
    private Handler handler = new Handler();
    private Runnable updater;
    private String lastNetwork = "";
    private long startTime;
    private NetworkLogger logger;
    private SeekBar timeSeek;
    private TextView tvTimeLabel;
    private int[] timeOptions = {15, 30, 60, 1440};
    private LanguageManager langManager;
    private FloatingWindowPrefs windowPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        windowPrefs = new FloatingWindowPrefs(this);
        startTime = System.currentTimeMillis();
        logger = new NetworkLogger(this);

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }

        buildUI();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            } else {
                if (windowPrefs.isVisible()) {
                    startService(new Intent(this, FloatingService.class));
                }
            }
        }

        updater = new Runnable() {
            public void run() {
                try {
                    updateUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updater);
        updateHistory(0);
    }

    private boolean hasPermissions() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.parseColor("#111111"));
        main.setPadding(40, 80, 40, 40);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(main);

        TextView title = new TextView(this);
        title.setText(langManager.get("true_network"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        main.addView(title);

        tvGrade = new TextView(this);
        tvGrade.setText("...");
        tvGrade.setTextSize(72);
        tvGrade.setTypeface(null, android.graphics.Typeface.BOLD);
        tvGrade.setGravity(Gravity.CENTER);
        tvGrade.setPadding(0, 20, 0, 0);
        main.addView(tvGrade);

        tvPing = new TextView(this);
        tvPing.setText(langManager.get("ping") + ": -- ms");
        tvPing.setTextSize(20);
        tvPing.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPing.setGravity(Gravity.CENTER);
        main.addView(tvPing);

        tvDbm = new TextView(this);
        tvDbm.setText(langManager.get("signal") + ": -- dBm");
        tvDbm.setTextSize(16);
        tvDbm.setGravity(Gravity.CENTER);
        tvDbm.setTextColor(Color.parseColor("#AAAAAA"));
        main.addView(tvDbm);

        tvSignal = new TextView(this);
        tvSignal.setText(langManager.get("operator") + ": --");
        tvSignal.setTextColor(Color.parseColor("#00CC44"));
        tvSignal.setTextSize(16);
        tvSignal.setGravity(Gravity.CENTER);
        main.addView(tvSignal);

        tvTime = new TextView(this);
        tvTime.setText(langManager.get("running") + ": 0 " + langManager.get("seconds"));
        tvTime.setTextColor(Color.parseColor("#AAAAAA"));
        tvTime.setTextSize(13);
        tvTime.setGravity(Gravity.CENTER);
        main.addView(tvTime);

        tvData = new TextView(this);
        tvData.setText("Data: --");
        tvData.setTextColor(Color.parseColor("#FFD700"));
        tvData.setTextSize(14);
        tvData.setGravity(Gravity.CENTER);
        main.addView(tvData);

        tvTimeLabel = new TextView(this);
        tvTimeLabel.setText(langManager.get("time") + ": 15 min");
        tvTimeLabel.setTextColor(Color.parseColor("#AAAAAA"));
        tvTimeLabel.setTextSize(13);
        tvTimeLabel.setGravity(Gravity.CENTER);
        main.addView(tvTimeLabel);

        timeSeek = new SeekBar(this);
        timeSeek.setMax(3);
        timeSeek.setProgress(0);
        timeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                updateHistory(p);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        main.addView(timeSeek);

        tvHistory = new TextView(this);
        tvHistory.setTextColor(Color.parseColor("#CCCCCC"));
        tvHistory.setTextSize(11);
        tvHistory.setTypeface(android.graphics.Typeface.MONOSPACE);
        main.addView(tvHistory);

        Button clearBtn = new Button(this);
        clearBtn.setText(langManager.get("clear_history"));
        clearBtn.setBackgroundColor(Color.parseColor("#E63329"));
        clearBtn.setTextColor(Color.WHITE);
        clearBtn.setOnClickListener(v -> {
            logger.clear();
            tvHistory.setText(langManager.get("no_data"));
        });
        main.addView(clearBtn);

        Button analyticsBtn = new Button(this);
        analyticsBtn.setText("📊 " + langManager.get("data_analytics"));
        analyticsBtn.setBackgroundColor(Color.parseColor("#0099FF"));
        analyticsBtn.setTextColor(Color.WHITE);
        analyticsBtn.setOnClickListener(v -> startActivity(new Intent(this, DataAnalyticsActivity.class)));
        main.addView(analyticsBtn);

        Button settingsBtn = new Button(this);
        settingsBtn.setText("⚙ " + langManager.get("settings"));
        settingsBtn.setBackgroundColor(Color.parseColor("#333333"));
        settingsBtn.setTextColor(Color.WHITE);
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        main.addView(settingsBtn);

        setContentView(scroll);
    }

    private void updateHistory(int timeIndex) {
        try {
            JSONArray logs = logger.getLogs();
            StringBuilder sb = new StringBuilder();
            sb.append("Time    Grade  Ping   Data\n");
            sb.append("---------------------------\n");
            
            for (int i = logs.length() - 1; i >= 0; i--) {
                JSONObject obj = logs.getJSONObject(i);
                sb.append(obj.getString("time").substring(0, 8)).append(" ")
                  .append(obj.getString("grade")).append(" ")
                  .append(obj.getLong("ping")).append("ms ")
                  .append(obj.getLong("data")).append("KB\n");
            }
            tvHistory.setText(sb.toString());
        } catch (Exception e) {
            tvHistory.setText("No data");
        }
    }

    private void updateUI() {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        tvTime.setText(langManager.get("running") + ": " + (elapsed / 60) + "m " + (elapsed % 60) + "s");
        
        tvSignal.setText(langManager.get("operator") + ": " + tm.getNetworkOperatorName());
        
        try {
            long rx = android.net.TrafficStats.getMobileRxBytes();
            long tx = android.net.TrafficStats.getMobileTxBytes();
            long totalKB = (rx + tx) / 1024;
            tvData.setText(totalKB > 1024 ? (totalKB / 1024) + " MB" : totalKB + " KB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updater);
    }
}
