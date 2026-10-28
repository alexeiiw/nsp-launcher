package com.nsp.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Los cuatro segmentos verticales de CASE. */
public class CaseView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int onColor = 0xFF1BA1E2;
    private int offColor = 0x33FFFFFF;
    private int lit = 4;

    public CaseView(Context context) {
        super(context);
    }

    public CaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setColors(int on, int off) {
        onColor = on;
        offColor = off;
        invalidate();
    }

    public void setLit(int segments) {
        lit = Math.max(0, Math.min(4, segments));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float gap = getWidth() * 0.14f;
        float bar = (getWidth() - gap * 3f) / 4f;
        float top = getHeight() * 0.06f;
        float height = getHeight() * 0.88f;
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 4; i++) {
            paint.setColor(i < lit ? onColor : offColor);
            float left = i * (bar + gap);
            canvas.drawRect(left, top, left + bar, top + height, paint);
        }
    }
}
