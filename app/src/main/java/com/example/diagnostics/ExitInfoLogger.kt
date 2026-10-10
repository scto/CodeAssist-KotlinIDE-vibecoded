package com.example.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Protokolliert, WARUM frühere Prozesse beendet wurden (Android 11+).
 * Wichtig für Abstürze, die der Java-CrashHandler nicht sieht: native Crashes, ANR, Kill durch System.
 */
object ExitInfoLogger {

    private const val TAG = "ExitInfo"

    fun logRecent(context: Context, max: Int = 5) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val infos = am.getHistoricalProcessExitReasons(context.packageName, 0, max)
            if (infos.isEmpty()) {
                AppLogger.i(TAG, "Keine früheren Prozess-Beendigungen vorhanden")
                return
            }
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            for (info in infos) {
                AppLogger.i(
                    TAG,
                    "Früherer Prozess '${info.processName}' pid=${info.pid} @ ${fmt.format(Date(info.timestamp))}: " +
                        "${reasonName(info.reason)} status=${info.status} " +
                        "importance=${info.importance} rss=${info.rss shr 10} MB " +
                        "desc='${info.description}'"
                )
                if (info.reason == ApplicationExitInfo.REASON_ANR) {
                    try {
                        info.traceInputStream?.use { s ->
                            val text = s.readBytes().take(20_000).toByteArray().toString(Charsets.UTF_8)
                            AppLogger.w(TAG, "ANR-Trace (gekürzt):\n$text")
                        }
                    } catch (_: Throwable) { }
                }
                if (info.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                    AppLogger.w(
                        TAG,
                        "NATIVER Absturz (kein Java-Stacktrace möglich). Details: `adb logcat -b crash` " +
                            "oder `adb bugreport` direkt nach dem Absturz."
                    )
                }
            }
        } catch (t: Throwable) {
            AppLogger.w(TAG, "ExitInfo nicht lesbar", t)
        }
    }

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_CRASH -> "CRASH (Java)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_OTHER -> "OTHER"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        else -> "REASON_$reason"
    }
}
