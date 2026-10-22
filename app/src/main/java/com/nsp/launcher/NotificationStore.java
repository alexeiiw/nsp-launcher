package com.nsp.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NotificationStore {

    public interface Listener {
        void onBadgeChanged(String pkg, int count);
    }

    private static final Map<String, Set<String>> KEYS = new HashMap<String, Set<String>>();
    private static final List<Listener> LISTENERS = new ArrayList<Listener>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private NotificationStore() {
    }

    /** Reemplaza el conjunto de notificaciones vivas de un paquete. */
    public static void setKeys(String pkg, Set<String> keys) {
        int count;
        synchronized (KEYS) {
            if (keys == null || keys.isEmpty()) {
                KEYS.remove(pkg);
                count = 0;
            } else {
                KEYS.put(pkg, new HashSet<String>(keys));
                count = keys.size();
            }
        }
        notifyListeners(pkg, count);
    }

    public static int count(String pkg) {
        synchronized (KEYS) {
            Set<String> set = KEYS.get(pkg);
            return set == null ? 0 : set.size();
        }
    }

    public static int total() {
        synchronized (KEYS) {
            int total = 0;
            for (Set<String> set : KEYS.values()) total += set.size();
            return total;
        }
    }

    /** Paquetes con notificaciones, de más a menos. */
    public static List<String> packages() {
        ArrayList<String> list = new ArrayList<String>();
        synchronized (KEYS) {
            for (Map.Entry<String, Set<String>> entry : KEYS.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    list.add(entry.getKey());
                }
            }
        }
        Collections.sort(list, new Comparator<String>() {
            public int compare(String a, String b) {
                int ca = count(a);
                int cb = count(b);
                if (ca != cb) return cb - ca;
                return a.compareTo(b);
            }
        });
        return list;
    }

    /** Descarta las notificaciones de un paquete. */
    public static void clear(String pkg) {
        Set<String> keys;
        synchronized (KEYS) {
            Set<String> set = KEYS.get(pkg);
            keys = set == null ? null : new HashSet<String>(set);
            KEYS.remove(pkg);
        }
        if (keys != null) {
            for (String key : keys) {
                NotificationService.cancelKey(key);
            }
        }
        notifyListeners(pkg, 0);
    }

    /** Descarta todas las notificaciones conocidas. */
    public static void clearAll() {
        ArrayList<String> packages = new ArrayList<String>();
        synchronized (KEYS) {
            for (String pkg : KEYS.keySet()) packages.add(pkg);
        }
        for (String pkg : packages) clear(pkg);
    }

    /** Vacía el almacén sin cancelar (el servicio se desconectó). */
    public static void reset() {
        ArrayList<String> packages;
        synchronized (KEYS) {
            packages = new ArrayList<String>(KEYS.keySet());
            KEYS.clear();
        }
        for (String pkg : packages) {
            notifyListeners(pkg, 0);
        }
    }

    private static void notifyListeners(final String pkg, final int count) {
        MAIN.post(new Runnable() {
            public void run() {
                Listener[] copy;
                synchronized (LISTENERS) {
                    copy = LISTENERS.toArray(new Listener[LISTENERS.size()]);
                }
                for (Listener listener : copy) {
                    listener.onBadgeChanged(pkg, count);
                }
            }
        });
    }

    public static void addListener(Listener listener) {
        synchronized (LISTENERS) {
            if (!LISTENERS.contains(listener)) LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        synchronized (LISTENERS) {
            LISTENERS.remove(listener);
        }
    }

    public static boolean hasAccess(Context context) {
        try {
            String enabled = Settings.Secure.getString(context.getContentResolver(),
                    "enabled_notification_listeners");
            return enabled != null && enabled.contains(context.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
}
