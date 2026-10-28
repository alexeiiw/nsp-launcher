package com.nsp.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/** Comprueba, descarga y valida las Releases de GitHub. */
public final class UpdateService {

    private static final String API =
            "https://api.github.com/repos/alexeiiw/nsp-launcher/releases/latest";
    private static final long CHECK_INTERVAL = 24L * 60L * 60L * 1000L;
    private static final String PREFS = "nsp";

    public static class Release {
        public String version = "";
        public int versionCode;
        public String tag = "";
        public String url = "";
        public String pageUrl = "";
        public long size;
    }

    public interface CheckCallback {
        void onResult(Release release, String error);
    }

    public interface DownloadCallback {
        void onProgress(int percent);

        void onResult(String path, String error);
    }

    private UpdateService() {
    }

    public static String installedVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.versionName == null ? "?" : info.versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    public static int installedCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String statusText(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long checked = prefs.getLong("update_checked_at", 0L);
        if (checked <= 0L) return "Sin comprobar";
        String version = prefs.getString("update_version", null);
        String when = new SimpleDateFormat("d MMM HH:mm", new Locale("es", "ES"))
                .format(new Date(checked));
        if (version != null && compare(version, installedVersion(context)) > 0) {
            return version + " disponible · " + when;
        }
        return "Al día · " + when;
    }

    public static Release cached(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String version = prefs.getString("update_version", null);
        String url = prefs.getString("update_url", null);
        if (version == null || url == null) return null;
        if (compare(version, installedVersion(context)) <= 0) return null;
        Release release = new Release();
        release.version = version;
        release.versionCode = toCode(version);
        release.url = url;
        release.pageUrl = prefs.getString("update_page", "");
        release.size = prefs.getLong("update_size", 0L);
        return release;
    }

