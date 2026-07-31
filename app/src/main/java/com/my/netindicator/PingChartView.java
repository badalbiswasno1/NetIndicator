package com.my.netindicator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PingChartView extends View {
    private List<Long> values = new ArrayList<>();
    private Paint linePaint, dotPaint, gridPaint, textPaint, fillPaint;

    public PingChartView(Context context) {
        super(context);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#00CC44"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#2200CC44"));
        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#00FF66"));
        dotPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#333333"));
        gridPaint.setStrokeWidth(1f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#888888"));
        textPaint.setTextSize(22f);
    }

    public void setData(List<Long> newValues) {
        values = newValues;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int padLeft = 80, padRight = 20, padTop = 20, padBottom = 30;

        if (values == null || values.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No data yet", w / 2f, h / 2f, textPaint);
            return;
        }

        long max = 10, min = 0;
        for (long v : values) {
            if (v > max) max = v;
        }
        max = (long) (max * 1.15);

        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            float y = padTop + (h - padTop - padBottom) * i / (float) steps;
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);
            long labelVal = max - (max - min) * i / steps;
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(labelVal + "ms", 4, y + 8, textPaint);
        }

        int n = values.size();
        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        Path linePath = new Path();
        Path fillPath = new Path();
        float[] xs = new float[n];
        float[] ys = new float[n];

        for (int i = 0; i < n; i++) {
            long v = Math.max(values.get(i), 0);
            float x = padLeft + (n == 1 ? chartW / 2f : chartW * i / (float) (n - 1));
            float y = padTop + chartH - (chartH * (v - min) / (float) (max - min));
            xs[i] = x;
            ys[i] = y;
            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, padTop + chartH);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        fillPath.lineTo(xs[n - 1], padTop + chartH);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], 5f, dotPaint);
        }
    }
}
