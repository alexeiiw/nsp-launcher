package com.nsp.launcher;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NotificationService extends NotificationListenerService {

    private final Map<String, Set<String>> keys = new HashMap<String, Set<String>>();

    private boolean isCounted(StatusBarNotification sbn) {
        if (sbn == null) return false;
        if (sbn.isOngoing()) return false;
        Notification notification = sbn.getNotification();
        return notification == null
                || (notification.flags & Notification.FLAG_GROUP_SUMMARY) == 0;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!isCounted(sbn)) return;
        String pkg = sbn.getPackageName();
        String key = sbn.getKey();
        if (pkg == null || key == null) return;

        Set<String> set = keys.get(pkg);
        if (set == null) {
            set = new HashSet<String>();
            keys.put(pkg, set);
        }
        if (set.add(key)) {
            NotificationStore.setCount(pkg, set.size());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();
        String key = sbn.getKey();
        if (pkg == null || key == null) return;

        Set<String> set = keys.get(pkg);
        if (set == null) return;
        set.remove(key);
        if (set.isEmpty()) keys.remove(pkg);
        NotificationStore.setCount(pkg, set.size());
    }

    @Override
    public void onListenerDisconnected() {
        keys.clear();
        NotificationStore.clearAll();
    }
}
