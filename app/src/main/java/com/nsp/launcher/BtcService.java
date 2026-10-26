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

public final class BtcService {

    private static final String ENDPOINT =
            "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd";
    private static final long MAX_AGE = 60L * 60L * 1000L;

    public interface Callback {
        void onResult(BtcData data, String error);
    }

    private BtcService() {
    }

    public static BtcData loadCached(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("nsp", Context.MODE_PRIVATE);
        long updated = prefs.getLong("btc_updated", 0L);
        if (updated <= 0L) return null;
        return new BtcData(prefs.getFloat("btc_usd", 0f), updated);
    }

    public static boolean isStale(BtcData data) {
        return data == null || System.currentTimeMillis() - data.updated > MAX_AGE;
    }

    public static String format(double usd) {
        return String.format(java.util.Locale.US, "$%,.0f", usd);
    }

    public static void fetch(final Context context, final Callback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            public void run() {
                BtcData data = null;
                String error = null;
                try {
                    data = download(app, null);
                } catch (Exception first) {
                    // El almacén del sistema puede no tener la raíz de la API
                    // (Let's Encrypt). Reintento con las raíces empaquetadas.
                    try {
                        data = download(app, TrustedRoots.factory(app));
                    } catch (Exception second) {
                        error = second.getMessage() == null ? "Sin conexion" : second.getMessage();
                    }
                }

                final BtcData result = data;
                final String message = error;
                main.post(new Runnable() {
                    public void run() {
                        callback.onResult(result, message);
                    }
                });
            }
        }).start();
    }

    private static BtcData download(Context context, javax.net.ssl.SSLSocketFactory sslFactory)
            throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            if (sslFactory != null && connection instanceof javax.net.ssl.HttpsURLConnection) {
                ((javax.net.ssl.HttpsURLConnection) connection).setSSLSocketFactory(sslFactory);
            }
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "NSPLauncher/1.3");
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
            double usd = root.getJSONObject("bitcoin").getDouble("usd");
            BtcData data = new BtcData(usd, System.currentTimeMillis());
            save(context, data);
            return data;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void save(Context context, BtcData data) {
        context.getSharedPreferences("nsp", Context.MODE_PRIVATE).edit()
                .putFloat("btc_usd", (float) data.usd)
                .putLong("btc_updated", data.updated)
                .apply();
    }
}
