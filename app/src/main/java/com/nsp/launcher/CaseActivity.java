package com.nsp.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Overlay de CASE: estado del sistema con la voz del robot. */
public class CaseActivity extends Activity {

    private CaseView mark;
    private TextView voice;
    private TextView versionLine;
    private TextView updateLine;
    private TextView batteryLine;
    private TextView weatherLine;
    private TextView btcLine;
    private TextView newsLine;
    private boolean checking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(32), dp(22), dp(32), dp(16));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        mark = new CaseView(this);
        mark.setColors(0xFF1BA1E2, 0x33FFFFFF);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(54), dp(84));
        header.addView(mark, markParams);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titlesParams = new LinearLayout.LayoutParams(-1, -2);
        titlesParams.leftMargin = dp(22);
        header.addView(titles, titlesParams);

        TextView title = new TextView(this);
        title.setText("CASE");
        title.setTextSize(34);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setLetterSpacing(0.12f);
        titles.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("UNIDAD DE SOPORTE · NSP");
        subtitle.setTextSize(9);
        subtitle.setTextColor(0xFF9E9E9E);
        subtitle.setLetterSpacing(0.22f);
        titles.addView(subtitle);

        root.addView(header);

        voice = new TextView(this);
        voice.setTextSize(15);
        voice.setTextColor(0xFF7FD3FF);
        voice.setPadding(0, dp(18), 0, dp(14));
        root.addView(voice);

        View divider = new View(this);
        divider.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.bottomMargin = dp(12);
        root.addView(divider, dividerParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(status, new LinearLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        versionLine = addLine(status, "NSP");
        updateLine = addLine(status, "ACTUALIZACIÓN");
        batteryLine = addLine(status, "BATERÍA");
        weatherLine = addLine(status, "CLIMA");
        btcLine = addLine(status, "BTC");
        newsLine = addLine(status, "NOTICIAS");

        TextView hint = new TextView(this);
        hint.setText("A comprobar actualización   |   B volver");
        hint.setTextSize(9);
        hint.setTextColor(0xFF777777);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(8), 0, 0);
        root.addView(hint);

        setContentView(root);
        refresh();
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private TextView addLine(LinearLayout parent, String label) {
        TextView line = new TextView(this);
        line.setTextSize(12);
        line.setTextColor(0xFFE0E0E0);
        line.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        line.setPadding(0, dp(3), 0, dp(3));
        line.setTag(label);
        parent.addView(line);
        return line;
    }

    private boolean voiceEnabled() {
        return getSharedPreferences("nsp", MODE_PRIVATE).getBoolean("case_voice", true);
    }

    private void refresh() {
        int level = 0;
        boolean charging = false;
        try {
            Intent battery = registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int raw = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                level = raw >= 0 && scale > 0 ? Math.round(raw * 100f / scale) : 0;
                charging = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            }
        } catch (Exception ignored) {
        }

        versionLine.setText("NSP        " + UpdateService.installedVersion(this)
                + "  ·  código " + UpdateService.installedCode(this));
        updateLine.setText("ACTUALIZ.  " + UpdateService.statusText(this));
        batteryLine.setText("BATERÍA    " + level + " %"
                + (charging ? "  ·  cargando" : ""));

        WeatherData weather = WeatherService.loadCached(this);
        if (weather == null) {
            weatherLine.setText("CLIMA      sin datos");
        } else {
            weatherLine.setText("CLIMA      " + Math.round(weather.temperature) + "°  "
                    + WeatherService.label(weather.code));
        }

        BtcData btc = BtcService.loadCached(this);
        if (btc == null) {
            btcLine.setText("BTC        sin datos");
        } else {
            btcLine.setText("BTC        " + BtcService.format(btc.usd)
                    + "  ·  " + age(btc.updated));
        }

        ArrayList<RssItem> news = RssService.peek(0);
        newsLine.setText("NOTICIAS   " + (news == null ? "sin datos" : news.size() + " titulares"));

        int lit = charging ? 4 : (level >= 50 ? 4 : level >= 20 ? 3 : level >= 10 ? 2 : 1);
        mark.setLit(lit);

        if (!voiceEnabled()) {
            voice.setText("Estado del sistema");
            return;
        }
        if (!charging && level > 0 && level < 15) {
            voice.setText("CASE · Reserva crítica. Conecta la consola.");
        } else if (charging) {
            voice.setText("CASE · Recargando. Sistemas nominales.");
        } else {
            voice.setText("CASE · Sistemas nominales. Todo en orden.");
        }
    }

    private String age(long updated) {
        long delta = System.currentTimeMillis() - updated;
        if (delta < 60L * 1000L) return "ahora";
        long minutes = delta / (60L * 1000L);
        if (minutes < 60L) return "hace " + minutes + " min";
        return "hace " + (minutes / 60L) + " h";
    }

    private void checkUpdate() {
        if (checking) return;
        checking = true;
        updateLine.setText("ACTUALIZ.  comprobando…");
        UpdateService.check(this, true, new UpdateService.CheckCallback() {
            public void onResult(UpdateService.Release release, String error) {
                checking = false;
                refresh();
                if (release == null) {
                    toast(error == null ? "NSP está al día" : "No se pudo comprobar: " + error);
                }
            }
        });
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BUTTON_B:
                    finish();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_A:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    checkUpdate();
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
