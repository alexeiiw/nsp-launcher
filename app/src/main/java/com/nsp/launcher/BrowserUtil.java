package com.nsp.launcher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Abre enlaces en Chrome o Firefox; si no estan, usa el selector del sistema. */
public final class BrowserUtil {

    private static final String[] PACKAGES = {
            "com.android.chrome",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "com.android.browser"
    };

    private BrowserUtil() {
    }

    public static boolean open(Context context, String url) {
        if (url == null || url.isEmpty()) return false;
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        for (String pkg : PACKAGES) {
            Intent direct = new Intent(view);
            direct.setPackage(pkg);
            try {
                if (context.getPackageManager().resolveActivity(direct, 0) != null) {
                    context.startActivity(direct);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        try {
            context.startActivity(view);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String searchUrl(String query) {
        return "https://www.google.com/search?q=" + Uri.encode(query);
    }
}
