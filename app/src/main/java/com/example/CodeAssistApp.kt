package com.example

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.example.diagnostics.AppLogger
import com.example.diagnostics.CrashHandler
import com.example.diagnostics.ExitInfoLogger
import java.io.File

/**
 * Installiert Logger und CrashHandler so früh wie möglich (attachBaseContext, noch vor onCreate
 * und vor allen Activities).
 */
class CodeAssistApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            val process = currentProcessName(base)
            AppLogger.init(base, process)
            // Im Crash-Prozess keinen eigenen Handler: ein Fehler dort darf keine Schleife erzeugen.
            if (!process.endsWith(":crash")) CrashHandler.install(base)
        } catch (t: Throwable) {
            // Diagnose darf die App niemals selbst zum Absturz bringen.
            android.util.Log.e(TAG, "Diagnose-Initialisierung fehlgeschlagen", t)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            AppLogger.i(TAG, "Application.onCreate")
            if (!currentProcessName(this).endsWith(":crash")) {
                ExitInfoLogger.logRecent(this)
            }
        } catch (t: Throwable) {
            android.util.Log.e(TAG, "Application.onCreate-Diagnose fehlgeschlagen", t)
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private fun n(a: Activity) = a.javaClass.simpleName
            override fun onActivityCreated(a: Activity, s: Bundle?) =
                AppLogger.i(TAG, "${n(a)}.onCreate (savedState=${s != null})")
            override fun onActivityStarted(a: Activity) = AppLogger.d(TAG, "${n(a)}.onStart")
            override fun onActivityResumed(a: Activity) = AppLogger.d(TAG, "${n(a)}.onResume")
            override fun onActivityPaused(a: Activity) = AppLogger.d(TAG, "${n(a)}.onPause")
            override fun onActivityStopped(a: Activity) = AppLogger.d(TAG, "${n(a)}.onStop")
            override fun onActivitySaveInstanceState(a: Activity, o: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) =
                AppLogger.i(TAG, "${n(a)}.onDestroy (finishing=${a.isFinishing})")
        })
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AppLogger.w(TAG, "onLowMemory – ${AppLogger.memorySummary(this)}")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            AppLogger.w(TAG, "onTrimMemory(level=$level) – ${AppLogger.memorySummary(this)}")
        }
    }

    private fun currentProcessName(ctx: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return getProcessName()
        return try {
            File("/proc/self/cmdline").readText().trim { it == '\u0000' || it.isWhitespace() }
                .ifEmpty { ctx.packageName }
        } catch (_: Throwable) {
            ctx.packageName
        }
    }

    private companion object {
        const val TAG = "CodeAssistApp"
    }
}
