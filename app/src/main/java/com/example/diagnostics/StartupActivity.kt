package com.example.diagnostics

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity

/**
 * Launcher-Einstieg vor der eigentlichen App. Fragt (einmalig pro Start, solange nicht erteilt)
 * den Speicherzugriff für /storage/emulated/0/CodeAssistLSP ab, BEVOR MainActivity (und damit
 * ein möglicher Absturz) startet. Bewusst nur Framework-Views, keine eigene Logik.
 */
class StartupActivity : Activity() {

    private var skipped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i(TAG, "StartupActivity.onCreate, Speicherzugriff=${StorageAccess.hasPublicStorageAccess(this)}")
        if (StorageAccess.hasPublicStorageAccess(this) || SKIPPED_THIS_PROCESS) {
            launchMain()
            return
        }
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        if (!skipped && StorageAccess.hasPublicStorageAccess(this)) {
            AppLogger.refreshStorage()
            launchMain()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (StorageAccess.hasPublicStorageAccess(this)) {
            AppLogger.refreshStorage()
        }
        launchMain()
    }

    private fun launchMain() {
        AppLogger.i(TAG, "Starte MainActivity (Log-Ordner: ${AppLogger.logDir?.absolutePath})")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun buildUi(): LinearLayout {
        val pad = (20 * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1F22"))
            setPadding(pad, pad * 2, pad, pad)
            fitsSystemWindows = true

            addView(TextView(context).apply {
                text = "Log-Dateien"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "Logs und Crash-Reports werden in den Ordner „${AppLogger.DIR_NAME}“ geschrieben.\n\n" +
                    "Für /storage/emulated/0/${AppLogger.DIR_NAME} wird „Zugriff auf alle Dateien“ benötigt.\n\n" +
                    "Ohne Zugriff landen die Logs hier (auf Android 11+ mit normalen Datei-Apps meist nicht erreichbar):\n" +
                    "${AppLogger.logDir?.absolutePath}"
                setTextColor(Color.parseColor("#E6E6E6"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, pad, 0, pad)
            })
            addView(Button(context).apply {
                text = "Zugriff erteilen"
                setOnClickListener { StorageAccess.request(this@StartupActivity) }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(Button(context).apply {
                text = "Ohne Zugriff fortfahren"
                setOnClickListener {
                    skipped = true
                    SKIPPED_THIS_PROCESS = true
                    launchMain()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private companion object {
        const val TAG = "StartupActivity"
        @Volatile var SKIPPED_THIS_PROCESS = false
    }
}
