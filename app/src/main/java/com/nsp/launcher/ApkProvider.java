package com.nsp.launcher;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Sirve el APK descargado como content:// para que el instalador pueda leerlo. */
public class ApkProvider extends ContentProvider {

    public static final String AUTHORITY = "com.nsp.launcher.apk";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/update.apk");

    @Override
    public boolean onCreate() {
        return true;
    }

    private File file() {
        File external = getContext().getExternalFilesDir(null);
        if (external != null) {
            File candidate = new File(external, "update.apk");
            if (candidate.exists()) return candidate;
        }
        return new File(getContext().getCacheDir(), "update.apk");
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        File apk = file();
        MatrixCursor cursor = new MatrixCursor(new String[]{
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        cursor.addRow(new Object[]{"NSPLauncher-update.apk", apk.length()});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(), ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
