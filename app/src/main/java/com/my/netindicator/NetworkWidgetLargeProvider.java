package com.my.netindicator;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class NetworkWidgetLargeProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);
        String grade = prefs.getString("grade", "?.0G");
        String ping = prefs.getString("ping", "-- ms");
        String signal = prefs.getString("signal", "Signal: -- dBm");
        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);
            views.setTextViewText(R.id.widgetGrade, grade);
            views.setTextViewText(R.id.widgetPing, ping);
            views.setTextViewText(R.id.widgetSignal, signal);
            appWidgetManager.updateAppWidget(id, views);
        }
    }
}
