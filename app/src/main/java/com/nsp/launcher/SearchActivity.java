package com.nsp.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;

/** Overlay de busqueda: teclado virtual y apertura en Chrome o Firefox. */
public class SearchActivity extends Activity {

    private EditText input;
    private LinearLayout historyList;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("nsp", MODE_PRIVATE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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
        root.setPadding(dp(28), dp(18), dp(28), dp(12));

        TextView title = new TextView(this);
        title.setText("BUSCAR EN GOOGLE");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-light",
                android.graphics.Typeface.NORMAL));
        title.setLetterSpacing(0.06f);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("A buscar   |   B cerrar   |   teclado en pantalla");
        hint.setTextSize(9);
        hint.setTextColor(0xFF777777);
        hint.setPadding(0, dp(2), 0, dp(10));
        root.addView(hint);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(EditorInfo.IME_ACTION_GO
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        input.setHint("Escribe una búsqueda…");
        input.setHintTextColor(0xFF777777);
        input.setTextColor(Color.WHITE);
        input.setTextSize(18);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setShowSoftInputOnFocus(true);
        GradientDrawable box = new GradientDrawable();
        box.setShape(GradientDrawable.RECTANGLE);
        box.setColor(0xFF141414);
        box.setStroke(dp(2), 0xFF1BA1E2);
        input.setBackgroundDrawable(box);
        root.addView(input, new LinearLayout.LayoutParams(-1, -2));

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search();
                    return true;
                }
                return false;
            }
        });

        TextView recentLabel = new TextView(this);
        recentLabel.setText("BÚSQUEDAS RECIENTES");
        recentLabel.setTextSize(9);
        recentLabel.setTextColor(0xFF9E9E9E);
        recentLabel.setLetterSpacing(0.18f);
        recentLabel.setPadding(0, dp(18), 0, dp(6));
        root.addView(recentLabel);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(historyList, new LinearLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
        buildHistory();
        input.postDelayed(new Runnable() {
            public void run() {
                input.requestFocus();
                showKeyboard();
            }
        }, 250);
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private void showKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(input, InputMethodManager.SHOW_FORCED);
        } catch (Exception ignored) {
        }
    }

    private ArrayList<String> history() {
        ArrayList<String> list = new ArrayList<String>();
        try {
            JSONArray array = new JSONArray(prefs.getString("search_history", "[]"));
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "");
                if (!value.isEmpty()) list.add(value);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private void addHistory(String query) {
        ArrayList<String> list = history();
        list.remove(query);
        list.add(0, query);
        while (list.size() > 5) list.remove(list.size() - 1);
        JSONArray array = new JSONArray();
        for (String value : list) array.put(value);
        prefs.edit().putString("search_history", array.toString()).apply();
    }

    private void buildHistory() {
        historyList.removeAllViews();
        ArrayList<String> items = history();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Sin búsquedas todavía");
            empty.setTextSize(12);
            empty.setTextColor(0xFF777777);
            empty.setPadding(dp(10), dp(8), dp(10), dp(8));
            historyList.addView(empty);
            return;
        }
        for (final String item : items) {
            final TextView row = new TextView(this);
            row.setText(item);
            row.setTextSize(14);
            row.setTextColor(Color.WHITE);
            row.setPadding(dp(10), dp(11), dp(10), dp(11));
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);
            row.setClickable(true);
            row.setSingleLine(true);
            row.setEllipsize(TextUtils.TruncateAt.END);

            final GradientDrawable normal = new GradientDrawable();
            normal.setColor(0x00000000);
            final GradientDrawable focused = new GradientDrawable();
            focused.setColor(0x331BA1E2);
            focused.setStroke(dp(2), 0xFF1BA1E2);
            row.setBackgroundDrawable(normal);
            row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean hasFocus) {
                    v.setBackgroundDrawable(hasFocus ? focused : normal);
                }
            });
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    input.setText(item);
                    input.setSelection(item.length());
                    search();
                }
            });
            historyList.addView(row);
        }
    }

    private void search() {
        String query = input.getText().toString().trim();
        if (query.isEmpty()) {
            toast("Escribe una búsqueda");
            input.requestFocus();
            showKeyboard();
            return;
        }
        addHistory(query);
        if (!BrowserUtil.open(this, BrowserUtil.searchUrl(query))) {
            toast("No hay navegador instalado");
            return;
        }
        finish();
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
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    View focused = getCurrentFocus();
                    if (focused != null && focused != input && focused.isClickable()) {
                        focused.performClick();
                    } else {
                        search();
                    }
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
