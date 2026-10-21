package com.nsp.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public final class HomeUtil {

    private static final String ALIAS = "com.nsp.launcher.HomeAlias";

    private HomeUtil() {
    }

    public static ComponentName alias(Context context) {
        return new ComponentName(context.getPackageName(), ALIAS);
    }

    public static boolean isAliasEnabled(Context context) {
        return context.getPackageManager().getComponentEnabledSetting(alias(context))
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    public static void setAliasEnabled(Context context, boolean enabled) {
        context.getPackageManager().setComponentEnabledSetting(alias(context),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static boolean isDefaultHome(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo info = context.getPackageManager()
                    .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return info != null && info.activityInfo != null
                    && context.getPackageName().equals(info.activityInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }
}
