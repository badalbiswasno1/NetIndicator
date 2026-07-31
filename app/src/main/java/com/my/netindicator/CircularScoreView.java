package com.my.netindicator;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class CircularScoreView extends View {
    private float animatedScore = 0f;
    private int targetScore = 0;
    private int ringColor = Color.parseColor("#00CC44");
    private String centerLabel = "0";
    private String subLabel = "";
    private Paint bgRing, fgRing, textPaint, subTextPaint;
    private ValueAnimator animator;

    public CircularScoreView(Context context) {
        super(context);
        init();
    }

    private void init() {
        bgRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgRing.setStyle(Paint.Style.STROKE);
        bgRing.setStrokeWidth(24f);
        bgRing.setColor(Color.parseColor("#222222"));
        bgRing.setStrokeCap(Paint.Cap.ROUND);

        fgRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        fgRing.setStyle(Paint.Style.STROKE);
        fgRing.setStrokeWidth(24f);
        fgRing.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(Color.parseColor("#AAAAAA"));
        subTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setScore(int score, int color, String sub) {
        this.targetScore = score;
        this.ringColor = color;
        this.subLabel = sub;
        fgRing.setColor(color);

        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(animatedScore, score);
        animator.setDuration(700);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            animatedScore = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float strokeW = 24f;
        float size = Math.min(w, h) - strokeW;
        RectF rect = new RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f);

        canvas.drawArc(rect, 135, 270, false, bgRing);
        float sweep = 270f * (animatedScore / 100f);
        canvas.drawArc(rect, 135, sweep, false, fgRing);

        textPaint.setTextSize(size * 0.28f);
        canvas.drawText(String.valueOf(Math.round(animatedScore)), w / 2f, h / 2f + size * 0.06f, textPaint);

        subTextPaint.setTextSize(size * 0.09f);
        canvas.drawText(subLabel, w / 2f, h / 2f + size * 0.20f, subTextPaint);
    }
}
