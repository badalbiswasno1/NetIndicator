package com.my.netindicator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PingChartView extends View {
    private List<Long> values = new ArrayList<>();
    private Paint linePaint, dotPaint, gridPaint, textPaint, fillPaint, avgPaint, bestPaint, worstPaint, tooltipBg, tooltipText;
    private int touchIndex = -1;
    private float[] lastXs, lastYs;

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
        gridPaint.setColor(Color.parseColor("#222222"));
        gridPaint.setStrokeWidth(1f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#777777"));
        textPaint.setTextSize(22f);

        avgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avgPaint.setColor(Color.parseColor("#FFD700"));
        avgPaint.setStrokeWidth(2.5f);
        avgPaint.setStyle(Paint.Style.STROKE);
        avgPaint.setPathEffect(new DashPathEffect(new float[]{12f, 8f}, 0));

        bestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bestPaint.setColor(Color.parseColor("#00FF66"));
        bestPaint.setStyle(Paint.Style.FILL);

        worstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        worstPaint.setColor(Color.parseColor("#FF4444"));
        worstPaint.setStyle(Paint.Style.FILL);

        tooltipBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBg.setColor(Color.parseColor("#DD222222"));
        tooltipBg.setStyle(Paint.Style.FILL);

        tooltipText = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipText.setColor(Color.WHITE);
        tooltipText.setTextSize(24f);
        tooltipText.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Long> newValues) {
        values = newValues;
        touchIndex = -1;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (lastXs == null || lastXs.length == 0) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float touchX = event.getX();
            int nearest = 0;
            float minDist = Float.MAX_VALUE;
            for (int i = 0; i < lastXs.length; i++) {
                float d = Math.abs(lastXs[i] - touchX);
                if (d < minDist) {
                    minDist = d;
                    nearest = i;
                }
            }
            touchIndex = nearest;
            invalidate();
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            touchIndex = -1;
            invalidate();
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
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
        long sum = 0;
        int validCount = 0;
        int bestIdx = -1, worstIdx = -1;
        long bestVal = Long.MAX_VALUE, worstVal = -1;

        for (int i = 0; i < values.size(); i++) {
            long v = values.get(i);
            if (v > max) max = v;
            if (v >= 0) {
                sum += v;
                validCount++;
                if (v < bestVal) { bestVal = v; bestIdx = i; }
                if (v > worstVal) { worstVal = v; worstIdx = i; }
            }
        }
        max = (long) (max * 1.15);
        double avg = validCount > 0 ? (double) sum / validCount : 0;

        int steps = 3;
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
        lastXs = xs;
        lastYs = ys;

        canvas.drawPath(fillPath, fillPaint);

        if (validCount > 0) {
            float avgY = padTop + chartH - (chartH * (float) (avg - min) / (float) (max - min));
            canvas.drawLine(padLeft, avgY, w - padRight, avgY, avgPaint);
        }

        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], 5f, dotPaint);
        }

        if (bestIdx >= 0) {
            canvas.drawCircle(xs[bestIdx], ys[bestIdx], 9f, bestPaint);
        }
        if (worstIdx >= 0 && worstIdx != bestIdx) {
            canvas.drawCircle(xs[worstIdx], ys[worstIdx], 9f, worstPaint);
        }

        if (touchIndex >= 0 && touchIndex < n) {
            float tx = xs[touchIndex];
            float ty = ys[touchIndex];
            canvas.drawLine(tx, padTop, tx, padTop + chartH, gridPaint);
            canvas.drawCircle(tx, ty, 7f, dotPaint);

            String label = values.get(touchIndex) + "ms";
            float boxW = tooltipText.measureText(label) + 30;
            float boxH = 50;
            float boxX = Math.min(Math.max(tx - boxW / 2, padLeft), w - padRight - boxW);
            float boxY = Math.max(ty - boxH - 15, padTop);
            RectF box = new RectF(boxX, boxY, boxX + boxW, boxY + boxH);
            canvas.drawRoundRect(box, 12f, 12f, tooltipBg);
            canvas.drawText(label, box.centerX(), box.centerY() + 8, tooltipText);
        }
    }
}
