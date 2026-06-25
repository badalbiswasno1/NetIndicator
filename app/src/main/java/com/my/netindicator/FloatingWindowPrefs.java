package com.my.netindicator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;

public class FloatingWindowPrefs {
    private SharedPreferences prefs;

    public FloatingWindowPrefs(Context ctx) {
        prefs = ctx.getSharedPreferences("floating_prefs", Context.MODE_PRIVATE);
    }

    public boolean isVisible() {
        return prefs.getBoolean("visible", false);
    }

    public void setVisible(boolean visible) {
        prefs.edit().putBoolean("visible", visible).apply();
    }

    public int getGravity() {
        return prefs.getInt("gravity", Gravity.TOP | Gravity.END);
    }

    public void setGravity(int gravity) {
        prefs.edit().putInt("gravity", gravity).apply();
    }

    public int getX() {
        return prefs.getInt("pos_x", 20);
    }

    public void setX(int x) {
        prefs.edit().putInt("pos_x", x).apply();
    }

    public int getY() {
        return prefs.getInt("pos_y", 100);
    }

    public void setY(int y) {
        prefs.edit().putInt("pos_y", y).apply();
    }

    public float getSize() {
        return prefs.getFloat("size", 16f);
    }

    public void setSize(float size) {
        prefs.edit().putFloat("size", size).apply();
    }

    public int getTransparency() {
        return prefs.getInt("transparency", 30);
    }

    public void setTransparency(int transparency) {
        prefs.edit().putInt("transparency", transparency).apply();
    }

    public int getTextColor() {
        return prefs.getInt("text_color", Color.parseColor("#00CC44"));
    }

    public void setTextColor(int color) {
        prefs.edit().putInt("text_color", color).apply();
    }

    public int getBackgroundColor() {
        return prefs.getInt("bg_color", Color.BLACK);
    }

    public void setBackgroundColor(int color) {
        prefs.edit().putInt("bg_color", color).apply();
    }

    public int getRefreshInterval() {
        return prefs.getInt("refresh_interval", 3000);
    }

    public void setRefreshInterval(int interval) {
        prefs.edit().putInt("refresh_interval", interval).apply();
    }

    public boolean isLocked() {
        return prefs.getBoolean("locked", false);
    }

    public void setLocked(boolean locked) {
        prefs.edit().putBoolean("locked", locked).apply();
    }
}
