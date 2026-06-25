package com.my.netindicator;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
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

import java.util.List;

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
    private long startTime;
    private NetworkLogger logger;
    private SeekBar timeSeek;
    private TextView tvTimeLabel;
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
        main.setPadding(40, 60, 40, 40);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(main);

        // Title
        TextView title = new TextView(this);
        title.setText(langManager.get("true_network"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        main.addView(title);

        // Grade Display
        tvGrade = new TextView(this);
        tvGrade.setText("?.0G");
        tvGrade.setTextSize(64);
        tvGrade.setTypeface(null, android.graphics.Typeface.BOLD);
        tvGrade.setGravity(Gravity.CENTER);
        tvGrade.setTextColor(Color.parseColor("#00CC44"));
        tvGrade.setPadding(0, 20, 0, 10);
        main.addView(tvGrade);

        // Ping
        tvPing = new TextView(this);
        tvPing.setText(langManager.get("ping") + ": -- ms");
        tvPing.setTextSize(18);
        tvPing.setGravity(Gravity.CENTER);
        tvPing.setTextColor(Color.WHITE);
        main.addView(tvPing);

        // Signal dBm
        tvDbm = new TextView(this);
        tvDbm.setText(langManager.get("signal") + ": -- dBm");
        tvDbm.setTextSize(14);
        tvDbm.setGravity(Gravity.CENTER);
        tvDbm.setTextColor(Color.parseColor("#AAAAAA"));
        main.addView(tvDbm);

        // Operator
        tvSignal = new TextView(this);
        tvSignal.setText(langManager.get("operator") + ": --");
        tvSignal.setTextColor(Color.parseColor("#00CC44"));
        tvSignal.setTextSize(16);
        tvSignal.setGravity(Gravity.CENTER);
        tvSignal.setPadding(0, 20, 0, 5);
        main.addView(tvSignal);

        // Running time
        tvTime = new TextView(this);
        tvTime.setText(langManager.get("running") + ": 0s");
        tvTime.setTextColor(Color.parseColor("#AAAAAA"));
        tvTime.setTextSize(13);
        tvTime.setGravity(Gravity.CENTER);
        main.addView(tvTime);

        // Data usage
        tvData = new TextView(this);
        tvData.setText("0 MB");
        tvData.setTextColor(Color.parseColor("#FFD700"));
        tvData.setTextSize(16);
        tvData.setGravity(Gravity.CENTER);
        tvData.setPadding(0, 5, 0, 20);
        main.addView(tvData);

        // History Section
        TextView histTitle = new TextView(this);
        histTitle.setText(langManager.get("network_history"));
        histTitle.setTextColor(Color.parseColor("#FFD700"));
        histTitle.setTextSize(15);
        histTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        main.addView(histTitle);

        tvHistory = new TextView(this);
        tvHistory.setTextColor(Color.parseColor("#CCCCCC"));
        tvHistory.setTextSize(11);
        tvHistory.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvHistory.setPadding(0, 10, 0, 10);
        main.addView(tvHistory);

        // Buttons
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

    private void updateUI() {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        // Get network type
        int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        try {
            if (hasPermissions()) {
                type = tm.getDataNetworkType();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // Display grade
        String grade = getNetworkName(type) + ".0G";
        tvGrade.setText(grade);
        tvGrade.setTextColor(getNetworkColor(type));

        // Operator
        tvSignal.setText(langManager.get("operator") + ": " + tm.getNetworkOperatorName());

        // Running time
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        tvTime.setText(langManager.get("running") + ": " + (elapsed / 60) + "m " + (elapsed % 60) + "s");

        // Data usage
        try {
            long rx = android.net.TrafficStats.getMobileRxBytes();
            long tx = android.net.TrafficStats.getMobileTxBytes();
            long totalKB = (rx + tx) / 1024;
            tvData.setText(totalKB > 1024 ? (totalKB / 1024) + " MB" : totalKB + " KB");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Signal Strength (dBm)
        updateSignalInfo(tm);

        // Ping (background thread)
        new Thread(() -> {
            long ping = measurePing();
            final String pingText = ping >= 0 ? ping + " ms" : "timeout";
            final int pingColor = ping < 0 ? Color.RED :
                    ping < 100 ? Color.parseColor("#00CC44") :
                    ping < 300 ? Color.parseColor("#FFD700") : Color.parseColor("#E63329");

            runOnUiThread(() -> {
                tvPing.setText(langManager.get("ping") + ": " + pingText);
                tvPing.setTextColor(pingColor);
                
                // Log data
                try {
                    long rx = android.net.TrafficStats.getMobileRxBytes();
                    long tx = android.net.TrafficStats.getMobileTxBytes();
                    long dataKB = (rx + tx) / 1024;
                    logger.log(grade, ping, dataKB);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                updateHistory();
            });
        }).start();
    }

    private void updateSignalInfo(TelephonyManager tm) {
        try {
            if (!hasPermissions()) {
                tvDbm.setText(langManager.get("signal") + ": No permission");
                return;
            }

            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null || cells.isEmpty()) {
                tvDbm.setText(langManager.get("signal") + ": No signal info");
                return;
            }

            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;

                if (cell instanceof CellInfoLte) {
                    CellSignalStrengthLte lte = ((CellInfoLte) cell).getCellSignalStrength();
                    int dbm = lte.getDbm();
                    int rsrp = lte.getRsrp();
                    tvDbm.setText("Signal: " + dbm + " dBm (RSRP: " + rsrp + ")");
                    return;
                }

                if (cell instanceof CellInfoNr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    CellSignalStrengthNr nr = (CellSignalStrengthNr) ((CellInfoNr) cell).getCellSignalStrength();
                    int ssRsrp = nr.getSsRsrp();
                    tvDbm.setText("Signal: " + ssRsrp + " dBm (5G NR)");
                    return;
                }

                if (cell instanceof CellInfoWcdma) {
                    CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) cell).getCellSignalStrength();
                    int dbm = wcdma.getDbm();
                    tvDbm.setText("Signal: " + dbm + " dBm (3G)");
                    return;
                }

                if (cell instanceof CellInfoGsm) {
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) cell).getCellSignalStrength();
                    int dbm = gsm.getDbm();
                    tvDbm.setText("Signal: " + dbm + " dBm (2G)");
                    return;
                }
            }

            tvDbm.setText(langManager.get("signal") + ": Unknown type");

        } catch (Exception e) {
            e.printStackTrace();
            tvDbm.setText(langManager.get("signal") + ": Error");
        }
    }

    private String getNetworkName(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR: return "5";
            case TelephonyManager.NETWORK_TYPE_LTE: return "4";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_UMTS: return "3";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS: return "2";
            default: return "?";
        }
    }

    private int getNetworkColor(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR: return Color.parseColor("#00FF88");
            case TelephonyManager.NETWORK_TYPE_LTE: return Color.parseColor("#00CC44");
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_UMTS: return Color.parseColor("#FFD700");
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS: return Color.parseColor("#FF8800");
            default: return Color.GRAY;
        }
    }

    private long measurePing() {
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8");
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("time=")) {
                    int start = line.indexOf("time=") + 5;
                    int end = line.indexOf(" ms", start);
                    return (long) Float.parseFloat(line.substring(start, end));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void updateHistory() {
        try {
            JSONArray logs = logger.getLogs();
            StringBuilder sb = new StringBuilder();
            sb.append("Time    Grade  Ping  Data\n");
            sb.append("--------------------------\n");
            
            int count = 0;
            for (int i = logs.length() - 1; i >= 0 && count < 10; i--) {
                JSONObject obj = logs.getJSONObject(i);
                sb.append(obj.getString("time").substring(0, 8)).append(" ")
                  .append(obj.getString("grade")).append(" ")
                  .append(obj.getLong("ping")).append("ms ")
                  .append(obj.getLong("data")).append("KB\n");
                count++;
            }
            
            if (count == 0) {
                sb.append(langManager.get("no_data"));
            }
            
            tvHistory.setText(sb.toString());
        } catch (Exception e) {
            tvHistory.setText("Error");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updater);
    }
}
