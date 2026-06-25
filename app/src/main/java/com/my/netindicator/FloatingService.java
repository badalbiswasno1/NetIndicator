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
    private LanguageManager langManager;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new FloatingWindowPrefs(this);
        langManager = new LanguageManager(this);
        
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
                .setContentText("Monitoring network...")
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
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.gravity = prefs.getGravity();
        params.x = 20;
        params.y = 100;

        floatingView = new TextView(this);
        floatingView.setText("?.?G");
        floatingView.setTextSize(prefs.getSize());
        floatingView.setTextColor(prefs.getTextColor());
        
        int bgColor = prefs.getBackgroundColor();
        int transparency = prefs.getTransparency();
        int alpha = 255 - (int)(transparency * 2.55);
        int finalBgColor = (bgColor & 0x00FFFFFF) | (alpha << 24);
        floatingView.setBackgroundColor(finalBgColor);
        floatingView.setPadding(20, 10, 20, 10);

        floatingView.setClickable(false);
        floatingView.setFocusable(false);
        floatingView.setFocusableInTouchMode(false);

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
                } else {
                    type = tm.getDataNetworkType();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            
            String grade = calculatePreciseGrade(type, tm);

            floatingView.setText(grade);
            floatingView.setTextColor(prefs.getTextColor());
            
            int bgColor = prefs.getBackgroundColor();
            int transparency = prefs.getTransparency();
            int alpha = 255 - (int)(transparency * 2.55);
            int finalBgColor = (bgColor & 0x00FFFFFF) | (alpha << 24);
            floatingView.setBackgroundColor(finalBgColor);
            
            floatingView.setTextSize(prefs.getSize());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String calculatePreciseGrade(int type, TelephonyManager tm) {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                return getNetworkName(type) + ".0G";
            }
            
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null || cells.isEmpty()) {
                return getNetworkName(type) + ".0G";
            }

            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;

                if (cell instanceof CellInfoNr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    CellSignalStrengthNr nr = (CellSignalStrengthNr) ((CellInfoNr) cell).getCellSignalStrength();
                    float score = calculate5GScore(nr.getSsRsrp(), nr.getSsRsrq());
                    return String.format("5.%dG", (int)(score * 9));
                }

                if (cell instanceof CellInfoLte) {
                    CellSignalStrengthLte lte = ((CellInfoLte) cell).getCellSignalStrength();
                    float score = calculate4GScore(lte.getRsrp(), lte.getRsrq(), lte.getRssnr());
                    return String.format("4.%dG", (int)(score * 9));
                }

                if (cell instanceof CellInfoWcdma) {
                    CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) cell).getCellSignalStrength();
                    float score = calculate3GScore(wcdma.getAsuLevel(), wcdma.getDbm());
                    return String.format("3.%dG", (int)(score * 9));
                }

                if (cell instanceof CellInfoGsm) {
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) cell).getCellSignalStrength();
                    float score = calculate2GScore(gsm.getAsuLevel(), gsm.getDbm());
                    return String.format("2.%dG", (int)(score * 9));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getNetworkName(type) + ".0G";
    }

    private float calculate5GScore(int ssRsrp, int ssRsrq) {
        float rsrpScore = 0;
        if (ssRsrp >= -80) rsrpScore = 1.0f;
        else if (ssRsrp >= -90) rsrpScore = 0.8f;
        else if (ssRsrp >= -100) rsrpScore = 0.6f;
        else if (ssRsrp >= -110) rsrpScore = 0.4f;
        else if (ssRsrp >= -120) rsrpScore = 0.2f;
        else rsrpScore = 0.0f;

        float rsrqScore = 0;
        if (ssRsrq >= -10) rsrqScore = 1.0f;
        else if (ssRsrq >= -15) rsrqScore = 0.7f;
        else if (ssRsrq >= -20) rsrqScore = 0.4f;
        else rsrqScore = 0.1f;

        return (rsrpScore * 0.6f + rsrqScore * 0.4f);
    }

    private float calculate4GScore(int rsrp, int rsrq, int snr) {
        float rsrpScore = 0;
        if (rsrp >= -80) rsrpScore = 1.0f;
        else if (rsrp >= -90) rsrpScore = 0.8f;
        else if (rsrp >= -100) rsrpScore = 0.6f;
        else if (rsrp >= -110) rsrpScore = 0.4f;
        else if (rsrp >= -120) rsrpScore = 0.2f;
        else rsrpScore = 0.0f;

        float rsrqScore = 0;
        if (rsrq >= -10) rsrqScore = 1.0f;
        else if (rsrq >= -15) rsrqScore = 0.7f;
        else if (rsrq >= -20) rsrqScore = 0.4f;
        else rsrqScore = 0.1f;

        float snrScore = 0;
        if (snr >= 20) snrScore = 1.0f;
        else if (snr >= 13) snrScore = 0.8f;
        else if (snr >= 0) snrScore = 0.5f;
        else snrScore = 0.1f;

        return (rsrpScore * 0.5f + rsrqScore * 0.3f + snrScore * 0.2f);
    }

    private float calculate3GScore(int asu, int dbm) {
        float asuScore = 0;
        if (asu >= 24) asuScore = 1.0f;
        else if (asu >= 16) asuScore = 0.7f;
        else if (asu >= 8) asuScore = 0.4f;
        else if (asu > 0) asuScore = 0.2f;
        else asuScore = 0.0f;

        float dbmScore = 0;
        if (dbm >= -70) dbmScore = 1.0f;
        else if (dbm >= -85) dbmScore = 0.7f;
        else if (dbm >= -100) dbmScore = 0.4f;
        else dbmScore = 0.1f;

        return (asuScore * 0.5f + dbmScore * 0.5f);
    }

    private float calculate2GScore(int asu, int dbm) {
        float asuScore = 0;
        if (asu >= 24) asuScore = 1.0f;
        else if (asu >= 16) asuScore = 0.7f;
        else if (asu >= 8) asuScore = 0.4f;
        else if (asu > 0) asuScore = 0.2f;
        else asuScore = 0.0f;

        float dbmScore = 0;
        if (dbm >= -70) dbmScore = 1.0f;
        else if (dbm >= -85) dbmScore = 0.7f;
        else if (dbm >= -100) dbmScore = 0.4f;
        else dbmScore = 0.1f;

        return (asuScore * 0.5f + dbmScore * 0.5f);
    }

    private String getNetworkName(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR: return "5";
            case TelephonyManager.NETWORK_TYPE_LTE: return "4";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
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
