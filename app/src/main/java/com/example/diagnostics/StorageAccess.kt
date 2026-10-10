package com.example.diagnostics

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * Prüft/fordert den Zugriff auf den öffentlichen Speicher (/storage/emulated/0/CodeAssistLSP).
 *
 * - Android 11+ (API 30+): "Zugriff auf alle Dateien" (MANAGE_EXTERNAL_STORAGE), per Einstellungsseite.
 * - Android 8–10: klassische WRITE_EXTERNAL_STORAGE-Laufzeitberechtigung.
 */
object StorageAccess {

    const val REQUEST_CODE = 4711

    fun hasPublicStorageAccess(context: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    } catch (_: Throwable) {
        false
    }

    /** Öffnet die passende Systemseite bzw. den Berechtigungsdialog. */
    fun request(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uri = Uri.parse("package:${activity.packageName}")
                try {
                    activity.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                    )
                } catch (_: Throwable) {
                    activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                activity.requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE
                )
            }
        } catch (t: Throwable) {
            AppLogger.e("StorageAccess", "Speicherzugriff konnte nicht angefordert werden", t)
        }
    }
}
