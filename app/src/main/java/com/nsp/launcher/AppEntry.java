package com.nsp.launcher;

public class AppEntry {

    public final String pkg;
    public final String activity;
    public final String label;
    public long updated;
    public String category;

    public AppEntry(String pkg, String activity, String label) {
        this.pkg = pkg;
        this.activity = activity;
        this.label = label;
    }
}
