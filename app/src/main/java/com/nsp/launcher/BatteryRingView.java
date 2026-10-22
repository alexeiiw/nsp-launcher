package com.nsp.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

public class BatteryRingView extends View {

    private static final int COLOR_HIGH = 0xFF60A917;
    private static final int COLOR_MID = 0xFFF0A30A;
    private static final int COLOR_LOW = 0xFFE51400;
    private static final int COLOR_CHARGING = 0xFF1BA1E2;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progress = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int percent = 0;
    private boolean charging;
    private int trackColor = 0xFF2C2C2C;
    private int textColor = 0xFFFFFFFF;

    public BatteryRingView(Context context) {
        super(context);
        track.setStyle(Paint.Style.STROKE);
        progress.setStyle(Paint.Style.STROKE);
        progress.setStrokeCap(Paint.Cap.ROUND);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
    }

    public void setColors(int track, int text) {
        trackColor = track;
        textColor = text;
        invalidate();
    }

    public void setLevel(int value) {
        int clamped = value < 0 ? 0 : (value > 100 ? 100 : value);
        if (clamped == percent) return;
        percent = clamped;
        invalidate();
    }

    public void setCharging(boolean value) {
        if (value == charging) return;
        charging = value;
        invalidate();
    }

    private int progressColor() {
        if (charging) return COLOR_CHARGING;
        if (percent <= 15) return COLOR_LOW;
        if (percent <= 40) return COLOR_MID;
        return COLOR_HIGH;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float size = Math.min(width, height);
        if (size <= 0f) return;

        float cx = width / 2f;
        float cy = height / 2f;
        float stroke = size * 0.10f;
        float radius = size / 2f - stroke / 2f - dp(1f);
        if (radius <= 0f) return;

        RectF rect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        track.setColor(trackColor);
        track.setStrokeWidth(stroke);
        canvas.drawArc(rect, 135f, 270f, false, track);

        progress.setColor(progressColor());
        progress.setStrokeWidth(stroke);
        canvas.drawArc(rect, 135f, 270f * percent / 100f, false, progress);

        label.setColor(textColor);
        label.setTextSize(radius * 0.78f);
        Paint.FontMetrics metrics = label.getFontMetrics();
        float baseline = cy - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(String.valueOf(percent), cx, baseline, label);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }
}
