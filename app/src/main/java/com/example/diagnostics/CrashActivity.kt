package com.example.diagnostics

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Process
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Zeigt die Ursache eines Absturzes. Läuft im eigenen Prozess (":crash"), nutzt bewusst nur
 * Framework-Views und ein Framework-Theme – unabhängig von AppCompat/Material/ViewBinding,
 * damit die Anzeige auch dann funktioniert, wenn genau diese Teile den Crash verursacht haben.
 */
class CrashActivity : Activity() {

    private enum class Tab(val label: String) { OVERVIEW("Übersicht"), STACK("Stacktrace"), DEVICE("Gerät"), LOG("Log") }

    private var report: String = ""
    private var sections: Map<String, String> = emptyMap()
    private lateinit var content: TextView
    private lateinit var scroll: ScrollView
    private val tabButtons = LinkedHashMap<Tab, Button>()
    private lateinit var storageButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i(TAG, "CrashActivity gestartet")
        report = loadReport()
        try {
            sections = CrashReport.sections(report)
            setContentView(buildUi())
            show(Tab.OVERVIEW)
        } catch (t: Throwable) {
            // Notanzeige: reiner Text, damit der Report auf jeden Fall sichtbar ist
            AppLogger.e(TAG, "Crash-UI konnte nicht aufgebaut werden", t)
            val tv = TextView(this).apply {
                text = "CrashActivity-UI fehlgeschlagen: $t\n\n$report"
                typeface = Typeface.MONOSPACE
                setTextIsSelectable(true)
                setTextColor(Color.WHITE)
                setPadding(24, 48, 24, 24)
            }
            setContentView(ScrollView(this).apply { setBackgroundColor(Color.BLACK); addView(tv) })
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::storageButton.isInitialized) return
        AppLogger.refreshStorage()
        storageButton.visibility =
            if (StorageAccess.hasPublicStorageAccess(this)) android.view.View.GONE else android.view.View.VISIBLE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onResume()
    }

    private fun loadReport(): String {
        val path = intent?.getStringExtra(EXTRA_REPORT_FILE)
        val candidates = listOfNotNull(path?.let(::File), File(filesDir, "crash/last_crash.txt"))
        for (f in candidates) {
            try {
                if (f.isFile) return f.readText()
            } catch (t: Throwable) {
                AppLogger.e(TAG, "Report nicht lesbar: ${f.absolutePath}", t)
            }
        }
        return "=== ${CrashReport.SECTION_SUMMARY} ===\nKein Crash-Report gefunden.\n"
    }

    // ------------------------------------------------------------------ UI

    private fun buildUi(): LinearLayout {
        val summary = sections[CrashReport.SECTION_SUMMARY].orEmpty()
        val exception = summary.lineSequence().firstOrNull { it.startsWith("Exception:") }
            ?.removePrefix("Exception:")?.trim().orEmpty()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            fitsSystemWindows = true
        }

        root.addView(TextView(this).apply {
            text = "App abgestürzt"
            setTextColor(ERROR)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(16), dp(14), dp(16), dp(2))
        })
        root.addView(TextView(this).apply {
            text = exception.ifEmpty { "Unbekannte Exception" }
            setTextColor(TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            setPadding(dp(16), 0, dp(16), dp(8))
        })

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (tab in Tab.values()) {
            val b = smallButton(tab.label) { show(tab) }
            tabButtons[tab] = b
            tabs.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(tabs)

        content = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setTextColor(TEXT)
            setTextIsSelectable(true)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        scroll = ScrollView(this).apply { addView(content) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        root.addView(TextView(this).apply {
            text = "Logs: ${AppLogger.logDir?.absolutePath ?: "?"}  [${AppLogger.storageDescription}]"
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setPadding(dp(12), dp(4), dp(12), dp(4))
        })

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(smallButton("Kopieren") { copyReport() }, weight())
        row1.addView(smallButton("Teilen") { shareReport() }, weight())
        row1.addView(smallButton("Neustart") { restartApp() }, weight())
        root.addView(row1)

        storageButton = smallButton("Speicherzugriff für /CodeAssistLSP erteilen") { StorageAccess.request(this) }
        root.addView(storageButton)
        return root
    }

    private fun show(tab: Tab) {
        val text = when (tab) {
            Tab.OVERVIEW -> listOf(CrashReport.SECTION_SUMMARY, CrashReport.SECTION_HINTS)
                .joinToString("\n\n") { title -> "▌$title\n${sections[title].orEmpty()}" }
            Tab.STACK -> sections[CrashReport.SECTION_STACK].orEmpty()
            Tab.DEVICE -> sections[CrashReport.SECTION_DEVICE].orEmpty()
            Tab.LOG -> sections[CrashReport.SECTION_LOG].orEmpty()
        }
        content.text = text.ifEmpty { "(leer)" }
        scroll.post { scroll.scrollTo(0, 0) }
        tabButtons.forEach { (t, b) -> b.setTextColor(if (t == tab) ACCENT else TEXT) }
    }

    // ------------------------------------------------------------------ Aktionen

    private fun copyReport() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("CodeAssist Crash", report.take(MAX_CLIP_CHARS)))
            Toast.makeText(this, "Report kopiert", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Kopieren fehlgeschlagen", t)
        }
    }

    private fun shareReport() {
        try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "CodeAssist Crash-Report")
                putExtra(Intent.EXTRA_TEXT, report.take(MAX_CLIP_CHARS))
            }
            startActivity(Intent.createChooser(send, "Crash-Report teilen"))
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Teilen fehlgeschlagen", t)
        }
    }

    private fun restartApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (launch != null) startActivity(launch)
        finishAndRemoveTask()
        Process.killProcess(Process.myPid())
    }

    // ------------------------------------------------------------------ Helfer

    private fun smallButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(TEXT)
        gravity = Gravity.CENTER
        setOnClickListener { onClick() }
    }

    private fun weight() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_REPORT_FILE = "report_file"
        private const val TAG = "CrashActivity"
        private const val MAX_CLIP_CHARS = 100_000
        private val BG = Color.parseColor("#1E1F22")
        private val TEXT = Color.parseColor("#E6E6E6")
        private val MUTED = Color.parseColor("#9DA0A8")
        private val ERROR = Color.parseColor("#F44336")
        private val ACCENT = Color.parseColor("#7F9BFF")
    }
}
