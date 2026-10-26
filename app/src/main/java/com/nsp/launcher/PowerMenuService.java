package com.nsp.launcher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

/**
 * Servicio de accesibilidad mínimo: su única función es abrir el diálogo de
 * energía del sistema (Apagar/Reiniciar) cuando se lo piden desde Ajustes.
 * No observa ni lee contenido de la pantalla.
 */
public class PowerMenuService extends AccessibilityService {

    private static PowerMenuService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    public static boolean serviceConnected() {
        return instance != null;
    }

    /** ¿Está el servicio habilitado por el usuario en Ajustes de Acceso? */
    public static boolean isEnabled(Context context) {
        try {
            AccessibilityManager manager = (AccessibilityManager) context
                    .getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (manager == null) return false;
            List<AccessibilityServiceInfo> active = manager
                    .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (AccessibilityServiceInfo info : active) {
                if (info.getId() != null && info.getId().endsWith(".PowerMenuService")) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void openPowerMenu() {
        if (instance == null) return;
        try {
            instance.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
        } catch (Exception ignored) {
        }
    }
}
