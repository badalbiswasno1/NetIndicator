package com.my.netindicator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class FloatingWindowPrefs {
    private static final String PREFS_NAME = "FloatingWindowPrefs";
    private static final String KEY_VISIBLE = "visible";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_BG_COLOR = "bg_color";
    private static final String KEY_TRANSPARENCY = "transparency";
    private static final String KEY_SIZE = "size";
    private static final String KEY_POSITION = "position";
    
    private SharedPreferences prefs;
    
    public FloatingWindowPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean isVisible() {
        return prefs.getBoolean(KEY_VISIBLE, true);
    }
    
    public void setVisible(boolean visible) {
        prefs.edit().putBoolean(KEY_VISIBLE, visible).apply();
    }
    
    public int getTextColor() {
        return prefs.getInt(KEY_TEXT_COLOR, Color.parseColor("#00CC44"));
    }
    
    public void setTextColor(int color) {
        prefs.edit().putInt(KEY_TEXT_COLOR, color).apply();
    }
    
    public int getBackgroundColor() {
        return prefs.getInt(KEY_BG_COLOR, Color.parseColor("#CC000000"));
    }
    
    public void setBackgroundColor(int color) {
        prefs.edit().putInt(KEY_BG_COLOR, color).apply();
    }
    
    public int getTransparency() {
        return prefs.getInt(KEY_TRANSPARENCY, 0);
    }
    
    public void setTransparency(int transparency) {
        prefs.edit().putInt(KEY_TRANSPARENCY, transparency).apply();
    }
    
    public int getSize() {
        return prefs.getInt(KEY_SIZE, 14);
    }
    
    public void setSize(int size) {
        prefs.edit().putInt(KEY_SIZE, size).apply();
    }
    
    public String getPosition() {
        return prefs.getString(KEY_POSITION, "Top-Right");
    }
    
    public void setPosition(String position) {
        prefs.edit().putString(KEY_POSITION, position).apply();
    }
    
    public int getGravity() {
        String pos = getPosition();
        switch (pos) {
            case "Top-Left": return android.view.Gravity.TOP | android.view.Gravity.START;
            case "Top-Right": return android.view.Gravity.TOP | android.view.Gravity.END;
            case "Bottom-Left": return android.view.Gravity.BOTTOM | android.view.Gravity.START;
            case "Bottom-Right": return android.view.Gravity.BOTTOM | android.view.Gravity.END;
            case "Center": return android.view.Gravity.CENTER;
            default: return android.view.Gravity.TOP | android.view.Gravity.END;
        }
    }
}
