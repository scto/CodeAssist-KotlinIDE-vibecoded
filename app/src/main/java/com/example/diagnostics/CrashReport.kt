package com.example.diagnostics

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Baut den Crash-Report (Text mit Abschnitten) und speichert ihn. */
object CrashReport {

    const val SECTION_SUMMARY = "ZUSAMMENFASSUNG"
    const val SECTION_HINTS = "MÖGLICHE URSACHE"
    const val SECTION_STACK = "STACKTRACE"
    const val SECTION_DEVICE = "GERÄT & APP"
    const val SECTION_LOG = "LOG (letzte Zeilen)"

    private val SECTION_REGEX = Regex("^=== (.+?) ===$", RegexOption.MULTILINE)

    fun build(context: Context, thread: Thread?, t: Throwable): String {
        val analysis = CrashAnalyzer.analyze(t)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
        val sb = StringBuilder(16 * 1024)

        sb.append("=== $SECTION_SUMMARY ===\n")
        sb.append("Zeit:       $time\n")
        sb.append("Thread:     ${thread?.name ?: "?"}\n")
        sb.append("Exception:  ${t.javaClass.name}\n")
        sb.append("Nachricht:  ${t.message ?: "(keine)"}\n")
        if (analysis.root !== t) {
            sb.append("Ursache:    ${analysis.root.javaClass.name}: ${analysis.root.message ?: "(keine)"}\n")
        }
        val top = analysis.root.stackTrace.firstOrNull()
        if (top != null) sb.append("Auslöser:   $top\n")
        analysis.ownFrame?.let { sb.append("Eigener Code: $it\n") }
        sb.append("Kette:      ${analysis.chain.joinToString(" → ") { it.javaClass.simpleName }}\n\n")

        sb.append("=== $SECTION_HINTS ===\n")
        analysis.hints.forEachIndexed { i, h -> sb.append("${i + 1}. $h\n") }
        sb.append('\n')

        sb.append("=== $SECTION_STACK ===\n")
        sb.append(stackTrace(t)).append('\n')
        val main = Looper_mainThread()
        if (main != null && main !== thread) {
            sb.append("--- Haupt-Thread (${main.name}) zum Absturzzeitpunkt ---\n")
            main.stackTrace.forEach { sb.append("    at ").append(it).append('\n') }
            sb.append('\n')
        }

        sb.append("=== $SECTION_DEVICE ===\n")
        sb.append(deviceInfo(context)).append('\n')

        sb.append("=== $SECTION_LOG ===\n")
        AppLogger.recentLines(250).forEach { sb.append(it).append('\n') }
        return sb.toString()
    }

    /** Speichert extern (CodeAssistLSP) und intern. Rückgabe: interne Kopie (immer lesbar). */
    fun save(context: Context, report: String): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val internalDir = File(context.filesDir, "crash").apply { mkdirs() }
        val internal = File(internalDir, "last_crash.txt")
        internal.writeText(report)
        try {
            val dir = AppLogger.logDir
            if (dir != null) {
                dir.mkdirs()
                File(dir, "crash_$stamp.txt").writeText(report)
            }
        } catch (t: Throwable) {
            Log.e("CrashReport", "Externe Kopie fehlgeschlagen", t)
        }
        val shown = AppLogger.writeToDownloads(context, "crash_$stamp.txt", report)
        if (shown != null) Log.i("CrashReport", "Crash-Report auch in $shown")
        return internal
    }

    /** Zerlegt einen Report in (Titel → Inhalt). */
    fun sections(report: String): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()
        val matches = SECTION_REGEX.findAll(report).toList()
        for ((i, m) in matches.withIndex()) {
            val start = m.range.last + 1
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else report.length
            out[m.groupValues[1]] = report.substring(start, end).trim('\n')
        }
        return out
    }

    private fun stackTrace(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString().trimEnd()
    }

    @Suppress("FunctionName")
    private fun Looper_mainThread(): Thread? = try {
        android.os.Looper.getMainLooper().thread
    } catch (_: Throwable) { null }

    @Suppress("DEPRECATION")
    private fun deviceInfo(ctx: Context): String {
        val pkg: PackageInfo? = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0) } catch (_: Throwable) { null }
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg?.longVersionCode else pkg?.versionCode?.toLong()
        return buildString {
            append("App:        ${ctx.packageName} v${pkg?.versionName} ($code)\n")
            append("Gerät:      ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n")
            append("Android:    ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), Patch ${Build.VERSION.SECURITY_PATCH}\n")
            append("ABIs:       ${Build.SUPPORTED_ABIS.joinToString()}\n")
            append("Build:      ${Build.FINGERPRINT}\n")
            append(AppLogger.memorySummary(ctx)).append('\n')
            append("Log-Ordner: ${AppLogger.logDir?.absolutePath} [${AppLogger.storageDescription}]\n")
            append("Session:    ${AppLogger.sessionFile?.absolutePath}\n")
            append("Kopie:      ${AppLogger.mirrorDescription ?: "-"}\n")
            append("Speicherzugriff: ${StorageAccess.hasPublicStorageAccess(ctx)}\n")
        }
    }
}
