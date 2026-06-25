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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = prefs.getGravity();

        floatingView = new TextView(this);
        floatingView.setText("?.?G");
        floatingView.setTextSize(prefs.getSize());
        floatingView.setTextColor(prefs.getTextColor());
        floatingView.setBackgroundColor(prefs.getBackgroundColor());
        floatingView.setPadding(20, 10, 20, 10);

        try {
            windowManager.addView(floatingView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        updater = new Runnable() {
            @Override
            public void run() {
                updateFloatingNetwork();
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(updater);
    }

    private void updateFloatingNetwork() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) 
                            == PackageManager.PERMISSION_GRANTED) {
                        type = tm.getDataNetworkType();
                    }
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            
            String grade = getNetworkName(type) + ".0G";
            floatingView.setText(grade);
            
        } catch (Exception e) {
            e.printStackTrace();
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
