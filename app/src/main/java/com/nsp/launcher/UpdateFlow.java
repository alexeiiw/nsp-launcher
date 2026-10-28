package com.nsp.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.widget.Toast;

/** Dialogos de actualizacion compartidos por Ajustes y CASE. */
public final class UpdateFlow {

    private UpdateFlow() {
    }

    public static void checkPrompt(final Activity activity) {
        checkPrompt(activity, null);
    }

    public static void checkPrompt(final Activity activity, final Runnable onFinished) {
        Toast.makeText(activity, "Buscando actualización…", Toast.LENGTH_SHORT).show();
        UpdateService.check(activity, true, new UpdateService.CheckCallback() {
            public void onResult(UpdateService.Release release, String error) {
                if (release == null) {
                    Toast.makeText(activity,
                            error == null ? "NSP está al día" : "No se pudo comprobar: " + error,
                            Toast.LENGTH_LONG).show();
                } else {
                    prompt(activity, release);
                }
                if (onFinished != null) onFinished.run();
            }
        });
    }

    private static void prompt(final Activity activity, final UpdateService.Release release) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("NSP " + release.version + " disponible");
        builder.setMessage("Instalada: " + UpdateService.installedVersion(activity)
                + "\nNueva: " + release.version
                + (release.size > 0 ? "\nTamaño: " + (release.size / 1024) + " KB" : "")
                + "\n\nAndroid pedirá confirmación para instalar.");
        builder.setPositiveButton("Descargar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                download(activity, release);
            }
        });
        builder.setNegativeButton("Ahora no", null);

        final AlertDialog dialog = builder.create();
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                    d.dismiss();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    private static void download(final Activity activity, final UpdateService.Release release) {
        final AlertDialog progress = new AlertDialog.Builder(activity)
                .setTitle("Descargando NSP " + release.version)
                .setMessage("0 %")
                .setCancelable(false)
                .create();
        progress.show();

        UpdateService.download(activity, release, new UpdateService.DownloadCallback() {
            public void onProgress(int percent) {
                progress.setMessage(percent + " %");
            }

            public void onResult(String path, String error) {
                try {
                    progress.dismiss();
                } catch (Exception ignored) {
                }
                if (path == null) {
                    error(activity, release, error);
                    return;
                }
                if (!UpdateService.unknownSourcesEnabled(activity)) {
                    unknownSources(activity);
                    return;
                }
                if (!UpdateService.install(activity, path)) {
                    error(activity, release, "No se pudo abrir el instalador");
                }
            }
        });
    }

    private static void error(final Activity activity, final UpdateService.Release release,
                              String error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("No se pudo actualizar");
        builder.setMessage(error == null ? "Error desconocido" : error);
        builder.setPositiveButton("Abrir Release", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                BrowserUtil.open(activity, release.pageUrl);
            }
        });
        builder.setNegativeButton("Cerrar", null);
        builder.show();
    }

    private static void unknownSources(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Permitir instalación");
        builder.setMessage("El APK ya está descargado. Activa «Orígenes desconocidos» en "
                + "Seguridad para que Android pueda instalarlo.");
        builder.setPositiveButton("Abrir Seguridad", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                UpdateService.openSecuritySettings(activity);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}
