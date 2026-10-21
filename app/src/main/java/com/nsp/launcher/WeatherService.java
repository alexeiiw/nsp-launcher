package com.nsp.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class WeatherService {

    public static final String CITY = "Guatemala";

    private static final double LAT = 14.6349;
    private static final double LON = -90.5069;
    private static final String ENDPOINT = "http://api.open-meteo.com/v1/forecast?latitude="
            + LAT + "&longitude=" + LON
            + "&current_weather=true&timezone=America%2FGuatemala";
    private static final long MAX_AGE = 30L * 60L * 1000L;

    public interface Callback {
        void onResult(WeatherData data, String error);
    }

    private WeatherService() {
    }

    public static WeatherData loadCached(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("nsp", Context.MODE_PRIVATE);
        long updated = prefs.getLong("weather_updated", 0L);
        if (updated <= 0L) return null;
        float temp = prefs.getFloat("weather_temp", 0f);
        int code = prefs.getInt("weather_code", -1);
        return new WeatherData(temp, code, updated);
    }

    public static boolean isStale(WeatherData data) {
        return data == null || System.currentTimeMillis() - data.updated > MAX_AGE;
    }

    public static void fetch(final Context context, final Callback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            public void run() {
                WeatherData data = null;
                String error = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(ENDPOINT);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.setRequestProperty("User-Agent", "NSPLauncher/1.1");
                    connection.setRequestProperty("Accept", "application/json");

                    int status = connection.getResponseCode();
                    if (status != 200) throw new Exception("HTTP " + status);

                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in, StandardCharsets.UTF_8));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) builder.append(line);
                    reader.close();

                    JSONObject root = new JSONObject(builder.toString());
                    JSONObject current = root.getJSONObject("current_weather");
                    double temperature = current.getDouble("temperature");
                    int code = current.optInt("weathercode", 0);

                    data = new WeatherData(temperature, code, System.currentTimeMillis());
                    save(app, data);
                } catch (Exception e) {
                    error = e.getMessage() == null ? "Sin conexion" : e.getMessage();
                } finally {
                    if (connection != null) connection.disconnect();
                }

                final WeatherData result = data;
                final String message = error;
                main.post(new Runnable() {
                    public void run() {
                        callback.onResult(result, message);
                    }
                });
            }
        }).start();
    }

    private static void save(Context context, WeatherData data) {
        context.getSharedPreferences("nsp", Context.MODE_PRIVATE).edit()
                .putFloat("weather_temp", (float) data.temperature)
                .putInt("weather_code", data.code)
                .putLong("weather_updated", data.updated)
                .apply();
    }

    public static String label(int code) {
        if (code == 0) return "Despejado";
        if (code == 1) return "Mayormente despejado";
        if (code == 2) return "Parcialmente nublado";
        if (code == 3) return "Nublado";
        if (code == 45 || code == 48) return "Niebla";
        if (code >= 51 && code <= 57) return "Llovizna";
        if (code >= 61 && code <= 67) return "Lluvia";
        if (code >= 71 && code <= 77) return "Nieve";
        if (code >= 80 && code <= 82) return "Chubascos";
        if (code == 85 || code == 86) return "Chubascos de nieve";
        if (code == 95) return "Tormenta";
        if (code == 96 || code == 99) return "Tormenta con granizo";
        return "Variable";
    }
}
