package com.nsp.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class BackgroundManager {

    public static final int STYLE_AUTO = 0;
    public static final int STYLE_DARK = 1;
    public static final int STYLE_LIGHT = 2;

    private static final String PREFS = "nsp";
    private static final String FILE_NAME = "custom_background.jpg";
    private static final int TARGET_W = 1280;
    private static final int TARGET_H = 720;

    private BackgroundManager() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static File customFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    public static boolean hasCustom(Context context) {
        File file = customFile(context);
        return file.exists() && file.length() > 0L;
    }

    public static int style(Context context) {
        return prefs(context).getInt("bg_style", STYLE_AUTO);
    }

    public static void setStyle(Context context, int style) {
        prefs(context).edit().putInt("bg_style", style).apply();
    }

    public static float luminance(Context context) {
        return prefs(context).getFloat("bg_luminance", 0.5f);
    }

    public static boolean effectiveLight(Context context) {
        if (!hasCustom(context)) {
            return prefs(context).getBoolean("light", false);
        }
        int style = style(context);
        if (style == STYLE_DARK) return false;
        if (style == STYLE_LIGHT) return true;
        return luminance(context) >= 0.55f;
    }

    public static void clearCustom(Context context) {
        customFile(context).delete();
        prefs(context).edit().remove("bg_luminance").putBoolean("bg_changed", true).apply();
    }

    public static Bitmap load(Context context) {
        if (!hasCustom(context)) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(customFile(context).getAbsolutePath(), options);
        } catch (Throwable t) {
            return null;
        }
    }

    public static float importImage(Context context, Uri uri) throws Exception {
        File temp = new File(context.getCacheDir(), "bg_import.tmp");
        InputStream input = null;
        FileOutputStream output = null;
        Bitmap bitmap = null;
        try {
            input = context.getContentResolver().openInputStream(uri);
            if (input == null) throw new Exception("No se pudo abrir la imagen");
            output = new FileOutputStream(temp);
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) > 0) output.write(buffer, 0, read);
            output.close();
            output = null;
            input.close();
            input = null;

            bitmap = decodeSampled(temp);
            if (bitmap == null) throw new Exception("Formato no compatible");

            int orientation = readOrientation(temp);
            bitmap = rotate(bitmap, orientation);

            Bitmap cropped = centerCrop(bitmap);
            float luminance = averageLuminance(cropped);

            FileOutputStream fileOutput = new FileOutputStream(customFile(context));
            try {
                cropped.compress(Bitmap.CompressFormat.JPEG, 90, fileOutput);
            } finally {
                fileOutput.close();
            }
            if (cropped != bitmap) cropped.recycle();

            prefs(context).edit()
                    .putFloat("bg_luminance", luminance)
                    .putBoolean("bg_changed", true)
                    .apply();
            return luminance;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (Exception ignored) {
                }
            }
            if (bitmap != null) bitmap.recycle();
            temp.delete();
        }
    }

    private static Bitmap decodeSampled(File file) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > 1920) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private static int readOrientation(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        } catch (Throwable t) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    private static Bitmap rotate(Bitmap bitmap, int orientation) {
        int degrees;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
            default:
                return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) bitmap.recycle();
        return rotated;
    }

    private static Bitmap centerCrop(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.max((float) TARGET_W / width, (float) TARGET_H / height);
        int scaledWidth = Math.max(TARGET_W, Math.round(width * scale));
        int scaledHeight = Math.max(TARGET_H, Math.round(height * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        int x = (scaledWidth - TARGET_W) / 2;
        int y = (scaledHeight - TARGET_H) / 2;
        Bitmap result = Bitmap.createBitmap(scaled, x, y, TARGET_W, TARGET_H);
        if (scaled != result && scaled != bitmap) scaled.recycle();
        return result;
    }

    private static float averageLuminance(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int stepX = Math.max(1, width / 80);
        int stepY = Math.max(1, height / 45);
        long sum = 0L;
        long count = 0L;
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                sum += (r * 299 + g * 587 + b * 114) / 1000;
                count++;
            }
        }
        if (count == 0L) return 0.5f;
        return (sum / (float) count) / 255f;
    }
}
