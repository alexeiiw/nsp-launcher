package com.nsp.launcher;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Dashboard extends LinearLayout {

    private final Context ctx;
    private final boolean light;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Locale locale = new Locale("es", "ES");
    private final SimpleDateFormat weekdayFormat = new SimpleDateFormat("EEEE", locale);
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", locale);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", locale);

    private final TextView weekdayView;
    private final TextView dayView;
    private final TextView monthView;
    private final BatteryRingView ring;
    private final TextView batteryStatus;
    private final TextView cityView;
    private final TextView tempView;
    private final TextView conditionView;
    private final TextView updatedView;
    private final WeatherIconView weatherIcon;
    private final MemRow ramRow;
    private final MemRow internalRow;
    private final MemRow sdRow;

    private boolean running;
    private int tick;
    private WeatherData weather;

    private class MemRow {
        LinearLayout container;
        TextView value;
        Bar bar;
    }

    private final Runnable ticker = new Runnable() {
        public void run() {
            updateDate();
            tick++;
            if (tick % 3 == 0) updateMemory();
            if (running) handler.postDelayed(this, 10000);
        }
    };

    public Dashboard(Context context, boolean light, boolean showSeconds, final Runnable refreshWeather) {
        super(context);
        this.ctx = context;
        this.light = light;
        setOrientation(HORIZONTAL);

        int accent = light ? 0xFF0050EF : 0xFF1BA1E2;

        LinearLayout dateCard = newCard(1.12f, surface());
        weekdayView = new TextView(ctx);
        weekdayView.setTextSize(9);
        weekdayView.setTextColor(accent);
        weekdayView.setLetterSpacing(0.12f);
        weekdayView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        dateCard.addView(weekdayView, new LayoutParams(-1, -2));

        dayView = new TextView(ctx);
        dayView.setTextSize(32);
        dayView.setTextColor(textColor());
        dayView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        dateCard.addView(dayView, new LayoutParams(-1, -2));

        monthView = new TextView(ctx);
        monthView.setTextSize(9);
        monthView.setTextColor(secondaryColor());
        monthView.setLetterSpacing(0.12f);
        dateCard.addView(monthView, new LayoutParams(-1, -2));
        addView(dateCard);

        LinearLayout batteryCard = newCard(0.95f, surface());
        batteryCard.setGravity(Gravity.CENTER);
        ring = new BatteryRingView(ctx);
        ring.setColors(light ? 0xFFD2D2D2 : 0xFF2C2C2C, textColor());
        LayoutParams ringParams = new LayoutParams(dp(56), dp(56));
        ringParams.gravity = Gravity.CENTER_HORIZONTAL;
        batteryCard.addView(ring, ringParams);

        batteryStatus = new TextView(ctx);
        batteryStatus.setTextSize(8);
        batteryStatus.setTextColor(secondaryColor());
        batteryStatus.setGravity(Gravity.CENTER);
        batteryStatus.setSingleLine(true);
        LayoutParams batteryParams = new LayoutParams(-1, -2);
        batteryParams.topMargin = dp(2);
        batteryCard.addView(batteryStatus, batteryParams);
        addView(batteryCard);

        LinearLayout weatherCard = newCard(1.38f, 0xFF0050EF);
        weatherCard.setClickable(true);
        weatherCard.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (refreshWeather != null) refreshWeather.run();
            }
        });

        cityView = new TextView(ctx);
        cityView.setText(WeatherService.CITY.toUpperCase(locale));
        cityView.setTextSize(7);
        cityView.setTextColor(0xCCFFFFFF);
        cityView.setLetterSpacing(0.12f);
        weatherCard.addView(cityView, new LayoutParams(-1, -2));

        LinearLayout tempRow = new LinearLayout(ctx);
        tempRow.setOrientation(HORIZONTAL);
        tempRow.setGravity(Gravity.CENTER_VERTICAL);

        tempView = new TextView(ctx);
        tempView.setTextSize(23);
        tempView.setTextColor(0xFFFFFFFF);
        tempView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        tempRow.addView(tempView, new LayoutParams(0, -2, 1f));

        weatherIcon = new WeatherIconView(ctx);
        weatherIcon.setColor(0xFFFFFFFF);
        LayoutParams iconParams = new LayoutParams(dp(30), dp(30));
        tempRow.addView(weatherIcon, iconParams);
        weatherCard.addView(tempRow, new LayoutParams(-1, -2));

        conditionView = new TextView(ctx);
        conditionView.setTextSize(8);
        conditionView.setTextColor(0xFFFFFFFF);
        conditionView.setMaxLines(2);
        conditionView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        weatherCard.addView(conditionView, new LayoutParams(-1, -2));

        updatedView = new TextView(ctx);
        updatedView.setTextSize(7);
        updatedView.setTextColor(0x99FFFFFF);
        weatherCard.addView(updatedView, new LayoutParams(-1, -2));
        addView(weatherCard);

        LinearLayout memoryCard = newCard(1.62f, surface());
        TextView memTitle = new TextView(ctx);
        memTitle.setText("MEMORIA");
        memTitle.setTextSize(7);
        memTitle.setTextColor(secondaryColor());
        memTitle.setLetterSpacing(0.15f);
        memTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        memoryCard.addView(memTitle, new LayoutParams(-1, -2));

        ramRow = addMemRow(memoryCard, "RAM", 0xFF1BA1E2);
        internalRow = addMemRow(memoryCard, "INTERNA", 0xFF60A917);
        sdRow = addMemRow(memoryCard, "SD", 0xFFF0A30A);
        addView(memoryCard);

        LayoutParams lastParams = (LayoutParams) memoryCard.getLayoutParams();
        lastParams.rightMargin = 0;
        memoryCard.setLayoutParams(lastParams);
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private int surface() {
        return light ? 0xFFEDEDED : 0xFF141414;
    }

    private int textColor() {
        return light ? 0xFF111111 : 0xFFFFFFFF;
    }

    private int secondaryColor() {
        return light ? 0xFF666666 : 0xFF9E9E9E;
    }

    private int barBackground() {
        return light ? 0xFFD2D2D2 : 0xFF2C2C2C;
    }

    private GradientDrawable flat(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        return drawable;
    }

    private LinearLayout newCard(float weight, int color) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(VERTICAL);
        card.setBackgroundDrawable(flat(color));
        card.setPadding(dp(8), dp(7), dp(8), dp(7));
        LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
        params.rightMargin = dp(6);
        card.setLayoutParams(params);
        return card;
    }

    private MemRow addMemRow(LinearLayout parent, String label, int color) {
        MemRow row = new MemRow();
        row.container = new LinearLayout(ctx);
        row.container.setOrientation(VERTICAL);

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(ctx);
        name.setText(label);
        name.setTextSize(8);
        name.setTextColor(secondaryColor());
        name.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        name.setLetterSpacing(0.08f);
        top.addView(name, new LayoutParams(0, -2, 1f));

        row.value = new TextView(ctx);
        row.value.setTextSize(8);
        row.value.setTextColor(textColor());
        row.value.setGravity(Gravity.RIGHT);
        top.addView(row.value, new LayoutParams(-2, -2));

        row.bar = new Bar(ctx);
        row.bar.setColors(barBackground(), color);
        LayoutParams barParams = new LayoutParams(-1, dp(3));
        barParams.topMargin = dp(2);

        row.container.addView(top, new LayoutParams(-1, -2));
        row.container.addView(row.bar, barParams);

        LayoutParams params = new LayoutParams(-1, -2);
        params.topMargin = dp(3);
        parent.addView(row.container, params);
        return row;
    }

    public void start() {
        running = true;
        handler.removeCallbacks(ticker);
        tick = 0;
        updateDate();
        updateMemory();
        handler.postDelayed(ticker, 10000);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(ticker);
    }

    public void refreshNow() {
        updateDate();
        updateMemory();
    }

    public void setBattery(int percent, boolean charging, String status) {
        ring.setLevel(percent);
        ring.setCharging(charging);
        batteryStatus.setText(status == null ? "" : status);
    }

    public void setWeather(WeatherData data) {
        weather = data;
        if (data == null) {
            tempView.setText("—");
            conditionView.setText("Sin datos");
            updatedView.setText("");
            weatherIcon.setCode(3);
            return;
        }
        tempView.setText(Math.round(data.temperature) + "°");
        conditionView.setText(WeatherService.label(data.code));
        updatedView.setText("ACT " + timeFormat.format(new Date(data.updated)));
        weatherIcon.setCode(data.code);
    }

    public void setWeatherLoading() {
        if (weather == null) {
            tempView.setText("—");
            conditionView.setText("Actualizando...");
            updatedView.setText("");
            weatherIcon.setCode(3);
        }
    }

    public void setWeatherError() {
        if (weather == null) {
            tempView.setText("—");
            conditionView.setText("Sin conexión");
            updatedView.setText("");
            weatherIcon.setCode(3);
        }
    }

    private void updateDate() {
        Calendar now = Calendar.getInstance();
        dayView.setText(String.valueOf(now.get(Calendar.DAY_OF_MONTH)));
        weekdayView.setText(weekdayFormat.format(now.getTime()).toUpperCase(locale));
        monthView.setText(monthFormat.format(now.getTime()).toUpperCase(locale));
    }

    private void updateMemory() {
        try {
            ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(info);
            setMem(ramRow, info.totalMem - info.availMem, info.totalMem);
        } catch (Exception ignored) {
        }

        try {
            StatFs data = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            setMem(internalRow, data.getTotalBytes() - data.getAvailableBytes(), data.getTotalBytes());
        } catch (Exception ignored) {
        }

        File sd = findSdCard();
        if (sd == null) {
            sdRow.container.setVisibility(GONE);
        } else {
            sdRow.container.setVisibility(VISIBLE);
            try {
                StatFs stats = new StatFs(sd.getAbsolutePath());
                setMem(sdRow, stats.getTotalBytes() - stats.getAvailableBytes(), stats.getTotalBytes());
            } catch (Exception e) {
                sdRow.container.setVisibility(GONE);
            }
        }
    }

    private File findSdCard() {
        try {
            File[] dirs = ctx.getExternalFilesDirs(null);
            if (dirs == null) return null;
            for (int i = 1; i < dirs.length; i++) {
                if (dirs[i] != null) return dirs[i];
            }
            if (dirs.length > 0 && dirs[0] != null
                    && !Environment.isExternalStorageEmulated(dirs[0])) {
                return dirs[0];
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void setMem(MemRow row, long used, long total) {
        if (total <= 0L) {
            row.value.setText("—");
            row.bar.setFraction(0f);
            return;
        }
        if (used < 0L) used = 0L;
        if (used > total) used = total;
        row.value.setText(Formatter.formatShortFileSize(ctx, used)
                + " / " + Formatter.formatShortFileSize(ctx, total));
        row.bar.setFraction((float) used / (float) total);
    }

    private static class Bar extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int background = 0xFF2C2C2C;
        private int foreground = 0xFF1BA1E2;
        private float fraction;

        Bar(Context context) {
            super(context);
        }

        void setColors(int bg, int fg) {
            background = bg;
            foreground = fg;
            invalidate();
        }

        void setFraction(float value) {
            fraction = Math.max(0f, Math.min(1f, value));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;
            paint.setColor(background);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(foreground);
            canvas.drawRect(0, 0, width * fraction, height, paint);
        }
    }
}
