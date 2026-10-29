package com.nsp.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private TextView clockValue;
    private TextView caseStatus;
    private EditText caseInput;
    private LinearLayout caseSaveRow;
    private TextView updateValue;

    private boolean caseTesting;

    private static final int REQ_PICK_IMAGE = 1001;
    private static final int REQ_STORAGE = 1002;

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
        hint.setText("v" + UpdateService.installedVersion(this) + " · A elegir  |  B volver");
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

        clockValue = addRow(content, "Reloj de la cabecera", "", new Runnable() {
            public void run() {
                showClockDialog();
            }
        });

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

        addRow(content, "Apagar consola…", "", new Runnable() {
            public void run() {
                openPowerMenu();
            }
        });

        addLabel(content, "CASE");

        TextView caseHint = new TextView(this);
        caseHint.setText("Pega el bloque del script (URL y token) o importa case.txt. "
                + "La URL y el token se guardan solo en este aparato.");
        caseHint.setTextSize(9);
        caseHint.setTextColor(isLight ? 0xFF666666 : 0xFF9E9E9E);
        caseHint.setPadding(dp(24), dp(2), dp(24), dp(6));
        content.addView(caseHint, new LinearLayout.LayoutParams(-1, -2));

        caseInput = new EditText(this);
        caseInput.setHint("$url = \"https://script.google.com/…\"\n$token = \"…\"");
        caseInput.setSingleLine(false);
        caseInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        caseInput.setMinLines(5);
        caseInput.setMaxLines(8);
        caseInput.setGravity(Gravity.TOP | Gravity.START);
        caseInput.setTextSize(12);
        caseInput.setTypeface(Typeface.MONOSPACE);
        caseInput.setTextColor(isLight ? 0xFF111111 : 0xFFFFFFFF);
        caseInput.setHintTextColor(isLight ? 0xFF999999 : 0xFF666666);
        GradientDrawable box = flat(isLight ? 0xFFF4F4F4 : 0xFF101010);
        box.setStroke(dp(1), isLight ? 0xFFCCCCCC : 0xFF333333);
        box.setCornerRadius(dp(4));
        caseInput.setBackgroundDrawable(box);
        caseInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(-1, -2);
        boxParams.leftMargin = dp(24);
        boxParams.rightMargin = dp(24);
        content.addView(caseInput, boxParams);

        caseStatus = new TextView(this);
        caseStatus.setTextSize(9);
        caseStatus.setTextColor(isLight ? 0xFF666666 : 0xFF9E9E9E);
        caseStatus.setPadding(dp(24), dp(4), dp(24), dp(4));
        content.addView(caseStatus, new LinearLayout.LayoutParams(-1, -2));

        TextView saveValue = addRow(content, "Guardar CASE", "", new Runnable() {
            public void run() {
                saveCase();
            }
        });
        caseSaveRow = (LinearLayout) saveValue.getParent();

        addRow(content, "Importar desde archivo", "case.txt", new Runnable() {
            public void run() {
                importCase();
            }
        });

        addRow(content, "Probar CASE", "Ahora", new Runnable() {
            public void run() {
                testCase();
            }
        });

        addRow(content, "Borrar configuración", "", new Runnable() {
            public void run() {
                confirmClearCase();
            }
        });

        addLabel(content, "ACERCA DE");
        addRow(content, "Versión",
                UpdateService.installedVersion(this) + " · código "
                        + UpdateService.installedCode(this),
                new Runnable() {
                    public void run() {
                        showAbout();
                    }
                });

        updateValue = addRow(content, "Actualización",
                UpdateService.statusText(this), new Runnable() {
                    public void run() {
                        updateValue.setText("Comprobando…");
                        UpdateFlow.checkPrompt(SettingsActivity.this, new Runnable() {
                            public void run() {
                                updateValues();
                            }
                        });
                    }
                });

        TextView footer = new TextView(this);
        footer.setText("NSP " + UpdateService.installedVersion(this)
                + "\nAndroid 5.1 · " + WeatherService.CITY);
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
        if (clockValue != null) {
            int style = prefs.getInt("clock_style", 0);
            if (style == 1) {
                clockValue.setText("Digital");
            } else if (style == 2) {
                clockValue.setText("Oculto");
            } else {
                clockValue.setText("Analógico");
            }
        }
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
        if (caseStatus != null) {
            if (!CaseService.isConfigured(this)) {
                caseStatus.setText("Sin configurar");
            } else {
                String line = CaseService.host(this) + " · token "
                        + CaseService.maskedToken(this) + " · " + CaseService.stamp(this);
                String error = CaseService.cachedError(this);
                if (error != null && error.length() > 0) {
                    line = line + " · " + error;
                }
                caseStatus.setText(line);
            }
        }
        if (updateValue != null) {
            updateValue.setText(UpdateService.statusText(this));
        }
    }

    private void saveCase() {
        CaseService.ParseResult result = CaseService.parse(caseInput.getText().toString());
        if (result.error != null) {
            toast(result.error);
            return;
        }
        CaseService.save(this, result.config);
        caseInput.setText("");
        updateValues();
        toast("CASE guardado");
    }

    private void testCase() {
        if (caseTesting) return;
        String raw = caseInput.getText().toString().trim();
        CaseService.Config config;
        if (raw.length() == 0) {
            config = CaseService.load(this);
            if (!config.complete()) {
                toast("Pega la URL y el token, o importa case.txt");
                return;
            }
        } else {
            CaseService.ParseResult result = CaseService.parse(raw);
            if (result.error != null) {
                toast(result.error);
                return;
            }
            config = result.config;
        }

        caseTesting = true;
        caseStatus.setText("Consultando… puede tardar unos 20 s");
        CaseService.test(this, config, new CaseService.Callback() {
            public void onResult(String text, String error) {
                caseTesting = false;
                updateValues();
                if (error != null) {
                    toast("Error: " + error);
                } else {
                    toast("CASE respondió · " + text.length() + " caracteres");
                }
            }
        });
    }

    private void importCase() {
        File appFile = new File(getExternalFilesDir(null), "case.txt");
        String text = readCaseFile(appFile);
        if (text == null) {
            if (!canReadStorage()) {
                if (Build.VERSION.SDK_INT >= 23) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQ_STORAGE);
                }
                return;
            }
            File downloads = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "case.txt");
            text = readCaseFile(downloads);
        }
        if (text == null) {
            toast("No se encontró case.txt en Descargas ni en la carpeta de NSP");
            return;
        }
        caseInput.setText(text);
        caseInput.setSelection(caseInput.getText().length());
        toast("Archivo importado; pulsa Guardar CASE");
    }

    private boolean canReadStorage() {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String readCaseFile(File file) {
        if (file == null || !file.isFile()) return null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) builder.append(line).append('\n');
            String text = builder.toString().trim();
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            return text.length() == 0 ? null : text;
        } catch (Exception e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void confirmClearCase() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Borrar configuración CASE");
        builder.setMessage("Se quitarán la URL, el token y el último reporte de este aparato.");
        builder.setPositiveButton("Borrar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                CaseService.clear(SettingsActivity.this);
                caseInput.setText("");
                updateValues();
                toast("Configuración CASE borrada");
            }
        });
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialogOpen = true;
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                dialogOpen = false;
            }
        });
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    d.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    private void showKeyboard() {
        caseInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(caseInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(caseInput.getWindowToken(), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_STORAGE) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            importCase();
        } else {
            toast("Sin permiso no se puede leer Descargas");
        }
    }

    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NSP " + UpdateService.installedVersion(this));
        builder.setMessage("Launcher para NVIDIA Shield Portable.\n"
                + "Android 5.1.1 · API 22\n\n"
                + "https://github.com/alexeiiw/nsp-launcher");
        builder.setPositiveButton("Abrir GitHub", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                BrowserUtil.open(SettingsActivity.this,
                        "https://github.com/alexeiiw/nsp-launcher");
            }
        });
        builder.setNegativeButton("Cerrar", null);
        builder.show();
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

    private void openPowerMenu() {
        if (PowerMenuService.serviceConnected()) {
            PowerMenuService.openPowerMenu();
            return;
        }
        if (PowerMenuService.isEnabled(this)) {
            toast("El servicio se está activando; inténtalo de nuevo");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Apagar consola");
        builder.setMessage("NSP abre el menú de apagado del sistema a través de Acceso. "
                + "Activa el servicio «NSP · Menú de energía» en Ajustes → Accesibilidad "
                + "(una sola vez). No lee el contenido de la pantalla.");
        builder.setPositiveButton("Abrir Acceso", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (Exception ex) {
                    toast("No se pudo abrir Accesibilidad");
                }
            }
        });
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialogOpen = true;
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                dialogOpen = false;
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

    private void showClockDialog() {
        final String[] items = {"Analógico", "Digital", "Oculto"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reloj de la cabecera");
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
                prefs.edit().putInt("clock_style", which).apply();
                updateValues();
            }
        });
        builder.show();
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
            View focused = getCurrentFocus();
            if (focused == caseInput) {
                if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    return super.dispatchKeyEvent(event);
                }
                if (code == KeyEvent.KEYCODE_BUTTON_A
                        || code == KeyEvent.KEYCODE_DPAD_CENTER) {
                    showKeyboard();
                    return true;
                }
                if (code == KeyEvent.KEYCODE_BUTTON_B) {
                    hideKeyboard();
                    return true;
                }
                if (code == KeyEvent.KEYCODE_DPAD_DOWN && caseSaveRow != null) {
                    caseSaveRow.requestFocus();
                    return true;
                }
            }
            if (code == KeyEvent.KEYCODE_BUTTON_A
                    || code == KeyEvent.KEYCODE_DPAD_CENTER
                    || code == KeyEvent.KEYCODE_ENTER) {
                if (event.getRepeatCount() == 0) {
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
