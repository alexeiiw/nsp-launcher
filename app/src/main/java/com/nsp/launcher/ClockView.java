package com.nsp.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;

import java.util.Calendar;

public class ClockView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean running;
    private boolean showSeconds = true;
    private int ringColor = 0xFF9E9E9E;
    private int handColor = 0xFFFFFFFF;
    private int accentColor = 0xFF1BA1E2;

    private final Runnable ticker = new Runnable() {
        public void run() {
            invalidate();
            if (running) handler.postDelayed(this, showSeconds ? 1000 : 20000);
        }
    };

    public ClockView(Context context) {
        super(context);
    }

    public void setColors(int ring, int hand, int accent) {
        ringColor = ring;
        handColor = hand;
        accentColor = accent;
        invalidate();
    }

    public void setShowSeconds(boolean show) {
        showSeconds = show;
        if (running) {
            handler.removeCallbacks(ticker);
            handler.post(ticker);
        }
        invalidate();
    }

    public void setRunning(boolean value) {
        running = value;
        handler.removeCallbacks(ticker);
        if (running) handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float size = Math.min(width, height);
        if (size <= 0f) return;

        float cx = width / 2f;
        float cy = height / 2f;
        float radius = size / 2f - dp(3f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(ringColor);
        canvas.drawCircle(cx, cy, radius, paint);

        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30.0 - 90.0);
            float inner = radius - (i % 3 == 0 ? dp(6f) : dp(3f));
            paint.setStrokeWidth(i % 3 == 0 ? dp(1.6f) : dp(1f));
            canvas.drawLine(
                    cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * (radius - dp(1f)),
                    cy + (float) Math.sin(angle) * (radius - dp(1f)),
                    paint);
        }

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY) % 12;
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);

        paint.setStrokeCap(Paint.Cap.ROUND);

        float hourAngle = (hour + minute / 60f) * 30f - 90f;
        paint.setStrokeWidth(dp(2.8f));
        paint.setColor(handColor);
        drawHand(canvas, cx, cy, hourAngle, radius * 0.50f);

        float minuteAngle = (minute + second / 60f) * 6f - 90f;
        paint.setStrokeWidth(dp(1.9f));
        paint.setColor(handColor);
        drawHand(canvas, cx, cy, minuteAngle, radius * 0.72f);

        if (showSeconds) {
            float secondAngle = second * 6f - 90f;
            paint.setStrokeWidth(dp(1f));
            paint.setColor(accentColor);
            drawHand(canvas, cx, cy, secondAngle, radius * 0.80f);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accentColor);
        canvas.drawCircle(cx, cy, dp(2.4f), paint);
    }

    private void drawHand(Canvas canvas, float cx, float cy, float degrees, float length) {
        double angle = Math.toRadians(degrees);
        canvas.drawLine(cx, cy,
                cx + (float) Math.cos(angle) * length,
                cy + (float) Math.sin(angle) * length,
                paint);
    }
}