    public static void check(final Context context, final boolean force,
                             final CheckCallback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        long checked = prefs.getLong("update_checked_at", 0L);
        if (!force && checked > 0L
                && System.currentTimeMillis() - checked < CHECK_INTERVAL) {
            final Release cached = cached(app);
            main.post(new Runnable() {
                public void run() {
                    callback.onResult(cached, null);
                }
            });
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                Release release = null;
                String error = null;
                try {
                    String body;
                    try {
                        body = fetch(app, API, null);
                    } catch (Exception first) {
                        body = fetch(app, API, TrustedRoots.factory(app));
                    }
                    release = parse(app, body);
                    if (release != null
                            && compare(release.version, installedVersion(app)) <= 0) {
                        release = null;
                    }
                    save(app, release);
                } catch (Exception e) {
                    error = e.getMessage() == null ? "Sin conexión" : e.getMessage();
                }
                final Release result = release;
                final String message = error;
                main.post(new Runnable() {
                    public void run() {
                        callback.onResult(result, message);
                    }
                });
            }
        }).start();
    }

    private static String fetch(Context context, String address, SSLSocketFactory factory)
            throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            if (factory != null && connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(factory);
            }
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "NSPLauncher/1.5");
            connection.setRequestProperty("Accept", "application/vnd.github+json");

            int status = connection.getResponseCode();
            if (status != 200) throw new Exception("HTTP " + status);

            InputStream in = connection.getInputStream();
            try {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(in, "UTF-8"));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) builder.append(line);
                reader.close();
                return builder.toString();
            } finally {
                in.close();
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static Release parse(Context context, String body) throws Exception {
        JSONObject root = new JSONObject(body);
        String tag = root.optString("tag_name", "");
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        if (version.isEmpty()) throw new Exception("Release sin etiqueta");

        JSONArray assets = root.optJSONArray("assets");
        String url = null;
        long size = 0L;
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null) continue;
                String name = asset.optString("name", "");
                if (!name.toLowerCase(Locale.US).endsWith(".apk")) continue;
                url = asset.optString("browser_download_url", null);
                size = asset.optLong("size", 0L);
                break;
            }
        }
        if (url == null || url.isEmpty()) throw new Exception("Release sin APK");

        Release release = new Release();
        release.version = version;
        release.versionCode = toCode(version);
        release.tag = tag;
        release.url = url;
        release.pageUrl = root.optString("html_url", "");
        release.size = size;
        return release;
    }

    private static void save(Context context, Release release) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit();
        editor.putLong("update_checked_at", System.currentTimeMillis());
        if (release == null) {
            editor.remove("update_version")
                    .remove("update_url")
                    .remove("update_page")
                    .remove("update_size");
        } else {
            editor.putString("update_version", release.version)
                    .putString("update_url", release.url)
                    .putString("update_page", release.pageUrl)
                    .putLong("update_size", release.size);
        }
        editor.apply();
    }

    public static int compare(String a, String b) {
        int[] left = parseVersion(a);
        int[] right = parseVersion(b);
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int x = i < left.length ? left[i] : 0;
            int y = i < right.length ? right[i] : 0;
            if (x != y) return x > y ? 1 : -1;
        }
        return 0;
    }

    private static int[] parseVersion(String value) {
        if (value == null) return new int[]{0};
        String clean = value.trim();
        if (clean.startsWith("v") || clean.startsWith("V")) clean = clean.substring(1);
        String[] parts = clean.split("[^0-9]+");
        int[] result = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            if (part.isEmpty()) continue;
            try {
                result[count++] = Integer.parseInt(part);
            } catch (Exception ignored) {
            }
        }
        if (count == 0) return new int[]{0};
        int[] trimmed = new int[count];
        System.arraycopy(result, 0, trimmed, 0, count);
        return trimmed;
    }

    private static int toCode(String version) {
        int[] parts = parseVersion(version);
        int code = 0;
        for (int i = 0; i < 3; i++) {
            code = code * 100 + (i < parts.length ? Math.min(parts[i], 99) : 0);
        }
        return code;
    }

    /** Carpeta donde el instalador del sistema puede leer el APK (Android 5/6). */
    public static File stagingDir(Context context) {
        File external = context.getExternalFilesDir(null);
        if (external != null) {
            if (!external.exists()) external.mkdirs();
            if (external.canWrite()) return external;
        }
        return context.getCacheDir();
    }

    public static void download(final Context context, final Release release,
                                final DownloadCallback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            public void run() {
                final File dir = stagingDir(app);
                final File target = new File(dir, "update.apk");
                File part = new File(dir, "update.apk.part");
                String error = null;
                try {
                    if (part.exists()) part.delete();
                    try {
                        downloadFile(app, release.url, part, null, callback, main);
                    } catch (Exception first) {
                        if (part.exists()) part.delete();
                        downloadFile(app, release.url, part, TrustedRoots.factory(app),
                                callback, main);
                    }
                    error = verify(app, part);
                    if (error == null) {
                        if (target.exists() && !target.delete()) {
                            throw new Exception("No se pudo reemplazar el APK anterior");
                        }
                        if (!part.renameTo(target)) {
                            throw new Exception("No se pudo guardar el APK");
                        }
                    }
                } catch (Exception e) {
                    error = e.getMessage() == null ? "Error de descarga" : e.getMessage();
                } finally {
                    if (error != null && part.exists()) part.delete();
                }

                final String path = error == null ? target.getAbsolutePath() : null;
                final String message = error;
                main.post(new Runnable() {
                    public void run() {
                        callback.onResult(path, message);
                    }
                });
            }
        }).start();
    }

    private static void downloadFile(Context context, String address, File target,
                                     SSLSocketFactory factory, DownloadCallback callback,
                                     Handler main) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            if (factory != null && connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(factory);
            }
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "NSPLauncher/1.5");
            connection.setRequestProperty("Accept", "application/octet-stream");

            int status = connection.getResponseCode();
            if (status != 200) throw new Exception("HTTP " + status);

            int total = connection.getContentLength();
            InputStream in = connection.getInputStream();
            OutputStream out = new FileOutputStream(target);
            try {
                byte[] buffer = new byte[16 * 1024];
                long done = 0L;
                int last = -1;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    done += read;
                    if (total > 0) {
                        int percent = (int) (done * 100 / total);
                        if (percent != last) {
                            last = percent;
                            notifyProgress(callback, main, percent);
                        }
                    }
                }
                out.flush();
            } finally {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
                in.close();
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void notifyProgress(DownloadCallback callback, Handler main,
                                       final int percent) {
        if (callback == null) return;
        main.post(new Runnable() {
            public void run() {
                callback.onProgress(percent);
            }
        });
    }

    private static String verify(Context context, File file) throws Exception {
        if (!file.exists() || file.length() < 1024) return "El APK descargado está vacío";
        PackageManager manager = context.getPackageManager();
        PackageInfo archive = manager.getPackageArchiveInfo(file.getAbsolutePath(),
                PackageManager.GET_SIGNATURES);
        if (archive == null) return "El archivo no es un APK válido";
        if (!context.getPackageName().equals(archive.packageName)) {
            return "El APK pertenece a otro paquete";
        }
        int installed = installedCode(context);
        if (archive.versionCode <= installed) {
            return "El APK no es más nuevo (código " + archive.versionCode + ")";
        }
        PackageInfo mine = manager.getPackageInfo(context.getPackageName(),
                PackageManager.GET_SIGNATURES);
        if (!sameSignature(mine.signatures, archive.signatures)) {
            return "La firma del APK no coincide";
        }
        return null;
    }

    private static boolean sameSignature(Signature[] a, Signature[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) return false;
        return a[0].toCharsString().equals(b[0].toCharsString());
    }

    /** Abre el instalador del sistema. Devuelve false si ningun intent pudo abrirse. */
    public static boolean install(Context context, String path) {
        // Android 7+: content:// con permiso temporal de lectura.
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(ApkProvider.URI,
                    "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
        }

        // Android 5/6: el instalador solo acepta file://; el sistema lo lee
        // directamente, sin permisos de almacenamiento.
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(path)),
                    "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
        }

        try {
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(Uri.fromFile(new File(path)));
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static boolean unknownSourcesEnabled(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.INSTALL_NON_MARKET_APPS, 1) != 0;
        } catch (Exception e) {
            return true;
        }
    }

    public static void openSecuritySettings(Context context) {
        try {
            context.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        } catch (Exception ignored) {
        }
    }
}
