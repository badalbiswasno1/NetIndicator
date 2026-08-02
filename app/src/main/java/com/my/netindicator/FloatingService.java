package com.my.netindicator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
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
import android.view.WindowManager;
import android.widget.TextView;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

import java.util.List;

public class FloatingService extends Service {
    public static final String ACTION_PAUSE = "com.my.netindicator.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.my.netindicator.ACTION_RESUME";
    private WindowManager windowManager;
    private TextView floatingView;
    private Handler handler = new Handler();
    private Runnable updater;
    private FloatingWindowPrefs prefs;
    private boolean paused = false;
    private String lastGrade = "?.0G";
    private String lastPing = "--";
    private int lastAlertDbm = 0;
    private long lastAlertPing = -1;
    private String lastAlertBaseType = "";
    private boolean lastAlertCA = false;
    private boolean alertChannelCreated = false;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new FloatingWindowPrefs(this);

        if (prefs.isVisible()) {
            startForeground(1, createNotification());
            createFloatingView();
        } else {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (ACTION_PAUSE.equals(intent.getAction())) {
                paused = true;
                if (floatingView != null) floatingView.setVisibility(View.GONE);
                updateNotification();
            } else if (ACTION_RESUME.equals(intent.getAction())) {
                paused = false;
                if (floatingView != null) floatingView.setVisibility(View.VISIBLE);
                updateNotification();
            }
        }
        return START_STICKY;
    }

    private Notification createNotification() {
        String channelId = "floating_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Network Monitor", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Intent toggleIntent = new Intent(this, FloatingService.class);
        toggleIntent.setAction(paused ? ACTION_RESUME : ACTION_PAUSE);
        PendingIntent togglePendingIntent = PendingIntent.getService(this, 1, toggleIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String content = lastGrade + "  |  " + lastPing + "  |  " + (paused ? "Paused" : "Live");

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("True Network")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(openPendingIntent)
                .addAction(0, paused ? "Resume" : "Pause", togglePendingIntent)
                .addAction(0, "Open App", openPendingIntent)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void updateNotification() {
        try {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.notify(1, createNotification());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int touchFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (prefs.isLocked()) {
            touchFlags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                touchFlags,
                PixelFormat.TRANSLUCENT);

        params.gravity = prefs.getGravity();
        params.x = prefs.getX();
        params.y = prefs.getY();

        floatingView = new TextView(this);
        floatingView.setTextSize(prefs.getSize());
        floatingView.setTextColor(prefs.getTextColor());

        applyGlassBackground(floatingView, prefs.getBackgroundColor(), prefs.getTransparency());
        floatingView.setPadding(36, 22, 36, 22);

        // Draggable floating window
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!prefs.isLocked()) {
                            params.x = initialX - (int)(event.getRawX() - initialTouchX);
                            params.y = initialY + (int)(event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        prefs.setX(params.x);
                        prefs.setY(params.y);
                        return true;
                }
                return false;
            }
        });

        try {
            windowManager.addView(floatingView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        updater = new Runnable() {
            @Override
            public void run() {
                updateFloatingNetwork();
                handler.postDelayed(this, prefs.getRefreshInterval());
            }
        };
        handler.post(updater);
    }

    private boolean lastLockedState = false;

    private void updateFloatingNetwork() {
        try {
            boolean locked = prefs.isLocked();
            if (locked != lastLockedState) {
                lastLockedState = locked;
                android.view.WindowManager.LayoutParams p =
                        (android.view.WindowManager.LayoutParams) floatingView.getLayoutParams();
                int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                if (locked) {
                    flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                }
                p.flags = flags;
                windowManager.updateViewLayout(floatingView, p);
            }

            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;

            try {
                if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED) {
                    type = tm.getDataNetworkType();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            int signalDbm = getSignalDbm(tm);
            String grade = calculateExactGrade(type, signalDbm);
            checkAlerts(signalDbm, grade, type, tm);

            floatingView.setTextColor(prefs.getTextColor());
            applyGlassBackground(floatingView, prefs.getBackgroundColor(), prefs.getTransparency());
            floatingView.setTextSize(prefs.getSize());
            floatingView.setGravity(android.view.Gravity.CENTER);

            final String gradeFinal = grade;
            floatingView.setText(gradeFinal + "\n..." );

            new Thread(() -> {
                long ping = measurePing();
                final String pingText = ping >= 0 ? ping + "ms" : "--";
                lastGrade = gradeFinal;
                lastPing = pingText;
                NetworkWidgetProvider.updateWidgets(FloatingService.this, gradeFinal, pingText);
                android.content.SharedPreferences appSettingsPing = getSharedPreferences("AppSettings", MODE_PRIVATE);
                boolean pingAlertsOn = appSettingsPing.getBoolean("alert_ping", true);
                if (pingAlertsOn && lastAlertPing >= 0 && ping >= 0 && ping > lastAlertPing * 2 && ping > 150) {
                    sendAlert("Ping Increased", "Latency jumped to " + ping + "ms");
                }
                if (ping >= 0) lastAlertPing = ping;
                handler.post(() -> {
                    updateNotification();
                    if (floatingView != null) {
                        floatingView.setText(gradeFinal + "\n" + pingText);
                        try {
                            android.view.WindowManager.LayoutParams p =
                                    (android.view.WindowManager.LayoutParams) floatingView.getLayoutParams();
                            windowManager.updateViewLayout(floatingView, p);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getSignalDbm(TelephonyManager tm) {
        try {
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null) return 0;
            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;
                if (cell instanceof CellInfoLte) {
                    return ((CellInfoLte) cell).getCellSignalStrength().getDbm();
                }
                if (cell instanceof CellInfoNr) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        CellSignalStrengthNr nr = (CellSignalStrengthNr) ((CellInfoNr) cell).getCellSignalStrength();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                return nr.getSsRsrp();
                            } catch (Exception e) {
                                return nr.getDbm();
                            }
                        } else {
                            return nr.getDbm();
                        }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void applyGlassBackground(TextView view, int baseColor, int transparencyPercent) {
        int alpha = 255 - (int) (transparencyPercent * 2.55);
        int topColor = (baseColor & 0x00FFFFFF) | (Math.min(255, alpha + 30) << 24);
        int bottomColor = (baseColor & 0x00FFFFFF) | (Math.max(0, alpha - 30) << 24);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor});
        gd.setCornerRadius(40f);
        gd.setStroke(2, 0x33FFFFFF);
        view.setBackground(gd);
    }

    private void checkAlerts(int signalDbm, String grade, int networkType, TelephonyManager tm) {
        try {
            if (!prefs.isVisible()) return;

            android.content.SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
            boolean lowSignalAlertsOn = appSettings.getBoolean("alert_low_signal", true);

            String baseType = grade.length() > 0 ? grade.substring(0, 1) : "?";

            if (lowSignalAlertsOn && lastAlertDbm != 0 && signalDbm != 0 && (signalDbm - lastAlertDbm) <= -15) {
                sendAlert("Signal Dropped", "Signal weakened to " + signalDbm + " dBm");
            }

            if (!lastAlertBaseType.isEmpty() && !lastAlertBaseType.equals(baseType) && !baseType.equals("?")) {
                sendAlert("Network Changed", "Switched to " + grade + " network");
                if (baseType.equals("5")) {
                    sendAlert("5G Available", "5G network is now available");
                }
            }

            boolean ca = isCarrierAggregationCheck(tm);
            if (ca && !lastAlertCA) {
                sendAlert("Carrier Aggregation", "Carrier aggregation is now active");
            }
            lastAlertCA = ca;

            if (signalDbm != 0) lastAlertDbm = signalDbm;
            if (!baseType.equals("?")) lastAlertBaseType = baseType;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isCarrierAggregationCheck(TelephonyManager tm) {
        try {
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null) return false;
            int count = 0;
            for (CellInfo cell : cells) {
                if (cell.isRegistered() && (cell instanceof CellInfoLte || cell instanceof CellInfoNr)) {
                    count++;
                }
            }
            return count > 1;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendAlert(String title, String message) {
        try {
            String alertChannelId = "network_alerts";
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (!alertChannelCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        alertChannelId, "Network Alerts", NotificationManager.IMPORTANCE_DEFAULT);
                manager.createNotificationChannel(channel);
                alertChannelCreated = true;
            }
            Intent openIntent = new Intent(this, MainActivity.class);
            PendingIntent openPendingIntent = PendingIntent.getActivity(this, 2, openIntent,
                    PendingIntent.FLAG_IMMUTABLE);
            boolean soundOn = getSharedPreferences("AppSettings", MODE_PRIVATE).getBoolean("alert_sound", false);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, alertChannelId)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setContentIntent(openPendingIntent)
                    .setAutoCancel(true);
            if (soundOn) {
                builder.setDefaults(Notification.DEFAULT_SOUND);
            }
            Notification n = builder.build();
            manager.notify((int) System.currentTimeMillis(), n);
        } catch (Exception e) {
            e.printStackTrace();
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

    private String calculateExactGrade(int networkType, int signalDbm) {
        double baseGrade;
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE: baseGrade = 2.0; break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA: baseGrade = 3.0; break;
            case TelephonyManager.NETWORK_TYPE_LTE: baseGrade = 4.0; break;
            case TelephonyManager.NETWORK_TYPE_NR: baseGrade = 5.0; break;
            default: return "?.0G";
        }

        if (signalDbm == 0) return String.format("%.1fG", baseGrade);

        if (signalDbm > -50) signalDbm = -50;
        if (signalDbm < -120) signalDbm = -120;

        double normalized = (double)(signalDbm + 120) / 70.0;
        double quality = normalized * 0.9;
        quality = Math.round(quality * 10) / 10.0;

        double exactGrade = baseGrade + quality;
        double maxGrade = baseGrade + 0.9;
        if (exactGrade > maxGrade) exactGrade = maxGrade;

        return String.format("%.1fG", exactGrade);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updater);
        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
