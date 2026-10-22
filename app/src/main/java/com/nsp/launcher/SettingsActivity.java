package com.nsp.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends Activity {

    private SharedPreferences prefs;
    private boolean isLight;
    private boolean dialogOpen;
    private boolean fetchingWeather;

    private TextView themeValue;
    private TextView secondsValue;
    private TextView homeValue;
    private TextView weatherStatus;
    private TextView notificationValue;
    private TextView backgroundValue;

    private static final int REQ_PICK_IMAGE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("nsp", MODE_PRIVATE);
        isLight = prefs.getBoolean("light", false);
        setTheme(isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        applyImmersive();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(isLight ? 0xFFFFFFFF : 0xFF000000);
        setContentView(root);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(24), dp(12), dp(24), dp(4));

        TextView title = new TextView(this);
        title.setText("AJUSTES");
        title.setTextSize(26);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setLetterSpacing(0.04f);
        title.setTextColor(isLight ? 0xFF111111 : 0xFFFFFFFF);
        header.addView(title, new LinearLayout.LayoutParams(-2, -2));

        TextView hint = new TextView(this);
        hint.setText("A elegir  |  B volver");
        hint.setTextSize(9);
        hint.setTextColor(isLight ? 0xFF666666 : 0xFF9E9E9E);
        hint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        hint.setEllipsize(android.text.TextUtils.TruncateAt.END); header.addView(hint, new LinearLayout.LayoutParams(0, -2, 1f));

        page.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new LinearLayout.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        addLabel(content, "APARIENCIA");
        final TextView themeRowValue = addRow(content, "Tema", "", new Runnable() {
            public void run() {
                isLight = !isLight;
                prefs.edit().putBoolean("light", isLight).apply();
                if (BackgroundManager.mode(SettingsActivity.this)
                        == BackgroundManager.MODE_CUSTOM) {
                    BackgroundManager.setThemeOverride(SettingsActivity.this,
                            isLight ? BackgroundManager.THEME_LIGHT : BackgroundManager.THEME_DARK);
                }
                prefs.edit().putBoolean("bg_changed", true).apply();
                recreate();
            }
        });
        themeValue = themeRowValue;

        backgroundValue = addRow(content, "Fondo de pantalla", "", new Runnable() {
            public void run() {
                showBackgroundDialog();
            }
        });

        TextView backgroundHint = new TextView(this);
        backgroundHint.setText("Solo se aplica al launcher; no cambia la pantalla de bloqueo.");
        backgroundHint.setTextSize(9);
        backgroundHint.setTextColor(isLight ? 0xFF666666 : 0xFF9E9E9E);
        backgroundHint.setPadding(dp(24), dp(2), dp(24), dp(8));
        content.addView(backgroundHint, new LinearLayout.LayoutParams(-1, -2));

        secondsValue = addRow(content, "Segundero del reloj", "", new Runnable() {
            public void run() {
                boolean show = !prefs.getBoolean("clock_seconds", false);
                prefs.edit().putBoolean("clock_seconds", show).apply();
                updateValues();
            }
        });

        addLabel(content, "CLIMA");
        addRow(content, "Actualizar clima", "Ahora", new Runnable() {
            public void run() {
                refreshWeather();
            }
        });

        weatherStatus = new TextView(this);
        weatherStatus.setTextSize(9);
        weatherStatus.setTextColor(isLight ? 0xFF666666 : 0xFF9E9E9E);
        weatherStatus.setPadding(dp(24), dp(2), dp(24), dp(8));
        content.addView(weatherStatus, new LinearLayout.LayoutParams(-1, -2));

        addLabel(content, "NOTIFICACIONES");
        notificationValue = addRow(content, "Acceso a notificaciones", "", new Runnable() {
            public void run() {
                openNotificationSettings();
            }
        });

        TextView notificationHint = new TextView(this);
        notificationHint.setText("Actívalo para ver el contador de cada app en su mosaico.");
        notificationHint.setTextSize(9);
        notificationHint.setTextColor(isLight ? 0xFF666666 : 0xFF9E9E9E);
        notificationHint.setPadding(dp(24), dp(2), dp(24), dp(8));
        content.addView(notificationHint, new LinearLayout.LayoutParams(-1, -2));

        addLabel(content, "SISTEMA");
        homeValue = addRow(content, "Usar NSP como inicio", "", new Runnable() {
            public void run() {
                showHomeDialog();
            }
        });

        addRow(content, "Ajustes de Inicio del sistema", "Abrir", new Runnable() {
            public void run() {
                openHomeSettings();
            }
        });

        addRow(content, "Recargar aplicaciones", "Ahora", new Runnable() {
            public void run() {
                prefs.edit().putBoolean("reload_apps", true).apply();
                toast("Se recargarán al volver");
            }
        });

        TextView footer = new TextView(this);
        footer.setText("NSP 1.1\nAndroid 5.1 · " + WeatherService.CITY);
        footer.setTextSize(9);
        footer.setTextColor(isLight ? 0xFF888888 : 0xFF777777);
        footer.setPadding(dp(24), dp(20), dp(24), dp(24));
        content.addView(footer, new LinearLayout.LayoutParams(-1, -2));

        updateValues();
        updateWeatherStatus();

        root.post(new Runnable() {
            public void run() {
                View first = themeValue;
                if (first != null) first.requestFocus();
            }
        });
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private GradientDrawable flat(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        return drawable;
    }

    private void addLabel(LinearLayout parent, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(10);
        label.setTextColor(isLight ? 0xFF666666 : 0xFF9E9E9E);
        label.setLetterSpacing(0.2f);
        label.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        label.setPadding(dp(24), dp(14), dp(24), dp(4));
        parent.addView(label, new LinearLayout.LayoutParams(-1, -2));
    }

    private TextView addRow(LinearLayout parent, String title, String value, final Runnable action) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(24), dp(13), dp(24), dp(13));
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setClickable(true);

        final GradientDrawable empty = flat(0x00000000);
        row.setBackgroundDrawable(empty);

        TextView name = new TextView(this);
        name.setText(title);
        name.setTextSize(14);
        name.setTextColor(isLight ? 0xFF111111 : 0xFFFFFFFF);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView current = new TextView(this);
        current.setText(value);
        current.setTextSize(11);
        current.setTextColor(isLight ? 0xFF0050EF : 0xFF7FD3FF);
        current.setGravity(Gravity.RIGHT);
        row.addView(current, new LinearLayout.LayoutParams(-2, -2));

        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    GradientDrawable focus = flat(isLight ? 0xFFE4E4E4 : 0xFF1B1B1B);
                    focus.setStroke(dp(2), isLight ? 0xFF111111 : 0xFFFFFFFF);
                    v.setBackgroundDrawable(focus);
                } else {
                    v.setBackgroundDrawable(empty);
                }
            }
        });

        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                action.run();
            }
        });

        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return current;
    }

    private void updateValues() {
        if (themeValue != null) themeValue.setText(isLight ? "Claro" : "Oscuro");
        if (backgroundValue != null) {
            int mode = BackgroundManager.mode(this);
            if (mode == BackgroundManager.MODE_CUSTOM && BackgroundManager.hasCustom(this)) {
                backgroundValue.setText("Galería");
            } else if (mode == BackgroundManager.MODE_NONE) {
                backgroundValue.setText("Sin fondo");
            } else {
                backgroundValue.setText("Incluido");
            }
        }
        if (secondsValue != null) {
            secondsValue.setText(prefs.getBoolean("clock_seconds", false) ? "Sí" : "No");
        }
        if (notificationValue != null) {
            notificationValue.setText(NotificationStore.hasAccess(this) ? "Concedido" : "No concedido");
        }
        if (homeValue != null) {
            if (HomeUtil.isDefaultHome(this)) {
                homeValue.setText("NSP (actual)");
            } else if (HomeUtil.isAliasEnabled(this)) {
                homeValue.setText("Activado");
            } else {
                homeValue.setText("Desactivado");
            }
        }
    }

    private void updateWeatherStatus() {
        WeatherData data = WeatherService.loadCached(this);
        if (data == null) {
            weatherStatus.setText("Sin datos todavía");
        } else {
            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", new Locale("es", "ES"));
            weatherStatus.setText("Última actualización: " + fmt.format(new Date(data.updated))
                    + " · " + Math.round(data.temperature) + "° " + WeatherService.label(data.code));
        }
    }

    private void refreshWeather() {
        if (fetchingWeather) return;
        fetchingWeather = true;
        weatherStatus.setText("Actualizando...");
        WeatherService.fetch(this, new WeatherService.Callback() {
            public void onResult(WeatherData data, String error) {
                fetchingWeather = false;
                updateWeatherStatus();
                if (data == null) {
                    toast("No se pudo actualizar: " + error);
                } else {
                    toast("Clima actualizado");
                }
            }
        });
    }

    private void showHomeDialog() {
        final boolean enable = !HomeUtil.isAliasEnabled(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(enable ? "Usar NSP como inicio" : "Desactivar NSP como inicio");
        builder.setMessage(enable
                ? "NSP aparecerá como opción de Inicio. Pulsa Home, elige NSP y marca Siempre.\n\n"
                + "Para volver atrás usa este mismo ajuste o Ajustes de Inicio del sistema."
                : "NSP dejará de ser opción de Inicio. Si ahora es el inicio predeterminado, "
                + "Android volverá al launcher de NVIDIA.");
        builder.setPositiveButton(enable ? "Activar" : "Desactivar",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        HomeUtil.setAliasEnabled(SettingsActivity.this, enable);
                        updateValues();
                        toast(enable
                                ? "Pulsa Home y elige NSP; marca Siempre"
                                : "NSP ya no es opción de Inicio");
                    }
                });
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialogOpen = true;
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                dialogOpen = false;
                updateValues();
            }
        });
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B && event.getAction() == KeyEvent.ACTION_DOWN) {
                    d.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    private void showBackgroundDialog() {
        final ArrayList<String> labels = new ArrayList<String>();
        final ArrayList<Integer> actions = new ArrayList<Integer>();

        labels.add("Incluido (tema actual)");
        actions.add(0);
        labels.add("Imagen de la galería");
        actions.add(1);
        labels.add("Sin fondo");
        actions.add(2);
        if (BackgroundManager.hasCustom(this)) {
            labels.add("Quitar imagen");
            actions.add(3);
        }

        final String[] items = labels.toArray(new String[labels.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Fondo de pantalla");
        builder.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(isLight ? 0xFF111111 : 0xFFFFFFFF);
                tv.setTextSize(14);
                tv.setPadding(dp(20), dp(6), dp(20), dp(6));
                tv.setMinHeight(0);
                tv.setMinimumHeight(0);
                return tv;
            }
        }, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int action = actions.get(which);
                if (action == 0) {
                    BackgroundManager.setMode(SettingsActivity.this,
                            BackgroundManager.MODE_BUNDLED);
                    prefs.edit().putBoolean("bg_changed", true).apply();
                    updateValues();
                } else if (action == 1) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        public void run() {
                            pickBackgroundImage();
                        }
                    }, 150);
                } else if (action == 2) {
                    BackgroundManager.setMode(SettingsActivity.this, BackgroundManager.MODE_NONE);
                    prefs.edit().putBoolean("bg_changed", true).apply();
                    updateValues();
                } else {
                    BackgroundManager.clearCustom(SettingsActivity.this);
                    prefs.edit().putBoolean("bg_changed", true).apply();
                    updateValues();
                }
            }
        });
        builder.show();
    }

    private void pickBackgroundImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, REQ_PICK_IMAGE);
        } catch (Exception e) {
            try {
                startActivityForResult(new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQ_PICK_IMAGE);
            } catch (Exception ignored) {
                toast("No se encontró una galería");
            }
        }
    }

    private void importBackground(final Uri uri) {
        toast("Procesando imagen...");
        new Thread(new Runnable() {
            public void run() {
                try {
                    final float luminance = BackgroundManager.importImage(
                            getApplicationContext(), uri);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (isFinishing()) return;
                            prefs.edit().putBoolean("bg_changed", true).apply();
                            if (BackgroundManager.themeOverride(SettingsActivity.this)
                                    == BackgroundManager.THEME_AUTO) {
                                boolean light = luminance >= 0.55f;
                                if (light != isLight) {
                                    isLight = light;
                                    prefs.edit().putBoolean("light", light).apply();
                                    updateValues();
                                    recreate();
                                    return;
                                }
                            }
                            updateValues();
                            toast("Fondo actualizado");
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (isFinishing()) return;
                            toast("No se pudo usar la imagen");
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) importBackground(uri);
        }
    }

    private void openNotificationSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            } catch (Exception ignored) {
                toast("No se pudo abrir Acceso a notificaciones");
            }
        }
    }

    private void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                toast("No se pudo abrir Ajustes");
            }
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void applyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateValues();
        updateWeatherStatus();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (dialogOpen) return super.dispatchKeyEvent(event);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_BUTTON_A
                    || code == KeyEvent.KEYCODE_DPAD_CENTER
                    || code == KeyEvent.KEYCODE_ENTER) {
                if (event.getRepeatCount() == 0) {
                    View focused = getCurrentFocus();
                    if (focused != null) focused.performClick();
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
