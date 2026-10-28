package com.nsp.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NewsActivity extends Activity {

    private LinearLayout list;
    private TextView title;
    private TextView tabs;
    private TextView status;
    private int cat = 0;
    private boolean loading;
    private boolean dialogOpen;
    private ArrayList<RssItem> items = new ArrayList<RssItem>();

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
        root.setPadding(dp(24), dp(12), dp(24), dp(8));

        title = new TextView(this);
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setSingleLine(true);
        root.addView(title);

        tabs = new TextView(this);
        tabs.setTextSize(10);
        tabs.setTextColor(0xFF9E9E9E);
        tabs.setSingleLine(true);
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(-1, -2);
        tabsParams.topMargin = dp(2);
        root.addView(tabs, tabsParams);

        status = new TextView(this);
        status.setTextSize(9);
        status.setTextColor(0xFF7FD3FF);
        status.setSingleLine(true);
        status.setPadding(0, dp(2), 0, dp(4));
        root.addView(status);

        TextView hint = new TextView(this);
        hint.setText("← → categoría  |  ↑ ↓ titular  |  A detalle  |  B volver");
        hint.setTextSize(9);
        hint.setTextColor(0xFF777777);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.bottomMargin = dp(4);
        root.addView(hint, hintParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new LinearLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
        load();
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private void load() {
        if (loading) return;
        loading = true;
        title.setText("NOTICIAS · " + RssService.NAMES[cat].toUpperCase(new Locale("es", "ES")));
        updateTabs();
        status.setText("Cargando…");
        status.setTextColor(0xFF9E9E9E);
        list.removeAllViews();

        RssService.load(this, cat, new RssService.Callback() {
            public void done(ArrayList<RssItem> result, String error) {
                loading = false;
                items = result == null ? new ArrayList<RssItem>() : result;
                if (items.isEmpty()) {
                    showEmpty(error);
                } else {
                    render(error);
                }
            }
        });
    }

    private void updateTabs() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < RssService.NAMES.length; i++) {
            if (i > 0) builder.append("   ·   ");
            if (i == cat) builder.append("[" + RssService.NAMES[i] + "]");
            else builder.append(RssService.NAMES[i]);
        }
        tabs.setText(builder.toString());
    }

    private void showEmpty(String error) {
        list.removeAllViews();
        status.setText(error == null ? "Sin noticias" : "Sin conexión · " + error);
        status.setTextColor(0xFFFF8A80);
        TextView retry = new TextView(this);
        retry.setText("A · reintentar\nB · volver");
        retry.setTextSize(14);
        retry.setTextColor(0xFFFFFFFF);
        retry.setPadding(dp(12), dp(16), dp(12), dp(16));
        retry.setFocusable(true);
        retry.setFocusableInTouchMode(true);
        retry.setClickable(true);
        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                load();
            }
        });
        list.addView(retry);
        retry.requestFocus();
    }

    private void render(String error) {
        list.removeAllViews();
        status.setTextColor(error == null ? 0xFF7FD3FF : 0xFFFFB74D);
        status.setText(error == null
                ? items.size() + " titulares · " + RssService.NAMES[cat]
                : "Mostrando caché · " + error);

        for (int i = 0; i < items.size(); i++) {
            list.addView(makeRow(items.get(i), i));
        }
        if (!items.isEmpty()) {
            final View first = list.getChildAt(0);
            first.post(new Runnable() {
                public void run() {
                    first.requestFocus();
                }
            });
        }
    }

    private View makeRow(final RssItem item, final int index) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(9), dp(12), dp(9));
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setClickable(true);

        final GradientDrawable normal = new GradientDrawable();
        normal.setColor(0x00000000);
        final GradientDrawable focused = new GradientDrawable();
        focused.setColor(0x331BA1E2);
        focused.setStroke(dp(2), 0xFF1BA1E2);
        row.setBackgroundDrawable(normal);

        TextView headline = new TextView(this);
        headline.setText(item.title);
        headline.setTextSize(15);
        headline.setTextColor(Color.WHITE);
        headline.setMaxLines(2);
        headline.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(headline);

        TextView meta = new TextView(this);
        meta.setText(item.source + " · " + age(item.published));
        meta.setTextSize(9);
        meta.setTextColor(0xFF9E9E9E);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2);
        metaParams.topMargin = dp(2);
        row.addView(meta, metaParams);

        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundDrawable(hasFocus ? focused : normal);
                if (hasFocus) v.animate().scaleX(1.01f).scaleY(1.01f).setDuration(90).start();
                else v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                detail(item);
            }
        });
        row.setTag(index);
        return row;
    }

    private String age(long published) {
        if (published <= 0L) return "ahora";
        long delta = System.currentTimeMillis() - published;
        if (delta < 60L * 1000L) return "ahora";
        long minutes = delta / (60L * 1000L);
        if (minutes < 60L) return "hace " + minutes + " min";
        long hours = minutes / 60L;
        if (hours < 24L) return "hace " + hours + " h";
        long days = hours / 24L;
        if (days < 7L) return "hace " + days + " d";
        return new java.text.SimpleDateFormat("d MMM", new Locale("es", "ES"))
                .format(new Date(published));
    }

    private void openSelected() {
        if (items.isEmpty()) {
            load();
            return;
        }
        View focused = list.getFocusedChild();
        int index = 0;
        if (focused != null && focused.getTag() instanceof Integer) {
            index = (Integer) focused.getTag();
        }
        if (index < 0 || index >= items.size()) index = 0;
        detail(items.get(index));
    }

    private void detail(final RssItem item) {
        StringBuilder body = new StringBuilder();
        if (item.description != null && !item.description.isEmpty()) {
            body.append(item.description);
        } else {
            body.append("Sin resumen en la fuente.");
        }
        if (item.published > 0L) {
            body.append("\n\n").append(new java.text.SimpleDateFormat(
                    "d MMM · HH:mm", new Locale("es", "ES")).format(new Date(item.published)));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.title);
        builder.setMessage(body.toString());
        builder.setPositiveButton("Abrir en navegador", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (!BrowserUtil.open(NewsActivity.this, item.link)) {
                    toast("No hay navegador");
                }
            }
        });
        builder.setNegativeButton("Volver", null);

        final AlertDialog dialog = builder.create();
        dialogOpen = true;
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                dialogOpen = false;
            }
        });
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                    d.dismiss();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (dialogOpen) return super.dispatchKeyEvent(event);
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BUTTON_B:
                    finish();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (loading) return true;
                    cat = (cat + RssService.NAMES.length - 1) % RssService.NAMES.length;
                    load();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (loading) return true;
                    cat = (cat + 1) % RssService.NAMES.length;
                    load();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_A:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    openSelected();
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
