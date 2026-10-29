package com.nsp.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Modulo CASE: consulta el Apps Script, guarda el ultimo reporte y decide
 * cuando toca refrescar. La URL y el token viven solo en SharedPreferences.
 */
public final class CaseService {

    private static final String PREFS = "nsp";
    private static final String K_URL = "case_url";
    private static final String K_TOKEN = "case_token";
    private static final String K_CANAL = "case_canal";
    private static final String K_TEXTO = "case_texto";
    private static final String K_TIME = "case_time";
    private static final String K_TEXT = "case_text";
    private static final String K_STATUS = "case_status";
    private static final String K_ERROR = "case_error";
    private static final String K_ATTEMPT = "case_attempt";

    private static final int CONNECT_TIMEOUT = 20000;
    private static final int READ_TIMEOUT = 120000;

    private static boolean refreshing;

    public interface Callback {
        void onResult(String text, String error);
    }

    public static final class Config {
        public String url = "";
        public String token = "";
        public String canal = "terminal";
        public String texto = "/hoy";

        public boolean complete() {
            return url != null && url.length() > 0 && token != null && token.length() > 0;
        }
    }

    public static final class ParseResult {
        public Config config;
        public String error;
    }

    private CaseService() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isConfigured(Context context) {
        Config config = load(context);
        return config.complete();
    }

    public static Config load(Context context) {
        SharedPreferences p = prefs(context);
        Config config = new Config();
        config.url = p.getString(K_URL, "");
        config.token = p.getString(K_TOKEN, "");
        String canal = p.getString(K_CANAL, "terminal");
        String texto = p.getString(K_TEXTO, "/hoy");
        if (canal != null && canal.length() > 0) config.canal = canal;
        if (texto != null && texto.length() > 0) config.texto = texto;
        return config;
    }

    public static void save(Context context, Config config) {
        prefs(context).edit()
                .putString(K_URL, config.url)
                .putString(K_TOKEN, config.token)
                .putString(K_CANAL, config.canal)
                .putString(K_TEXTO, config.texto)
                .apply();
    }

    public static void clear(Context context) {
        prefs(context).edit()
                .remove(K_URL)
                .remove(K_TOKEN)
                .remove(K_CANAL)
                .remove(K_TEXTO)
                .remove(K_TIME)
                .remove(K_TEXT)
                .remove(K_STATUS)
                .remove(K_ERROR)
                .remove(K_ATTEMPT)
                .apply();
    }

    public static String maskedToken(Context context) {
        String token = prefs(context).getString(K_TOKEN, "");
        if (token == null || token.length() == 0) return "";
        if (token.length() <= 4) return "••••";
        return "••••" + token.substring(token.length() - 4);
    }

    public static String cachedText(Context context) {
        return prefs(context).getString(K_TEXT, "");
    }

    public static String cachedError(Context context) {
        return prefs(context).getString(K_ERROR, "");
    }

    public static long lastTime(Context context) {
        return prefs(context).getLong(K_TIME, 0L);
    }

    public static String host(Context context) {
        return hostOf(prefs(context).getString(K_URL, ""));
    }

    private static String hostOf(String url) {
        try {
            URL parsed = new URL(url);
            String host = parsed.getHost();
            return host == null ? url : host;
        } catch (Exception e) {
            return url == null ? "" : url;
        }
    }

    public static String stamp(Context context) {
        return stampOf(lastTime(context));
    }

