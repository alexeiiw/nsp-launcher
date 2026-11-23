package com.nsp.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.MotionEvent;

public final class QuickKeys {

    public static final int SLOTS = 2;
    public static final int KIND_KEY = 1;
    public static final int KIND_AXIS = 2;
    public static final float AXIS_THRESHOLD = 0.5f;

    public static final int[] AXES = {
            MotionEvent.AXIS_LTRIGGER,
            MotionEvent.AXIS_RTRIGGER,
            MotionEvent.AXIS_BRAKE,
            MotionEvent.AXIS_GAS
    };

    private static final int[] RESERVED = {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_MODE,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MUTE
    };

    public static class Binding {
        public final int slot;
        public final int kind;
        public final int code;
        public final String pkg;
        public final String activity;
        public final String label;

        Binding(int slot, int kind, int code, String pkg, String activity, String label) {
            this.slot = slot;
            this.kind = kind;
            this.code = code;
            this.pkg = pkg;
            this.activity = activity;
            this.label = label;
        }

        public String describe() {
            return QuickKeys.describe(kind, code);
        }
    }

    private QuickKeys() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences("nsp", Context.MODE_PRIVATE);
    }

    public static boolean isReserved(int keyCode) {
        for (int reserved : RESERVED) {
            if (reserved == keyCode) return true;
        }
        return false;
    }

    public static boolean isTriggerAxis(int axis) {
        for (int candidate : AXES) {
            if (candidate == axis) return true;
        }
        return false;
    }

    public static String describe(int kind, int code) {
        if (kind == KIND_AXIS) {
            switch (code) {
                case MotionEvent.AXIS_LTRIGGER: return "L2 (eje)";
                case MotionEvent.AXIS_RTRIGGER: return "R2 (eje)";
                case MotionEvent.AXIS_BRAKE: return "Freno (eje)";
                case MotionEvent.AXIS_GAS: return "Acelerador (eje)";
                default: return "Eje " + code;
            }
        }
        String name = KeyEvent.keyCodeToString(code).replace("KEYCODE_", "").replace("BUTTON_", "");
        return name;
    }

    public static Binding load(Context context, int slot) {
        SharedPreferences prefs = prefs(context);
        String prefix = "qk" + slot + "_";
        int kind = prefs.getInt(prefix + "kind", 0);
        String pkg = prefs.getString(prefix + "pkg", "");
        String activity = prefs.getString(prefix + "act", "");
        if (kind == 0 || pkg.length() == 0 || activity.length() == 0) return null;
        return new Binding(slot, kind, prefs.getInt(prefix + "code", 0), pkg, activity,
                prefs.getString(prefix + "label", pkg));
    }

    public static Binding findKey(Context context, int keyCode) {
        for (int slot = 0; slot < SLOTS; slot++) {
            Binding binding = load(context, slot);
            if (binding != null && binding.kind == KIND_KEY && binding.code == keyCode) {
                return binding;
            }
        }
        return null;
    }

    public static void save(Context context, int slot, int kind, int code, AppEntry entry) {
        for (int other = 0; other < SLOTS; other++) {
            Binding binding = load(context, other);
            if (other != slot && binding != null && binding.kind == kind && binding.code == code) {
                clear(context, other);
            }
        }
        String prefix = "qk" + slot + "_";
        prefs(context).edit()
                .putInt(prefix + "kind", kind)
                .putInt(prefix + "code", code)
                .putString(prefix + "pkg", entry.pkg)
                .putString(prefix + "act", entry.activity)
                .putString(prefix + "label", entry.label)
                .apply();
    }

    public static void clear(Context context, int slot) {
        String prefix = "qk" + slot + "_";
        prefs(context).edit()
                .remove(prefix + "kind")
                .remove(prefix + "code")
                .remove(prefix + "pkg")
                .remove(prefix + "act")
                .remove(prefix + "label")
                .apply();
    }

    public static void clearAll(Context context) {
        for (int slot = 0; slot < SLOTS; slot++) clear(context, slot);
    }
}
