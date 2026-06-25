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
    private WindowManager windowManager;
    private TextView floatingView;
    private Handler handler = new Handler();
    private Runnable updater;
    private FloatingWindowPrefs prefs;

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

    private Notification createNotification() {
        String channelId = "floating_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Network Monitor", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("True Network")
                .setContentText("Monitoring...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = prefs.getGravity();
        params.x = prefs.getX();
        params.y = prefs.getY();

        floatingView = new TextView(this);
        floatingView.setTextSize(prefs.getSize());
        floatingView.setTextColor(prefs.getTextColor());

        int bgColor = prefs.getBackgroundColor();
        int alpha = 255 - (int)(prefs.getTransparency() * 2.55);
        int finalBg = (bgColor & 0x00FFFFFF) | (alpha << 24);
        floatingView.setBackgroundColor(finalBg);
        floatingView.setPadding(20, 10, 20, 10);

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
                        params.x = initialX + (int)(event.getRawX() - initialTouchX);
                        params.y = initialY + (int)(event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
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

    private void updateFloatingNetwork() {
        try {
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
            floatingView.setText(grade);

            floatingView.setTextColor(prefs.getTextColor());
            int bgColor = prefs.getBackgroundColor();
            int alpha = 255 - (int)(prefs.getTransparency() * 2.55);
            int finalBg = (bgColor & 0x00FFFFFF) | (alpha << 24);
            floatingView.setBackgroundColor(finalBg);
            floatingView.setTextSize(prefs.getSize());

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
