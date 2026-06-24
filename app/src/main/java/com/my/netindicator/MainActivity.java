package com.my.netindicator;

import android.app.Activity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Color;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.BLACK);
        
        TextView tv = new TextView(this);
        tv.setTextSize(72);
        tv.setTextColor(Color.YELLOW);
        tv.setGravity(Gravity.CENTER);
        
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        int type = tm.getDataNetworkType();
        String network = getNetworkName(type);
        
        tv.setText(network);
        layout.addView(tv);
        setContentView(layout);
    }
    
    private String getNetworkName(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR: return "5G";
            case TelephonyManager.NETWORK_TYPE_LTE: return "4G";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_UMTS: return "3G";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS: return "2G";
            default: return "?G";
        }
    }
}
