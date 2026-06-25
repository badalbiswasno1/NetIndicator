package com.my.netindicator;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private LanguageManager langManager;
    private FloatingWindowPrefs windowPrefs;
    private SwipeRefreshLayout swipeRefresh;
    private Vibrator vibrator;
    private ProgressBar loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        windowPrefs = new FloatingWindowPrefs(this);
        startTime = System.currentTimeMillis();
        logger = new NetworkLogger(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

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
                new Thread(() -> {
                    try {
                        updateUI();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
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

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private void buildUI() {
        // Root SwipeRefreshLayout
        swipeRefresh = new SwipeRefreshLayout(this);
        swipeRefresh.setColorSchemeColors(Color.parseColor("#00CC44"), Color.parseColor("#0099FF"), Color.parseColor("#FFD700"));
        swipeRefresh.setOnRefreshListener(() -> {
            try {
                vibrate();
                updateUI();
            } catch (Exception e) {
                e.printStackTrace();
                swipeRefresh.setRefreshing(false);
            }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.parseColor("#111111"));
        main.setPadding(40, 60, 40, 40);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(main);

        // Loading bar at top
        loadingBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        loadingBar.setIndeterminate(true);
        loadingBar.setVisibility(View.GONE);
        main.addView(loadingBar);

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
        title.setText(langManager.get("true_network"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleRow.addView(title);

        main.addView(titleRow);

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
            vibrate();
            logger.clear();
            tvHistory.setText(langManager.get("no_data"));
        });
        main.addView(clearBtn);

        Button analyticsBtn = new Button(this);
        analyticsBtn.setText("📊 " + langManager.get("data_analytics"));
        analyticsBtn.setBackgroundColor(Color.parseColor("#0099FF"));
        analyticsBtn.setTextColor(Color.WHITE);
        analyticsBtn.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(this, DataAnalyticsActivity.class));
        });
        main.addView(analyticsBtn);

        Button settingsBtn = new Button(this);
        settingsBtn.setText("⚙ " + langManager.get("settings"));
        settingsBtn.setBackgroundColor(Color.parseColor("#333333"));
        settingsBtn.setTextColor(Color.WHITE);
        settingsBtn.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        main.addView(settingsBtn);

        swipeRefresh.addView(scroll);
        setContentView(swipeRefresh);
    }

    private void updateUI() {
        runOnUiThread(() -> loadingBar.setVisibility(View.VISIBLE));

        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        try {
            if (hasPermissions()) {
                type = tm.getDataNetworkType();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        int signalDbm = getSignalDbm(tm);

        String grade = calculateExactGrade(type, signalDbm);
        tvGrade.setText(grade);
        tvGrade.setTextColor(getGradeColor(grade));

        tvSignal.setText(langManager.get("operator") + ": " + tm.getNetworkOperatorName());

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        tvTime.setText(langManager.get("running") + ": " + (elapsed / 60) + "m " + (elapsed % 60) + "s");

        try {
            long rx = android.net.TrafficStats.getMobileRxBytes();
            long tx = android.net.TrafficStats.getMobileTxBytes();
            long totalKB = (rx + tx) / 1024;
            tvData.setText(totalKB > 1024 ? (totalKB / 1024) + " MB" : totalKB + " KB");
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateSignalDisplay(tm, signalDbm);

        new Thread(() -> {
            long ping = measurePing();
            final String pingText = ping >= 0 ? ping + " ms" : "timeout";
            final int pingColor = ping < 0 ? Color.RED :
                    ping < 100 ? Color.parseColor("#00CC44") :
                    ping < 300 ? Color.parseColor("#FFD700") : Color.parseColor("#E63329");

            runOnUiThread(() -> {
                tvPing.setText(langManager.get("ping") + ": " + pingText);
                tvPing.setTextColor(pingColor);

                try {
                    long rx = android.net.TrafficStats.getMobileRxBytes();
                    long tx = android.net.TrafficStats.getMobileTxBytes();
                    long dataKB = (rx + tx) / 1024;
                    logger.log(grade, ping, dataKB);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                updateHistory();
                loadingBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            });
        }).start();
    }

    // ============ EXACT GRADE CALCULATION ============
    private String calculateExactGrade(int networkType, int signalDbm) {
        double baseGrade = getBaseGrade(networkType);
        if (baseGrade <= 0) return "?.0G";
        double signalQuality = calculateSignalQuality(signalDbm);
        double exactGrade = baseGrade + signalQuality;
        double maxGrade = baseGrade + 0.9;
        if (exactGrade > maxGrade) exactGrade = maxGrade;
        return String.format("%.1fG", exactGrade);
    }

    private double getBaseGrade(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE: return 2.0;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA: return 3.0;
            case TelephonyManager.NETWORK_TYPE_LTE: return 4.0;
            case TelephonyManager.NETWORK_TYPE_NR: return 5.0;
            default: return 0.0;
        }
    }

    private double calculateSignalQuality(int signalDbm) {
        if (signalDbm == 0) return 0.0;
        if (signalDbm > -50) signalDbm = -50;
        if (signalDbm < -120) signalDbm = -120;
        double normalized = (double)(signalDbm + 120) / 70.0;
        double quality = normalized * 0.9;
        return Math.round(quality * 10) / 10.0;
    }

    private int getGradeColor(String grade) {
        if (grade.startsWith("5.")) return Color.parseColor("#00FF88");
        if (grade.startsWith("4.")) {
            double val = Double.parseDouble(grade.replace("G", ""));
            if (val >= 4.7) return Color.parseColor("#00FF44");
            if (val >= 4.4) return Color.parseColor("#00CC44");
            if (val >= 4.1) return Color.parseColor("#66CC00");
            return Color.parseColor("#99CC00");
        }
        if (grade.startsWith("3.")) {
            double val = Double.parseDouble(grade.replace("G", ""));
            if (val >= 3.7) return Color.parseColor("#FFD700");
            if (val >= 3.4) return Color.parseColor("#FFAA00");
            if (val >= 3.1) return Color.parseColor("#FF8800");
            return Color.parseColor("#FF6600");
        }
        if (grade.startsWith("2.")) return Color.parseColor("#FF4400");
        return Color.GRAY;
    }

    private int getSignalDbm(TelephonyManager tm) {
        try {
            if (!hasPermissions()) return 0;
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null || cells.isEmpty()) return 0;
            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;
                if (cell instanceof CellInfoLte) {
                    return ((CellInfoLte) cell).getCellSignalStrength().getDbm();
                }
                if (cell instanceof CellInfoNr) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        CellSignalStrengthNr nr = (CellSignalStrengthNr) ((CellInfoNr) cell).getCellSignalStrength();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try { return nr.getSsRsrp(); } catch (Exception e) { return nr.getDbm(); }
                        } else { return nr.getDbm(); }
                    }
                    return 0;
                }
                if (cell instanceof CellInfoWcdma) {
                    return ((CellInfoWcdma) cell).getCellSignalStrength().getDbm();
                }
                if (cell instanceof CellInfoGsm) {
                    return ((CellInfoGsm) cell).getCellSignalStrength().getDbm();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private void updateSignalDisplay(TelephonyManager tm, int signalDbm) {
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
                    tvDbm.setText("Signal: " + signalDbm + " dBm (RSRP: " + lte.getRsrp() + ")");
                    return;
                }
                if (cell instanceof CellInfoNr) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        CellSignalStrengthNr nr = (CellSignalStrengthNr) ((CellInfoNr) cell).getCellSignalStrength();
                        int ssRsrp = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try { ssRsrp = nr.getSsRsrp(); } catch (Exception e) { ssRsrp = nr.getDbm(); }
                        } else { ssRsrp = nr.getDbm(); }
                        tvDbm.setText("Signal: " + signalDbm + " dBm (5G NR SsRsrp: " + ssRsrp + ")");
                    } else {
                        tvDbm.setText("Signal: " + signalDbm + " dBm (5G NR)");
                    }
                    return;
                }
                if (cell instanceof CellInfoWcdma) {
                    tvDbm.setText("Signal: " + signalDbm + " dBm (3G)");
                    return;
                }
                if (cell instanceof CellInfoGsm) {
                    tvDbm.setText("Signal: " + signalDbm + " dBm (2G)");
                    return;
                }
            }
            tvDbm.setText(langManager.get("signal") + ": Unknown type");
        } catch (Exception e) {
            e.printStackTrace();
            tvDbm.setText(langManager.get("signal") + ": Error");
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
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    private void updateHistory() {
        try {
            JSONArray logs = logger.getLogs();
            StringBuilder sb = new StringBuilder();
            sb.append("Time    Grade   Ping  Data\n");
            sb.append("---------------------------\n");
            int count = 0;
            for (int i = logs.length() - 1; i >= 0 && count < 10; i--) {
                JSONObject obj = logs.getJSONObject(i);
                sb.append(obj.getString("time").substring(0, 8)).append(" ")
                  .append(String.format("%-6s", obj.getString("grade"))).append(" ")
                  .append(obj.getLong("ping")).append("ms ")
                  .append(obj.getLong("data")).append("KB\n");
                count++;
            }
            if (count == 0) sb.append(langManager.get("no_data"));
            tvHistory.setText(sb.toString());
        } catch (Exception e) {
            tvHistory.setText("Error loading history");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updater);
    }
}
