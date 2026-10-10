package com.example.diagnostics

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashHandler : Thread.UncaughtExceptionHandler {
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var applicationContext: Context? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            // 1. Immer sicher in Logcat schreiben (schlägt nie fehl)
            Log.e("CrashHandler", "FATAL UNCAUGHT EXCEPTION:\n$stackTrace")

            // 2. Isolierter Block für den Dateizugriff -> Verhindert Double Fault
            try {
                StorageAccess.saveCrashLog(stackTrace)
            } catch (e: Exception) {
                Log.e("CrashHandler", "Fehler beim Speichern des Crash-Logs. Rechte fehlen?", e)
            }

            // 3. Fehler-Bildschirm (CrashActivity) starten
            applicationContext?.let { ctx ->
                val intent = Intent(ctx, CrashActivity::class.java).apply {
                    putExtra("CRASH_LOG", stackTrace)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                ctx.startActivity(intent)
            }

        } catch (e: Exception) {
            // Letzte Rettungsschicht, falls der Handler selbst abstürzt
            Log.e("CrashHandler", "Kritischer Fehler INNERHALB des CrashHandlers!", e)
        } finally {
            // Den Prozess nach der Verarbeitung sauber durch das System killen lassen
            defaultHandler?.uncaughtException(thread, throwable)
            exitProcess(1)
        }
    }
}
