package com.nsp.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class WeatherIconView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int code = 0;
    private int color = 0xFFFFFFFF;

    public WeatherIconView(Context context) {
        super(context);
    }

    public void setCode(int code) {
        this.code = code;
        invalidate();
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    private boolean isSun(int c) {
        return c == 0;
    }

    private boolean isPartly(int c) {
        return c == 1 || c == 2;
    }

    private boolean isRain(int c) {
        return (c >= 51 && c <= 67) || (c >= 80 && c <= 82);
    }

    private boolean isSnow(int c) {
        return (c >= 71 && c <= 77) || c == 85 || c == 86;
    }

    private boolean isStorm(int c) {
        return c >= 95;
    }

    private boolean isFog(int c) {
        return c == 45 || c == 48;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float size = Math.min(width, height);
        if (size <= 0f) return;

        float cx = width / 2f;
        float cy = height / 2f;

        paint.setColor(color);

        if (isSun(code)) {
            drawSun(canvas, cx, cy, size * 0.24f);
        } else if (isPartly(code)) {
            drawSun(canvas, cx - size * 0.10f, cy - size * 0.12f, size * 0.17f);
            drawCloud(canvas, cx + size * 0.04f, cy + size * 0.06f, size * 0.60f);
        } else if (isStorm(code)) {
            drawCloud(canvas, cx, cy - size * 0.06f, size * 0.62f);
            drawBolt(canvas, cx, cy + size * 0.10f, size * 0.26f);
        } else if (isRain(code)) {
            drawCloud(canvas, cx, cy - size * 0.06f, size * 0.62f);
            drawRain(canvas, cx, cy + size * 0.12f, size * 0.30f);
        } else if (isSnow(code)) {
            drawCloud(canvas, cx, cy - size * 0.06f, size * 0.62f);
            drawSnow(canvas, cx, cy + size * 0.14f, size * 0.30f);
        } else if (isFog(code)) {
            drawCloud(canvas, cx, cy - size * 0.08f, size * 0.60f);
            paint.setStrokeWidth(size * 0.035f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(cx - size * 0.18f, cy + size * 0.16f, cx + size * 0.18f, cy + size * 0.16f, paint);
            canvas.drawLine(cx - size * 0.12f, cy + size * 0.24f, cx + size * 0.12f, cy + size * 0.24f, paint);
            paint.setStyle(Paint.Style.FILL);
        } else {
            drawCloud(canvas, cx, cy, size * 0.68f);
        }
    }

    private void drawSun(Canvas canvas, float x, float y, float radius) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, radius * 0.62f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.20f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            float inner = radius * 0.85f;
            float outer = radius * 1.20f;
            canvas.drawLine(
                    x + (float) Math.cos(angle) * inner,
                    y + (float) Math.sin(angle) * inner,
                    x + (float) Math.cos(angle) * outer,
                    y + (float) Math.sin(angle) * outer,
                    paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCloud(Canvas canvas, float x, float y, float width) {
        float r = width * 0.20f;
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x - width * 0.20f, y + r * 0.35f, r, paint);
        canvas.drawCircle(x, y - r * 0.35f, r * 1.15f, paint);
        canvas.drawCircle(x + width * 0.22f, y + r * 0.30f, r * 0.95f, paint);
        canvas.drawRect(x - width * 0.20f, y + r * 0.30f, x + width * 0.24f, y + r * 1.25f, paint);
    }

    private void drawRain(Canvas canvas, float x, float y, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width * 0.10f);
        for (int i = -1; i <= 1; i++) {
            float sx = x + i * width * 0.30f;
            canvas.drawLine(sx + width * 0.10f, y, sx - width * 0.05f, y + width * 0.34f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSnow(Canvas canvas, float x, float y, float width) {
        paint.setStyle(Paint.Style.FILL);
        float r = width * 0.07f;
        for (int i = -1; i <= 1; i++) {
            float sx = x + i * width * 0.30f;
            canvas.drawCircle(sx, y + width * 0.15f, r, paint);
        }
    }

    private void drawBolt(Canvas canvas, float x, float y, float size) {
        Path path = new Path();
        path.moveTo(x + size * 0.10f, y);
        path.lineTo(x - size * 0.24f, y + size * 0.48f);
        path.lineTo(x - size * 0.02f, y + size * 0.48f);
        path.lineTo(x - size * 0.12f, y + size * 0.92f);
        path.lineTo(x + size * 0.28f, y + size * 0.34f);
        path.lineTo(x + size * 0.06f, y + size * 0.34f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }
}
