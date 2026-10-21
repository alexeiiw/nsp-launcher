package com.nsp.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AppRepository {

    public static final String FAV = "Favoritos";
    public static final String RECENT = "Recientes";

    public static final String[] CATS = {
            "Juegos",
            "Emuladores",
            "Multimedia",
            "Internet",
            "Herramientas",
            "Otros"
    };

    private static final String[] K_EMU = {
            "emu", "retroarch", "ppsspp", "dolphin", "mupen", "epsxe", "snes",
            "gba", "gbc", "n64", "nds", "desmume", "melon", "duckstation",
            "pcsx", "reicast", "flycast", "citra", "drastic", "myboy", "mame",
            "fba", "scummvm", "dosbox", "atari", "amiga", "vice", "cemu", "aether",
            "redream", "yaba", "john", "gens", "kega", "emulador"
    };

    private static final String[] K_MEDIA = {
            "youtube", "netflix", "kodi", "plex", "vlc", "mx player", "spotify",
            "music", "musica", "gallery", "galeria", "photo", "foto", "twitch",
            "iptv", "player", "video", "movie", "prime video", "disney", "hbo",
            "tubi", "podcast", "radio", "cast", "reproductor"
    };

    private static final String[] K_NET = {
            "chrome", "browser", "navegador", "firefox", "opera", "edge", "gmail",
            "mail", "correo", "facebook", "twitter", "reddit", "discord", "telegram",
            "whatsapp", "skype", "search", "drive", "dropbox", "maps", "teams", "zoom"
    };

    private static final String[] K_GAME = {
            "game", "juego", "half-life", "portal", "minecraft", "terraria", "doom",
            "quake", "sonic", "gta", "asphalt", "racing", "raider", "buggy", "bomb",
            "geforce", "nvidia games", "grid", "lego", "angry", "cut the rope",
            "monument", "limbo", "dead trigger", "clash", "kingdom", "star wars",
            "batman", "mortal kombat", "street fighter", "metal slug", "castlevania",
            "contra", "duke", "wolfenstein", "tomb", "hitman", "need for speed",
            "real racing", "beach", "subway", "candy", "puzzle", "mahjong", "chess",
            "solitaire"
    };

    private static final String[] K_TOOLS = {
            "setting", "ajuste", "file", "archivo", "explorer", "explorador",
            "calculator", "calculadora", "clock", "reloj", "calendar", "calendario",
            "terminal", "adb", "wifi", "vpn", "root", "busybox", "magisk", "store",
            "vending", "package", "speedtest", "titanium", "keyboard", "teclado",
            "nvidia", "shield", "hub", "update", "actualiz", "backup", "copia"
    };

    private final Context ctx;
    private final SharedPreferences prefs;

    public AppRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.prefs = this.ctx.getSharedPreferences("nsp", Context.MODE_PRIVATE);
    }

    public static int colorFor(String title) {
        if (FAV.equals(title)) return 0xFF00ABA9;
        if (RECENT.equals(title)) return 0xFF825A2C;
        if ("Juegos".equals(title)) return 0xFF1BA1E2;
        if ("Emuladores".equals(title)) return 0xFFA200FF;
        if ("Multimedia".equals(title)) return 0xFFD80073;
        if ("Internet".equals(title)) return 0xFF0050EF;
        if ("Herramientas".equals(title)) return 0xFF60A917;
        return 0xFF647687;
    }

    public ArrayList<AppEntry> load() {
        PackageManager pm = ctx.getPackageManager();
        Map<String, Long> updates = new HashMap<String, Long>();
        try {
            for (PackageInfo info : pm.getInstalledPackages(0)) {
                updates.put(info.packageName, info.lastUpdateTime);
            }
        } catch (Exception ignored) {
        }

        LinkedHashMap<String, AppEntry> unique = new LinkedHashMap<String, AppEntry>();
        collect(pm, Intent.CATEGORY_LAUNCHER, unique, updates);
        collect(pm, "android.intent.category.LEANBACK_LAUNCHER", unique, updates);

        ArrayList<AppEntry> list = new ArrayList<AppEntry>(unique.values());
        for (AppEntry e : list) {
            e.category = categoryOf(e);
        }

        final Collator collator = Collator.getInstance(new Locale("es", "ES"));
        Collections.sort(list, new Comparator<AppEntry>() {
            public int compare(AppEntry a, AppEntry b) {
                return collator.compare(a.label, b.label);
            }
        });
        return list;
    }

    public Set<String> installedPackages() {
        PackageManager pm = ctx.getPackageManager();
        Set<String> out = new HashSet<String>();
        collectPackages(pm, Intent.CATEGORY_LAUNCHER, out);
        collectPackages(pm, "android.intent.category.LEANBACK_LAUNCHER", out);
        out.remove(ctx.getPackageName());
        return out;
    }

    private void collectPackages(PackageManager pm, String category, Set<String> out) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(category);
            List<ResolveInfo> found = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo ri : found) {
                if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                    out.add(ri.activityInfo.packageName);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void collect(PackageManager pm, String category,
                         LinkedHashMap<String, AppEntry> out, Map<String, Long> updates) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(category);
            List<ResolveInfo> found = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo ri : found) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg == null || pkg.equals(ctx.getPackageName())) continue;
                if (out.containsKey(pkg)) continue;
                CharSequence cs = ri.loadLabel(pm);
                String label = cs == null ? pkg : cs.toString().trim();
                if (label.length() == 0) label = pkg;
                AppEntry entry = new AppEntry(pkg, ri.activityInfo.name, label);
                Long updated = updates.get(pkg);
                entry.updated = updated == null ? 0L : updated;
                out.put(pkg, entry);
            }
        } catch (Exception ignored) {
        }
    }

    private File cacheFile() {
        return new File(ctx.getFilesDir(), "apps_cache.json");
    }

    public ArrayList<AppEntry> readCache() {
        File file = cacheFile();
        if (!file.exists()) return new ArrayList<AppEntry>();
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, read);
            }
            reader.close();
            input = null;

            JSONArray array = new JSONArray(builder.toString());
            ArrayList<AppEntry> list = new ArrayList<AppEntry>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String pkg = o.optString("p", "");
                String activity = o.optString("a", "");
                String label = o.optString("l", "");
                if (pkg.length() == 0 || activity.length() == 0 || label.length() == 0) continue;
                AppEntry entry = new AppEntry(pkg, activity, label);
                entry.updated = o.optLong("u", 0L);
                entry.category = categoryOf(entry);
                list.add(entry);
            }
            return list;
        } catch (Throwable t) {
            return new ArrayList<AppEntry>();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void writeCache(List<AppEntry> list) {
        File tmp = new File(ctx.getFilesDir(), "apps_cache.tmp");
        File target = cacheFile();
        FileOutputStream output = null;
        try {
            JSONArray array = new JSONArray();
            for (AppEntry e : list) {
                JSONObject o = new JSONObject();
                o.put("p", e.pkg);
                o.put("a", e.activity);
                o.put("l", e.label);
                o.put("u", e.updated);
                array.put(o);
            }
            output = new FileOutputStream(tmp);
            OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
            writer.write(array.toString());
            writer.close();
            output = null;

            if (target.exists() && !target.delete()) {
                return;
            }
            tmp.renameTo(target);
        } catch (Throwable ignored) {
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public String categoryOf(AppEntry e) {
        String override = prefs.getString("cat:" + e.pkg, null);
        if (override != null && override.length() > 0) return override;
        String haystack = normalize(e.pkg + " " + e.label);
        String[] tokens = haystack.length() == 0 ? new String[0] : haystack.split(" ");
        if (matches(haystack, tokens, K_EMU)) return "Emuladores";
        if (matches(haystack, tokens, K_MEDIA)) return "Multimedia";
        if (matches(haystack, tokens, K_NET)) return "Internet";
        if (matches(haystack, tokens, K_GAME)) return "Juegos";
        if (matches(haystack, tokens, K_TOOLS)) return "Herramientas";
        return "Otros";
    }

    private static String normalize(String s) {
        StringBuilder b = new StringBuilder(s.length());
        boolean pendingSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (pendingSpace && b.length() > 0) b.append(' ');
                pendingSpace = false;
                b.append(fold(Character.toLowerCase(c)));
            } else {
                pendingSpace = true;
            }
        }
        return b.toString();
    }

    private static char fold(char c) {
        switch (c) {
            case 'á': case 'à': case 'ä': case 'â': return 'a';
            case 'é': case 'è': case 'ë': case 'ê': return 'e';
            case 'í': case 'ì': case 'ï': case 'î': return 'i';
            case 'ó': case 'ò': case 'ö': case 'ô': return 'o';
            case 'ú': case 'ù': case 'ü': case 'û': return 'u';
            case 'ñ': return 'n';
            default: return c;
        }
    }

    private static boolean matches(String haystack, String[] tokens, String[] keywords) {
        for (String raw : keywords) {
            String k = normalize(raw);
            if (k.length() == 0) continue;
            if (k.indexOf(' ') >= 0) {
                if (haystack.contains(k)) return true;
            } else {
                for (String token : tokens) {
                    if (token.startsWith(k)) return true;
                }
            }
        }
        return false;
    }

    public void setCategory(String pkg, String category) {
        prefs.edit().putString("cat:" + pkg, category).apply();
    }

    public void clearCategory(String pkg) {
        prefs.edit().remove("cat:" + pkg).apply();
    }

    public Set<String> favorites() {
        return new HashSet<String>(prefs.getStringSet("fav", Collections.<String>emptySet()));
    }

    public boolean isFavorite(String pkg) {
        return prefs.getStringSet("fav", Collections.<String>emptySet()).contains(pkg);
    }

    public boolean toggleFavorite(String pkg) {
        Set<String> set = favorites();
        boolean added;
        if (set.contains(pkg)) {
            set.remove(pkg);
            added = false;
        } else {
            set.add(pkg);
            added = true;
        }
        prefs.edit().putStringSet("fav", set).apply();
        return added;
    }

    public ArrayList<String> recentPackages() {
        String raw = prefs.getString("recent", "");
        ArrayList<String> list = new ArrayList<String>();
        if (raw.length() == 0) return list;
        String[] parts = raw.split("\\|");
        for (String p : parts) {
            if (p.length() > 0) list.add(p);
        }
        return list;
    }

    public void recordLaunch(String pkg) {
        ArrayList<String> list = recentPackages();
        list.remove(pkg);
        list.add(0, pkg);
        while (list.size() > 8) list.remove(list.size() - 1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) builder.append('|');
            builder.append(list.get(i));
        }
        prefs.edit().putString("recent", builder.toString()).apply();
    }
}
