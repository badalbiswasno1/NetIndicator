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
    private long baselineRx, baselineTx;
    private PingChartView chartView;
    private CircularScoreView scoreView;
    private TextView tvReason;
    private TextView tvHealthTitle, tvHealthOverall, tvHealthDns, tvHealthLatency, tvHealthLoss, tvHealthJitter;
    private TextView tvStabTitle, tvStabLabel, tvStabDetails;
    private TextView tvGamingTitle, tvGame1, tvGame2, tvGame3;
    private Handler healthHandler = new Handler();
    private Runnable healthUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        windowPrefs = new FloatingWindowPrefs(this);
        startTime = System.currentTimeMillis();
        logger = new NetworkLogger(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        baselineRx = android.net.TrafficStats.getMobileRxBytes();
        baselineTx = android.net.TrafficStats.getMobileTxBytes();

        java.io.File crashFile = new java.io.File(getFilesDir(), "crash_log.txt");
        if (crashFile.exists()) {
            try {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(crashFile));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Last Crash Log")
                        .setMessage(sb.toString())
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Copy", (d, w) -> {
                            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", sb.toString()));
                        })
                        .show();
                crashFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
                runOnUiThread(() -> {
                    try {
                        updateUI();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(updater);

        healthUpdater = new Runnable() {
            public void run() {
                new Thread(() -> {
                    try {
                        NetworkHealthEngine.Result hr = NetworkHealthEngine.measure();
                        runOnUiThread(() -> updateHealthCard(hr));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                healthHandler.postDelayed(this, 15000);
            }
        };
        healthHandler.postDelayed(healthUpdater, 2000);
    }

    private void updateHealthCard(NetworkHealthEngine.Result hr) {
        tvHealthOverall.setText("Overall Health: " + hr.healthPercent + " %");
        int color;
        if (hr.healthPercent >= 85) color = Color.parseColor("#00CC44");
        else if (hr.healthPercent >= 60) color = Color.parseColor("#0099FF");
        else if (hr.healthPercent >= 35) color = Color.parseColor("#FFA500");
        else color = Color.parseColor("#E63329");
        tvHealthOverall.setTextColor(color);
        tvHealthDns.setText("DNS: " + hr.dnsLabel + (hr.dnsMs >= 0 ? " (" + hr.dnsMs + "ms)" : ""));
        tvHealthLatency.setText("Latency: " + hr.latencyLabel + (hr.avgPing >= 0 ? " (" + hr.avgPing + "ms avg)" : ""));
        tvHealthLoss.setText("Packet Loss: " + hr.packetLossPercent + "%");
        tvHealthJitter.setText("Jitter: " + hr.jitter + "ms");

        java.util.List<GamingEngine.GameRating> ratings = GamingEngine.analyze(hr.avgPing, hr.jitter, hr.packetLossPercent);
        TextView[] gameViews = new TextView[]{tvGame1, tvGame2, tvGame3};
        for (int i = 0; i < ratings.size() && i < gameViews.length; i++) {
            GamingEngine.GameRating g = ratings.get(i);
            StringBuilder stars = new StringBuilder();
            for (int s = 0; s < 5; s++) stars.append(s < g.stars ? "\u2605" : "\u2606");
            gameViews[i].setText(g.name + ": " + g.status + " " + stars);
            gameViews[i].setTextColor(g.color);
        }
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
        tvGrade.setTextSize(40);
        tvGrade.setTypeface(null, android.graphics.Typeface.BOLD);
        tvGrade.setGravity(Gravity.CENTER);
        tvGrade.setTextColor(Color.parseColor("#00CC44"));
        tvGrade.setPadding(0, 20, 0, 0);
        main.addView(tvGrade);

        scoreView = new CircularScoreView(this);
        int scoreSize = (int) (240 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams scoreParams = new LinearLayout.LayoutParams(scoreSize, scoreSize);
        scoreParams.gravity = Gravity.CENTER_HORIZONTAL;
        scoreParams.topMargin = 10;
        main.addView(scoreView, scoreParams);

        tvReason = new TextView(this);
        tvReason.setText("");
        tvReason.setTextSize(13);
        tvReason.setGravity(Gravity.CENTER);
        tvReason.setTextColor(Color.parseColor("#CCCCCC"));
        tvReason.setPadding(20, 10, 20, 10);
        main.addView(tvReason);

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

        chartView = new PingChartView(this);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400);
        chartParams.topMargin = 10;
        chartParams.bottomMargin = 20;
        chartView.setLayoutParams(chartParams);
        main.addView(chartView);

        // Buttons
        Button clearBtn = new Button(this);
        clearBtn.setPadding(20, 14, 20, 14);
        clearBtn.setMinHeight(0);
        clearBtn.setMinimumHeight(0);
        clearBtn.setTextSize(12);
        clearBtn.setAllCaps(false);
        clearBtn.setText("🗑 Clear");
        clearBtn.setBackgroundColor(Color.parseColor("#E63329"));
        clearBtn.setTextColor(Color.WHITE);
        clearBtn.setOnClickListener(v -> {
            vibrate();
            logger.clear();
            chartView.setData(new java.util.ArrayList<>());
        });
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = 8;
        rowParams.bottomMargin = 20;

        LinearLayout.LayoutParams btnParams1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams1.setMargins(0, 0, 6, 0);
        btnRow.addView(clearBtn, btnParams1);

        Button analyticsBtn = new Button(this);
        analyticsBtn.setPadding(20, 14, 20, 14);
        analyticsBtn.setMinHeight(0);
        analyticsBtn.setMinimumHeight(0);
        analyticsBtn.setTextSize(12);
        analyticsBtn.setAllCaps(false);
        analyticsBtn.setText("📊 Stats");
        analyticsBtn.setBackgroundColor(Color.parseColor("#0099FF"));
        analyticsBtn.setTextColor(Color.WHITE);
        analyticsBtn.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(this, DataAnalyticsActivity.class));
        });
        LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams2.setMargins(6, 0, 6, 0);
        btnRow.addView(analyticsBtn, btnParams2);

        Button settingsBtn = new Button(this);
        settingsBtn.setPadding(20, 14, 20, 14);
        settingsBtn.setMinHeight(0);
        settingsBtn.setMinimumHeight(0);
        settingsBtn.setTextSize(12);
        settingsBtn.setAllCaps(false);
        settingsBtn.setText("⚙ Setup");
        settingsBtn.setBackgroundColor(Color.parseColor("#333333"));
        settingsBtn.setTextColor(Color.WHITE);
        settingsBtn.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        LinearLayout.LayoutParams btnParams3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams3.setMargins(6, 0, 0, 0);
        btnRow.addView(settingsBtn, btnParams3);

        LinearLayout healthCard = new LinearLayout(this);
        healthCard.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setColor(Color.parseColor("#1A1A1A"));
        cardBg.setCornerRadius(24f);
        healthCard.setBackground(cardBg);
        healthCard.setPadding(30, 24, 30, 24);
        LinearLayout.LayoutParams healthCardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        healthCardParams.topMargin = 10;
        healthCardParams.bottomMargin = 20;

        tvHealthTitle = new TextView(this);
        tvHealthTitle.setText("Internet Health");
        tvHealthTitle.setTextColor(Color.parseColor("#FFD700"));
        tvHealthTitle.setTextSize(15);
        tvHealthTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        healthCard.addView(tvHealthTitle);

        tvHealthOverall = new TextView(this);
        tvHealthOverall.setText("Overall Health: -- %");
        tvHealthOverall.setTextColor(Color.parseColor("#00CC44"));
        tvHealthOverall.setTextSize(20);
        tvHealthOverall.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHealthOverall.setPadding(0, 10, 0, 10);
        healthCard.addView(tvHealthOverall);

        tvHealthDns = new TextView(this);
        tvHealthDns.setText("DNS: --");
        tvHealthDns.setTextColor(Color.parseColor("#CCCCCC"));
        tvHealthDns.setTextSize(13);
        healthCard.addView(tvHealthDns);

        tvHealthLatency = new TextView(this);
        tvHealthLatency.setText("Latency: --");
        tvHealthLatency.setTextColor(Color.parseColor("#CCCCCC"));
        tvHealthLatency.setTextSize(13);
        healthCard.addView(tvHealthLatency);

        tvHealthLoss = new TextView(this);
        tvHealthLoss.setText("Packet Loss: --");
        tvHealthLoss.setTextColor(Color.parseColor("#CCCCCC"));
        tvHealthLoss.setTextSize(13);
        healthCard.addView(tvHealthLoss);

        tvHealthJitter = new TextView(this);
        tvHealthJitter.setText("Jitter: --");
        tvHealthJitter.setTextColor(Color.parseColor("#CCCCCC"));
        tvHealthJitter.setTextSize(13);
        healthCard.addView(tvHealthJitter);

        main.addView(healthCard, healthCardParams);

        LinearLayout stabilityCard = new LinearLayout(this);
        stabilityCard.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable stabBg = new android.graphics.drawable.GradientDrawable();
        stabBg.setColor(Color.parseColor("#1A1A1A"));
        stabBg.setCornerRadius(24f);
        stabilityCard.setBackground(stabBg);
        stabilityCard.setPadding(30, 24, 30, 24);
        LinearLayout.LayoutParams stabCardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stabCardParams.bottomMargin = 20;

        tvStabTitle = new TextView(this);
        tvStabTitle.setText("Stability Monitor");
        tvStabTitle.setTextColor(Color.parseColor("#FFD700"));
        tvStabTitle.setTextSize(15);
        tvStabTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        stabilityCard.addView(tvStabTitle);

        tvStabLabel = new TextView(this);
        tvStabLabel.setText("Analyzing...");
        tvStabLabel.setTextColor(Color.parseColor("#888888"));
        tvStabLabel.setTextSize(20);
        tvStabLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStabLabel.setPadding(0, 10, 0, 10);
        stabilityCard.addView(tvStabLabel);

        tvStabDetails = new TextView(this);
        tvStabDetails.setText("");
        tvStabDetails.setTextColor(Color.parseColor("#CCCCCC"));
        tvStabDetails.setTextSize(13);
        stabilityCard.addView(tvStabDetails);

        main.addView(stabilityCard, stabCardParams);

        LinearLayout gamingCard = new LinearLayout(this);
        gamingCard.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable gameBg = new android.graphics.drawable.GradientDrawable();
        gameBg.setColor(Color.parseColor("#1A1A1A"));
        gameBg.setCornerRadius(24f);
        gamingCard.setBackground(gameBg);
        gamingCard.setPadding(30, 24, 30, 24);
        LinearLayout.LayoutParams gameCardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gameCardParams.bottomMargin = 20;

        tvGamingTitle = new TextView(this);
        tvGamingTitle.setText("Gaming Analyzer");
        tvGamingTitle.setTextColor(Color.parseColor("#FFD700"));
        tvGamingTitle.setTextSize(15);
        tvGamingTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvGamingTitle.setPadding(0, 0, 0, 10);
        gamingCard.addView(tvGamingTitle);

        tvGame1 = new TextView(this);
        tvGame1.setText("PUBG Mobile: --");
        tvGame1.setTextColor(Color.parseColor("#CCCCCC"));
        tvGame1.setTextSize(13);
        tvGame1.setPadding(0, 4, 0, 4);
        gamingCard.addView(tvGame1);

        tvGame2 = new TextView(this);
        tvGame2.setText("Call of Duty Mobile: --");
        tvGame2.setTextColor(Color.parseColor("#CCCCCC"));
        tvGame2.setTextSize(13);
        tvGame2.setPadding(0, 4, 0, 4);
        gamingCard.addView(tvGame2);

        tvGame3 = new TextView(this);
        tvGame3.setText("Free Fire: --");
        tvGame3.setTextColor(Color.parseColor("#CCCCCC"));
        tvGame3.setTextSize(13);
        tvGame3.setPadding(0, 4, 0, 4);
        gamingCard.addView(tvGame3);

        main.addView(gamingCard, gameCardParams);

        main.addView(btnRow, rowParams);

        swipeRefresh.addView(scroll);
        setContentView(swipeRefresh);
    }

    private void updateUI() {
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

        boolean isWifi = isWifiConnected();
        String grade = calculateExactGrade(type, signalDbm);
        if (isWifi) {
            tvGrade.setText("WiFi");
            tvGrade.setTextColor(Color.parseColor("#0099FF"));
        } else {
            tvGrade.setText(grade);
            tvGrade.setTextColor(getGradeColor(grade));
        }

        tvSignal.setText(langManager.get("operator") + ": " + tm.getNetworkOperatorName());

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        tvTime.setText(langManager.get("running") + ": " + (elapsed / 60) + "m " + (elapsed % 60) + "s");

        try {
            long rx = android.net.TrafficStats.getMobileRxBytes() - baselineRx;
            long tx = android.net.TrafficStats.getMobileTxBytes() - baselineTx;
            long totalKB = Math.max(0, (rx + tx) / 1024);
            tvData.setText(langManager.get("session_data") + ": " + (totalKB > 1024 ? (totalKB / 1024) + " MB" : totalKB + " KB"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateSignalDisplay(tm, signalDbm, isWifi);

        final int signalDbmFinal = signalDbm;
        final double baseGradeFinal = getBaseGrade(type);
        final int rsrqFinal = getRsrq(tm);
        final int sinrFinal = getSinr(tm);
        final boolean caFinal = isCarrierAggregation(tm);
        final boolean isWifiFinal = isWifi;
        final boolean hasSinrFinal = sinrFinal != 0;

        new Thread(() -> {
            long ping = measurePing();
            final String pingText = ping >= 0 ? ping + " ms" : "timeout";
            final int pingColor;
            if (ping < 0) {
                pingColor = Color.parseColor("#FF0000");
            } else if (ping < 50) {
                pingColor = Color.parseColor("#00FF44");
            } else if (ping < 100) {
                pingColor = Color.parseColor("#00CC44");
            } else if (ping < 150) {
                pingColor = Color.parseColor("#88CC00");
            } else if (ping < 200) {
                pingColor = Color.parseColor("#FFD700");
            } else if (ping < 300) {
                pingColor = Color.parseColor("#FF8800");
            } else if (ping < 500) {
                pingColor = Color.parseColor("#FF4400");
            } else {
                pingColor = Color.parseColor("#CC0000");
            }

            runOnUiThread(() -> {
                tvPing.setText(langManager.get("ping") + ": " + pingText);
                tvPing.setTextColor(pingColor);

                NetworkScoreEngine.Result scoreResult = NetworkScoreEngine.compute(
                        baseGradeFinal, signalDbmFinal, rsrqFinal, sinrFinal, ping, caFinal, hasSinrFinal, isWifiFinal);
                scoreView.setScore(scoreResult.score, scoreResult.categoryColor, scoreResult.category);
                tvReason.setText("Reason: " + android.text.TextUtils.join(", ", scoreResult.reasons));

                try {
                    long rx = android.net.TrafficStats.getMobileRxBytes() - baselineRx;
                    long tx = android.net.TrafficStats.getMobileTxBytes() - baselineTx;
                    long dataKB = Math.max(0, (rx + tx) / 1024);
                    logger.log(grade, ping, dataKB, signalDbmFinal);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                updateHistory();
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

    private int getRsrq(TelephonyManager tm) {
        try {
            if (!hasPermissions()) return 0;
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null) return 0;
            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;
                if (cell instanceof CellInfoLte) {
                    return ((CellInfoLte) cell).getCellSignalStrength().getRsrq();
                }
                if (cell instanceof CellInfoNr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    CellSignalStrengthNr nr = (CellSignalStrengthNr) ((CellInfoNr) cell).getCellSignalStrength();
                    try { return nr.getSsRsrq(); } catch (Exception e) { return 0; }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private int getSinr(TelephonyManager tm) {
        try {
            if (!hasPermissions()) return 0;
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null) return 0;
            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;
                if (cell instanceof CellInfoLte) {
                    return ((CellInfoLte) cell).getCellSignalStrength().getRssnr();
                }
                if (cell instanceof CellInfoNr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    CellSignalStrengthNr nr = (CellSignalStrengthNr) ((CellInfoNr) cell).getCellSignalStrength();
                    try { return nr.getSsSinr(); } catch (Exception e) { return 0; }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private boolean isCarrierAggregation(TelephonyManager tm) {
        try {
            if (!hasPermissions()) return false;
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null) return false;
            int registeredCount = 0;
            for (CellInfo cell : cells) {
                if (cell.isRegistered() && (cell instanceof CellInfoLte || cell instanceof CellInfoNr)) {
                    registeredCount++;
                }
            }
            return registeredCount > 1;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private boolean isWifiConnected() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateSignalDisplay(TelephonyManager tm, int signalDbm, boolean isWifi) {
        if (isWifi) {
            tvDbm.setText(langManager.get("signal") + ": WiFi Connected");
            return;
        }
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
            java.util.List<Long> pingVals = new java.util.ArrayList<>();
            int count = 0;
            int start = Math.max(0, logs.length() - 20);
            for (int i = start; i < logs.length(); i++) {
                JSONObject obj = logs.getJSONObject(i);
                pingVals.add(obj.getLong("ping"));
                count++;
            }
            chartView.setData(pingVals);

            StabilityEngine.Result stab = StabilityEngine.analyze(logs, 20);
            tvStabLabel.setText(stab.label);
            tvStabLabel.setTextColor(stab.labelColor);
            tvStabDetails.setText(
                    "Ping variation: " + String.format("%.1f", stab.pingStdDev) + "ms\n" +
                    "Network drops: " + stab.dropCount + "\n" +
                    "Grade changes: " + stab.gradeChanges);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updater);
        healthHandler.removeCallbacks(healthUpdater);
    }
}
