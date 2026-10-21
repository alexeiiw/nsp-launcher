package com.nsp.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NotificationStore {

    public interface Listener {
        void onBadgeChanged(String pkg, int count);
    }

    private static final Map<String, Integer> COUNTS = new HashMap<String, Integer>();
    private static final List<Listener> LISTENERS = new ArrayList<Listener>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private NotificationStore() {
    }

    public static int count(String pkg) {
        synchronized (COUNTS) {
            Integer value = COUNTS.get(pkg);
            return value == null ? 0 : value;
        }
    }

    public static void setCount(final String pkg, int count) {
        if (count < 0) count = 0;
        synchronized (COUNTS) {
            if (count == 0) {
                COUNTS.remove(pkg);
            } else {
                COUNTS.put(pkg, count);
            }
        }
        final int value = count;
        MAIN.post(new Runnable() {
            public void run() {
                notifyListeners(pkg, value);
            }
        });
    }

    public static void clearAll() {
        final ArrayList<String> packages;
        synchronized (COUNTS) {
            packages = new ArrayList<String>(COUNTS.keySet());
            COUNTS.clear();
        }
        for (final String pkg : packages) {
            MAIN.post(new Runnable() {
                public void run() {
                    notifyListeners(pkg, 0);
                }
            });
        }
    }

    private static void notifyListeners(String pkg, int count) {
        Listener[] listeners;
        synchronized (LISTENERS) {
            listeners = LISTENERS.toArray(new Listener[LISTENERS.size()]);
        }
        for (Listener listener : listeners) {
            listener.onBadgeChanged(pkg, count);
        }
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
