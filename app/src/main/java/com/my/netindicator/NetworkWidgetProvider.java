package com.my.netindicator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class NetworkWidgetProvider extends AppWidgetProvider {

    private static final String PREFS_NAME = "widget_prefs";

    public static void updateWidgets(Context context, String grade, String ping) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("grade", grade).putString("ping", ping).apply();

            AppWidgetManager manager = AppWidgetManager.getInstance(context);

            int[] ids = manager.getAppWidgetIds(new android.content.ComponentName(context, NetworkWidgetProvider.class));
            for (int id : ids) {
                pushUpdate(context, manager, id, grade, ping);
            }

            int[] smallIds = manager.getAppWidgetIds(new android.content.ComponentName(context, NetworkWidgetSmallProvider.class));
            for (int id : smallIds) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_small);
                views.setTextViewText(R.id.widgetGrade, grade);
                manager.updateAppWidget(id, views);
            }

            String signal = prefs.getString("signal", "Signal: -- dBm");
            int[] largeIds = manager.getAppWidgetIds(new android.content.ComponentName(context, NetworkWidgetLargeProvider.class));
            for (int id : largeIds) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);
                views.setTextViewText(R.id.widgetGrade, grade);
                views.setTextViewText(R.id.widgetPing, ping);
                views.setTextViewText(R.id.widgetSignal, signal);
                manager.updateAppWidget(id, views);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateSignal(Context context, String signal) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("signal", signal).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pushUpdate(Context context, AppWidgetManager manager, int widgetId, String grade, String ping) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_network);
        views.setTextViewText(R.id.widgetGrade, grade);
        views.setTextViewText(R.id.widgetPing, ping);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetGrade, pendingIntent);

        manager.updateAppWidget(widgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String grade = prefs.getString("grade", "?.0G");
        String ping = prefs.getString("ping", "-- ms");
        for (int id : appWidgetIds) {
            pushUpdate(context, appWidgetManager, id, grade, ping);
        }
    }
}
