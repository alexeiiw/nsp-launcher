package com.nsp.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Lector de RSS. Descarga en segundo plano, reintenta con las raices empaquetadas
 * cuando el almacen del sistema (2015) no conoce la CA del sitio y conserva una
 * cache corta para la tarjeta de Noticias y para el propio lector.
 */
public final class RssService {

    public interface Callback {
        void done(ArrayList<RssItem> items, String error);
    }

    public static final String[] NAMES = {"Mundo", "Deportes", "Guatemala", "Fútbol"};

    private static final String[] URLS = {
            "https://feeds.bbci.co.uk/mundo/rss.xml",
            "https://www.espn.com/espn/rss/news",
            "https://news.google.com/rss/search?q=Guatemala&hl=es-419&gl=GT&ceid=GT:es-419",
            "https://news.google.com/rss/search?q=futbol&hl=es-419&gl=GT&ceid=GT:es-419"
    };

    private static final long MAX_AGE = 10L * 60L * 1000L;
    private static final int MAX_ITEMS = 12;

    @SuppressWarnings("unchecked")
    private static final ArrayList<RssItem>[] CACHE = new ArrayList[4];
    private static final long[] CACHED_AT = new long[4];
    private static final String[] ERRORS = new String[4];

    private RssService() {
    }

    public static ArrayList<RssItem> peek(int cat) {
        if (cat < 0 || cat >= CACHE.length) return null;
        return CACHE[cat];
    }

    public static String lastError(int cat) {
        if (cat < 0 || cat >= ERRORS.length) return null;
        return ERRORS[cat];
    }

    public static void load(final Context context, final int cat, final Callback callback) {
        if (cat < 0 || cat >= URLS.length) return;
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        ArrayList<RssItem> cached = CACHE[cat];
        if (cached != null && !cached.isEmpty()
                && System.currentTimeMillis() - CACHED_AT[cat] < MAX_AGE) {
            final ArrayList<RssItem> copy = new ArrayList<RssItem>(cached);
            main.post(new Runnable() {
                public void run() {
                    callback.done(copy, null);
                }
            });
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                ArrayList<RssItem> items = new ArrayList<RssItem>();
                String error = null;
                try {
                    String body;
                    try {
                        body = fetch(app, URLS[cat], null);
                    } catch (Exception first) {
                        body = fetch(app, URLS[cat], TrustedRoots.factory(app));
                    }
                    items = parse(body, NAMES[cat]);
                    if (items.isEmpty()) error = "La fuente no devolvió titulares";
                } catch (Exception e) {
                    error = e.getMessage() == null ? "Sin conexión" : e.getMessage();
                }

                if (!items.isEmpty()) {
                    CACHE[cat] = items;
                    CACHED_AT[cat] = System.currentTimeMillis();
                    ERRORS[cat] = null;
                } else {
                    ERRORS[cat] = error;
                }

                final ArrayList<RssItem> result =
                        items.isEmpty() && CACHE[cat] != null ? CACHE[cat] : items;
                final String message = error;
                main.post(new Runnable() {
                    public void run() {
                        callback.done(new ArrayList<RssItem>(result), message);
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
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent",
                    "NSPLauncher/1.5 (+https://github.com/alexeiiw/nsp-launcher)");
            connection.setRequestProperty("Accept",
                    "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.8");

            int status = connection.getResponseCode();
            if (status != 200) throw new Exception("HTTP " + status);

            InputStream in = connection.getInputStream();
            try {
                return read(in);
            } finally {
                in.close();
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line).append('\n');
        reader.close();
        return builder.toString();
    }

    private static ArrayList<RssItem> parse(String body, String source) throws Exception {
        ArrayList<RssItem> out = new ArrayList<RssItem>();
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        } catch (Exception ignored) {
        }
        parser.setInput(new StringReader(body));

        RssItem item = null;
        String tag = null;
        StringBuilder text = null;

        int event = parser.getEventType();
        try {
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("item".equals(name) || "entry".equals(name)) {
                        item = new RssItem();
                        tag = null;
                        text = null;
                    } else if (item != null) {
                        // Cada etiqueta abre su propio acumulador: el texto de una
                        // etiqueta nunca puede sobrescribir el de la anterior.
                        tag = name;
                        text = new StringBuilder();
                    }
                } else if ((event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT)
                        && item != null && tag != null) {
                    if (text != null) text.append(parser.getText());
                } else if (event == XmlPullParser.END_TAG) {
                    String name = parser.getName();
                    if (("item".equals(name) || "entry".equals(name)) && item != null) {
                        finishItem(item, source, out);
                        item = null;
                        tag = null;
                        text = null;
                    } else if (item != null && name.equals(tag)) {
                        String value = text == null ? "" : text.toString().trim();
                        if ("title".equals(tag)) {
                            item.title = clean(value);
                        } else if ("link".equals(tag)) {
                            if (!value.isEmpty()) item.link = value;
                        } else if ("description".equals(tag) || "summary".equals(tag)) {
                            item.description = clean(value);
                        } else if ("pubDate".equals(tag) || "published".equals(tag)
                                || "updated".equals(tag)) {
                            item.published = parseDate(value);
                        }
                        tag = null;
                        text = null;
                    }
                }
                event = parser.next();
            }
        } catch (StopSignal ignored) {
            // Ya tenemos los 12 titulares que muestra la lista.
        }
        return out;
    }

    private static void finishItem(RssItem item, String source, ArrayList<RssItem> out) {
        if (item.title == null || item.title.trim().isEmpty()) return;
        if (item.link == null || item.link.trim().isEmpty()) return;
        item.source = source;
        out.add(item);
        if (out.size() >= MAX_ITEMS) throw new StopSignal();
    }

    /** Excepcion de control para cortar el parser tras 12 elementos. */
    private static final class StopSignal extends RuntimeException {
    }

    private static String clean(String value) {
        if (value == null) return "";
        String text;
        try {
            text = Html.fromHtml(value).toString();
        } catch (Exception e) {
            text = value.replaceAll("<[^>]+>", " ");
        }
        return text.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static long parseDate(String value) {
        if (value == null || value.isEmpty()) return 0L;
        String[] patterns = {
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
                format.setLenient(true);
                Date date = format.parse(value);
                if (date != null) return date.getTime();
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }
}
