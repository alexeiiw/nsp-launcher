package com.nsp.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IconStore {

    public interface Listener {
        void onIcon(String pkg, Bitmap bitmap);
    }

    private final Context ctx;
    private final PackageManager pm;
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> memory;
    private final Set<String> pending = new HashSet<String>();
    private final File dir;

    public IconStore(Context context) {
        ctx = context.getApplicationContext();
        pm = ctx.getPackageManager();
        dir = new File(ctx.getCacheDir(), "icons");
        if (!dir.exists()) dir.mkdirs();
        memory = new LruCache<String, Bitmap>(8 * 1024) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    public Bitmap get(String pkg) {
        return memory.get(pkg);
    }

    public void invalidate(final String pkg) {
        memory.remove(pkg);
        pending.remove(pkg);
        final File directory = dir;
        pool.execute(new Runnable() {
            public void run() {
                File[] files = directory.listFiles();
                if (files == null) return;
                String prefix = safe(pkg) + "_";
                for (File f : files) {
                    if (f.getName().startsWith(prefix)) f.delete();
                }
            }
        });
    }

    public void request(final String pkg, final String activity, final long updated,
                        final Listener listener) {
        Bitmap cached = memory.get(pkg);
        if (cached != null) {
            listener.onIcon(pkg, cached);
            return;
        }
        if (pending.contains(pkg)) return;
        pending.add(pkg);

        pool.execute(new Runnable() {
            public void run() {
                Bitmap bitmap = loadFromDisk(pkg, updated);
                if (bitmap == null) {
                    bitmap = loadFromPackage(pkg, activity);
                    if (bitmap != null) saveToDisk(pkg, updated, bitmap);
                }
                if (bitmap != null) memory.put(pkg, bitmap);
                final Bitmap result = bitmap;
                main.post(new Runnable() {
                    public void run() {
                        pending.remove(pkg);
                        if (result != null) listener.onIcon(pkg, result);
                    }
                });
            }
        });
    }

    private String safe(String pkg) {
        StringBuilder builder = new StringBuilder(pkg.length());
        for (int i = 0; i < pkg.length(); i++) {
            char c = pkg.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    private File iconFile(String pkg, long updated) {
        return new File(dir, safe(pkg) + "_" + updated + ".png");
    }

    private Bitmap loadFromDisk(String pkg, long updated) {
        try {
            File file = iconFile(pkg, updated);
            if (!file.exists()) return null;
            return android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Throwable t) {
            return null;
        }
    }

    private Bitmap loadFromPackage(String pkg, String activity) {
        Drawable drawable = null;
        try {
            drawable = pm.getActivityIcon(new ComponentName(pkg, activity));
        } catch (Throwable ignored) {
        }
        if (drawable == null) {
            try {
                drawable = pm.getApplicationIcon(pkg);
            } catch (Throwable ignored) {
                return null;
            }
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, 96, 96);
            drawable.draw(canvas);
            return bitmap;
        } catch (Throwable t) {
            return null;
        }
    }

    private void saveToDisk(String pkg, long updated, Bitmap bitmap) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(iconFile(pkg, updated));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
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
}