    /** "hoy 10:19", "ayer 20:01" o "29 oct 08:02", sin prefijo. */
    public static String shortStamp(long time) {
        if (time <= 0L) return "nunca";
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(time);
        SimpleDateFormat hour = new SimpleDateFormat("HH:mm", new Locale("es", "ES"));
        if (sameDay(now, then)) return "hoy " + hour.format(new Date(time));
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(now, then)) return "ayer " + hour.format(new Date(time));
        SimpleDateFormat full = new SimpleDateFormat("d MMM HH:mm", new Locale("es", "ES"));
        return full.format(new Date(time));
    }

    /** Momento del ultimo intento de consulta. */
    public static String attemptStamp(Context context) {
        return shortStamp(prefs(context).getLong(K_ATTEMPT, 0L));
    }

    public static String stampOf(long time) {
        if (time <= 0L) return "Sin actualizar";
        return "Actualizado " + shortStamp(time);
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    /** Hora del slot mas reciente: hoy 08:00/20:00 o el de ayer. */
    public static long lastSlot(long now) {
        Calendar slot = Calendar.getInstance();
        slot.setTimeInMillis(now);
        slot.set(Calendar.SECOND, 0);
        slot.set(Calendar.MILLISECOND, 0);
        slot.set(Calendar.HOUR_OF_DAY, 20);
        slot.set(Calendar.MINUTE, 0);
        if (slot.getTimeInMillis() <= now) return slot.getTimeInMillis();
        slot.set(Calendar.HOUR_OF_DAY, 8);
        if (slot.getTimeInMillis() <= now) return slot.getTimeInMillis();
        slot.add(Calendar.DAY_OF_YEAR, -1);
        slot.set(Calendar.HOUR_OF_DAY, 20);
        return slot.getTimeInMillis();
    }

    public static boolean isDue(Context context) {
        if (!isConfigured(context)) return false;
        long last = lastTime(context);
        return last <= 0L || last < lastSlot(System.currentTimeMillis());
    }

    /**
     * Nivel del indicador: 0 sin configurar, 1 error, 2 desactualizado, 4 al dia.
     */
    public static int indicator(Context context) {
        if (!isConfigured(context)) return 0;
        String status = prefs(context).getString(K_STATUS, "");
        if ("error".equals(status)) return 1;
        if (isDue(context)) return 2;
        return 4;
    }

    /** Lanza una consulta si el slot vencio y no se ha intentado nada desde que empezo. */
    public static boolean refreshIfDue(Context context, Callback callback) {
        if (!isConfigured(context)) return false;
        if (!isDue(context)) return false;
        long attempt = prefs(context).getLong(K_ATTEMPT, 0L);
        if (attempt >= lastSlot(System.currentTimeMillis())) return false;
        refresh(context, callback);
        return true;
    }

    public static void refresh(final Context context, final Callback callback) {
        final Config config = load(context);
        if (!config.complete()) {
            if (callback != null) callback.onResult(null, "Falta configurar CASE");
            return;
        }
        run(context, config, true, callback);
    }

    /** Consulta de prueba: no toca la cache ni la configuracion. */
    public static void test(Context context, Config config, Callback callback) {
        if (config == null || !config.complete()) {
            if (callback != null) callback.onResult(null, "Falta la URL o el token");
            return;
        }
        run(context, config, false, callback);
    }

    private static void run(final Context context, final Config config, final boolean persist,
                            final Callback callback) {
        if (refreshing) {
            if (callback != null) callback.onResult(null, "Ya hay una consulta en curso");
            return;
        }
        refreshing = true;
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            public void run() {
                String body = null;
                String error = null;
                try {
                    body = fetchRaw(config, null);
                } catch (Exception first) {
                    // El almacen del sistema (Android 5.1) puede no tener la raiz
                    // de Google Trust Services: reintento con las raices empaquetadas.
                    try {
                        body = fetchRaw(config, TrustedRoots.factory(app));
                    } catch (Exception second) {
                        error = message(second);
                    }
                }

                String text = null;
                if (error == null) {
                    String[] parseError = new String[1];
                    text = parseBody(body, parseError);
                    if (text == null) error = parseError[0];
                }

                if (persist) persistResult(app, text, error);

                final String resultText = text;
                final String resultError = error;
                main.post(new Runnable() {
                    public void run() {
                        refreshed();
                        if (callback != null) callback.onResult(resultText, resultError);
                    }
                });
            }
        }).start();
    }

    private static synchronized void refreshed() {
        refreshing = false;
    }

    private static String message(Exception e) {
        if (e == null) return "Sin conexion";
        String message = e.getMessage();
        return message == null || message.length() == 0 ? "Sin conexion" : message;
    }

    private static void persistResult(Context context, String text, String error) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor edit = prefs(context).edit().putLong(K_ATTEMPT, now);
        if (text != null) {
            edit.putString(K_TEXT, text)
                    .putLong(K_TIME, now)
                    .putString(K_STATUS, "ok")
                    .remove(K_ERROR);
        } else {
            edit.putString(K_STATUS, "error")
                    .putString(K_ERROR, error == null ? "Sin conexion" : error);
        }
        edit.apply();
    }

    private static String fetchRaw(Config config, SSLSocketFactory ssl) throws Exception {
        URL url = new URL(config.url);
        HttpURLConnection post = null;
        try {
            post = (HttpURLConnection) url.openConnection();
            if (ssl != null && post instanceof HttpsURLConnection) {
                ((HttpsURLConnection) post).setSSLSocketFactory(ssl);
            }
            post.setInstanceFollowRedirects(false);
            post.setRequestMethod("POST");
            post.setDoOutput(true);
            post.setConnectTimeout(CONNECT_TIMEOUT);
            post.setReadTimeout(READ_TIMEOUT);
            post.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            post.setRequestProperty("Accept", "application/json");
            post.setRequestProperty("User-Agent", "NSPLauncher/1.6");

            JSONObject payload = new JSONObject();
            payload.put("canal", config.canal);
            payload.put("token", config.token);
            payload.put("texto", config.texto);

            byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            post.setFixedLengthStreamingMode(bytes.length);
            OutputStream out = post.getOutputStream();
            try {
                out.write(bytes);
                out.flush();
            } finally {
                out.close();
            }

            int code = post.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = post.getHeaderField("Location");
                if (location == null || location.length() == 0) {
                    throw new Exception("El script no devolvio destino");
                }
                URL next = new URL(url, location);
                post.disconnect();
                post = null;
                return getOnce(next, ssl);
            }
            if (code != 200) throw new Exception("HTTP " + code);
            return read(post.getInputStream());
        } finally {
            if (post != null) post.disconnect();
        }
    }

    /** El GET a googleusercontent es de un solo uso: se hace una vez y sin reintento. */
    private static String getOnce(URL url, SSLSocketFactory ssl) throws Exception {
        HttpURLConnection get = null;
        try {
            get = (HttpURLConnection) url.openConnection();
            if (ssl != null && get instanceof HttpsURLConnection) {
                ((HttpsURLConnection) get).setSSLSocketFactory(ssl);
            }
            get.setInstanceFollowRedirects(false);
            get.setRequestMethod("GET");
            get.setConnectTimeout(CONNECT_TIMEOUT);
            get.setReadTimeout(READ_TIMEOUT);
            get.setRequestProperty("Accept", "application/json");
            get.setRequestProperty("User-Agent", "NSPLauncher/1.6");

            int code = get.getResponseCode();
            if (code >= 300 && code < 400) {
                throw new Exception("El script no devolvio datos");
            }
            if (code != 200) throw new Exception("HTTP " + code);
            return read(get.getInputStream());
        } finally {
            if (get != null) get.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append('\n');
        }
        reader.close();
        int end = builder.length();
        while (end > 0 && (builder.charAt(end - 1) == '\n' || builder.charAt(end - 1) == '\r')) {
            end--;
        }
        return builder.substring(0, end);
    }

    private static String parseBody(String body, String[] error) {
        String text = body == null ? "" : body.trim();
        if (text.length() == 0) {
            error[0] = "Sin respuesta del script";
            return null;
        }
        if (text.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(text);
                if (!json.optBoolean("ok", true)) {
                    String message = json.optString("error", "");
                    error[0] = message.length() == 0 ? "El script devolvio un error" : message;
                    return null;
                }
                String content = json.optString("respuesta_plana", "");
                if (content.trim().length() == 0) content = json.optString("respuesta", "");
                if (content.trim().length() == 0) {
                    error[0] = "El script no devolvio datos";
                    return null;
                }
                return content;
            } catch (Exception ignored) {
                // No es JSON valido: se trata como texto.
            }
        }
        if (text.startsWith("<")) {
            error[0] = "El script no devolvio datos";
            return null;
        }
        return text;
    }

    // ---------------------------------------------------------------- parseo

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");
    private static final Pattern TOKEN_VAR = Pattern.compile(
            "\\$token\\s*=\\s*[\"']?([^\"'\\s;]+)[\"']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_ANY = Pattern.compile(
            "(?<![\\$\\w])token\\s*=\\s*[\"']?([^\"'\\s;]+)[\"']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CANAL = Pattern.compile(
            "canal\\s*=\\s*[\"']?([^\"'\\s;]+)[\"']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXTO = Pattern.compile(
            "texto\\s*=\\s*[\"']?([^\"'\\s;]+)[\"']?", Pattern.CASE_INSENSITIVE);

    /** Acepta el bloque de PowerShell, un formato url=/token= o un JSON. */
    public static ParseResult parse(String raw) {
        ParseResult result = new ParseResult();
        result.config = new Config();
        if (raw == null || raw.trim().length() == 0) {
            result.error = "Pega el bloque con la URL y el token";
            return result;
        }
        String block = raw.trim();

        if (block.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(block);
                result.config.url = json.optString("url", "");
                result.config.token = json.optString("token", "");
                String canal = json.optString("canal", "");
                String texto = json.optString("texto", "");
                if (canal.length() > 0) result.config.canal = canal;
                if (texto.length() > 0) result.config.texto = texto;
            } catch (Exception ignored) {
            }
        }

        if (result.config.url.length() == 0) {
            Matcher matcher = URL_PATTERN.matcher(block);
            String plain = null;
            while (matcher.find()) {
                String candidate = matcher.group();
                if (candidate.contains("script.google.com")) {
                    result.config.url = cleanUrl(candidate);
                    break;
                }
                if (plain == null) plain = candidate;
            }
            if (result.config.url.length() == 0 && plain != null) {
                result.config.url = cleanUrl(plain);
            }
        }

        if (result.config.token.length() == 0) {
            Matcher matcher = TOKEN_VAR.matcher(block);
            if (matcher.find()) {
                result.config.token = matcher.group(1);
            } else {
                matcher = TOKEN_ANY.matcher(block);
                while (matcher.find()) {
                    String value = matcher.group(1);
                    if (!value.startsWith("$")) {
                        result.config.token = value;
                        break;
                    }
                }
            }
        }

        Matcher canal = CANAL.matcher(block);
        if (canal.find() && canal.group(1).length() > 0) result.config.canal = canal.group(1);

        Matcher texto = TEXTO.matcher(block);
        if (texto.find() && texto.group(1).length() > 0) result.config.texto = texto.group(1);

        if (result.config.url.length() == 0 || result.config.token.length() == 0) {
            result.error = "Falta la URL o el token";
            return result;
        }
        if (!result.config.url.startsWith("http")) {
            result.error = "La URL no es valida";
            return result;
        }
        return result;
    }

    public static String firstLink(String text) {
        if (text == null) return null;
        Matcher matcher = URL_PATTERN.matcher(text);
        return matcher.find() ? cleanUrl(matcher.group()) : null;
    }

    private static String cleanUrl(String candidate) {
        int end = candidate.indexOf("/exec");
        return end >= 0 ? candidate.substring(0, end + 5) : candidate;
    }
}
