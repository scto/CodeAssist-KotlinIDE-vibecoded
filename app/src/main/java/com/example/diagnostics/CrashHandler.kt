package com.example.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Fängt jede unbehandelte Exception (alle Threads), schreibt Log + Report und
 * zeigt die [CrashActivity] (eigener Prozess ":crash") an.
 *
 * Nicht abgedeckt: native Abstürze (SIGSEGV) – siehe [ExitInfoLogger].
 */
class CrashHandler private constructor(
    private val app: Context,
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!handling.compareAndSet(false, true)) {
            // Folgefehler während der Behandlung → nicht endlos rekursieren
            previous?.uncaughtException(thread, throwable)
            return
        }
        var reported = false
        try {
            AppLogger.f(TAG, "UNBEHANDELTE EXCEPTION in Thread '${thread.name}'", throwable)
            val report = CrashReport.build(app, thread, throwable)
            val file = CrashReport.save(app, report)
            AppLogger.i(TAG, "Crash-Report gespeichert: ${file.absolutePath}")
            AppLogger.flushBlocking()
            val intent = Intent(app, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CrashActivity.EXTRA_REPORT_FILE, file.absolutePath)
            }
            app.startActivity(intent)
            reported = true
        } catch (t: Throwable) {
            try { Log.e(TAG, "Crash-Behandlung selbst fehlgeschlagen", t) } catch (_: Throwable) { }
        }

        if (!reported) {
            previous?.uncaughtException(thread, throwable)
        }
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    companion object {
        private const val TAG = "CrashHandler"
        private val handling = AtomicBoolean(false)

        fun install(context: Context) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current is CrashHandler) return
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext ?: context, current))
            AppLogger.i(TAG, "CrashHandler installiert (vorheriger Handler: ${current?.javaClass?.name})")
        }
    }
}
