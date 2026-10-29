package com.nsp.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Panel del modulo CASE: muestra el reporte completo del endpoint. */
public class CaseActivity extends Activity {

    private CaseView mark;
    private TextView stamp;
    private TextView status;
    private TextView body;
    private TextView footer;
    private ScrollView scroll;
    private boolean loading;
    private String shownText;

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
        root.setPadding(dp(32), dp(20), dp(32), dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        mark = new CaseView(this);
        mark.setColors(0xFF1BA1E2, 0x33FFFFFF);
        header.addView(mark, new LinearLayout.LayoutParams(dp(54), dp(80)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titlesParams = new LinearLayout.LayoutParams(0, -2, 1f);
        titlesParams.leftMargin = dp(22);
        header.addView(titles, titlesParams);

        TextView title = new TextView(this);
        title.setText("CASE");
        title.setTextSize(32);
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

        stamp = new TextView(this);
        stamp.setTextSize(9);
        stamp.setTextColor(0xFF9E9E9E);
        stamp.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(stamp, new LinearLayout.LayoutParams(-2, -2));

        root.addView(header);

        status = new TextView(this);
        status.setTextSize(12);
        status.setTextColor(0xFF7FD3FF);
        status.setPadding(0, dp(12), 0, dp(6));
        root.addView(status);

        View divider = new View(this);
        divider.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.bottomMargin = dp(10);
        root.addView(divider, dividerParams);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFocusable(true);

        body = new TextView(this);
        body.setTextSize(15);
        body.setTextColor(0xFFEDEDED);
        body.setLineSpacing(dp(4), 1f);
        body.setTextIsSelectable(false);
        scroll.addView(body, new LinearLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        footer = new TextView(this);
        footer.setTextSize(9);
        footer.setTextColor(0xFF777777);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, 0);
        root.addView(footer);

        setContentView(root);
        render();
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private CaseService.Callback callback() {
        return new CaseService.Callback() {
            public void onResult(String text, String error) {
                if (isFinishing()) return;
                loading = false;
                render();
            }
        };
    }

    private void render() {
        boolean configured = CaseService.isConfigured(this);
        String text = CaseService.cachedText(this);
        if (text == null) text = "";

        if (text.length() > 0) {
            setBody(text);
        } else if (configured) {
            setBody("Sin reporte todavía.\n\nA consulta el script.");
        } else {
            setBody("Falta configurar CASE en Ajustes.\n\n"
                    + "Pega el bloque con la URL y el token, o importa case.txt.\n\n"
                    + "A abre Ajustes.");
        }

        stamp.setText(CaseService.stamp(this));

        String cachedError = CaseService.cachedError(this);
        if (!configured) {
            status.setText("CASE sin configurar");
        } else if (loading) {
            status.setText("Consultando… puede tardar unos 20 s");
        } else if (cachedError != null && cachedError.length() > 0) {
            status.setText("No se pudo actualizar: " + cachedError
                    + " · " + CaseService.attemptStamp(this));
        } else if (CaseService.isDue(this) && CaseService.lastTime(this) > 0L) {
            status.setText("Sin actualizar desde "
                    + CaseService.shortStamp(CaseService.lastTime(this)));
        } else {
            status.setText("");
        }
        status.setVisibility(status.getText().length() == 0 ? View.GONE : View.VISIBLE);

        int level = CaseService.indicator(this);
        mark.setLit(level);
        mark.setColors(level <= 1 && level > 0 ? 0xFFFF5252 : 0xFF1BA1E2, 0x33FFFFFF);

        updateFooter(configured);
        if (!scroll.hasFocus()) scroll.requestFocus();
    }

    /** Cambia el texto solo si es distinto y vuelve arriba sin perder la lectura al reabrir. */
    private void setBody(String text) {
        if (text.equals(shownText)) return;
        shownText = text;
        body.setText(text);
        scroll.post(new Runnable() {
            public void run() {
                scroll.scrollTo(0, 0);
            }
        });
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        scroll.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        scroll.scrollTo(0, 0);
                    }
                });
    }

    private void updateFooter(boolean configured) {
        String link = CaseService.firstLink(CaseService.cachedText(this));
        StringBuilder hint = new StringBuilder();
        if (!configured) {
            hint.append("A abrir Ajustes");
        } else if (loading) {
            hint.append("Consultando…");
        } else {
            hint.append("A actualizar");
            if (link != null) hint.append("  |  X abrir enlace");
        }
        hint.append("  |  ").append(loading ? "B cancelar" : "B volver");
        footer.setText(hint.toString());
    }

    private void refresh() {
        if (loading) return;
        if (!CaseService.isConfigured(this)) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        loading = true;
        updateFooter(true);
        status.setText("Consultando… puede tardar unos 20 s");
        CaseService.refresh(this, callback());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loading) return;
        render();
        if (CaseService.refreshIfDue(this, callback())) {
            loading = true;
            status.setText("Consultando… puede tardar unos 20 s");
            updateFooter(true);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_BUTTON_A
                    || code == KeyEvent.KEYCODE_DPAD_CENTER
                    || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (event.getRepeatCount() == 0) refresh();
                return true;
            }
            if (code == KeyEvent.KEYCODE_BUTTON_X) {
                if (event.getRepeatCount() == 0) {
                    String link = CaseService.firstLink(CaseService.cachedText(this));
                    if (link != null) BrowserUtil.open(this, link);
                }
                return true;
            }
            if (code == KeyEvent.KEYCODE_BUTTON_B) {
                if (event.getRepeatCount() == 0) finish();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
