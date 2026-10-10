package com.example.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Zentrales Logging:
 *  - spiegelt nach Logcat
 *  - schreibt jede Zeile sofort (flush) in eine Session-Datei im Ordner "CodeAssistLSP"
 *  - hält die letzten Zeilen zusätzlich im Speicher (für den Crash-Report)
 *
 * Speicherort (erster beschreibbarer Treffer):
 *  1. /storage/emulated/0/CodeAssistLSP                       (mit "Zugriff auf alle Dateien")
 *  2. /storage/emulated/0/Android/data/<pkg>/files/CodeAssistLSP  (immer möglich, externer Speicher)
 *  3. <App-intern>/files/CodeAssistLSP
 */
object AppLogger {

    const val DIR_NAME = "CodeAssistLSP"
    private const val TAG = "AppLogger"
    private const val RING_SIZE = 400
    private const val MAX_FILE_BYTES = 5L * 1024 * 1024
    private const val KEEP_SESSION_FILES = 20
    private const val KEEP_CRASH_FILES = 50

    enum class Level(val letter: Char, val priority: Int) {
        VERBOSE('V', Log.VERBOSE), DEBUG('D', Log.DEBUG), INFO('I', Log.INFO),
        WARN('W', Log.WARN), ERROR('E', Log.ERROR), FATAL('F', Log.ERROR)
    }

    @Volatile private var appContext: Context? = null
    @Volatile private var processLabel: String = "main"
    @Volatile private var writer: BufferedWriter? = null
    @Volatile private var bytesWritten = 0L
    @Volatile private var mirror: OutputStream? = null

    /** Anzeige-Pfad der Kopie in Download/CodeAssistLSP (ohne Berechtigung, ab Android 10). */
    @Volatile var mirrorDescription: String? = null
        private set

    @Volatile var logDir: File? = null
        private set
    @Volatile var sessionFile: File? = null
        private set
    @Volatile var storageDescription: String = "nicht initialisiert"
        private set

    private val ring = ArrayDeque<String>(RING_SIZE + 1)
    private val ringLock = Any()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "AppLogger-IO").apply { isDaemon = true }
    }
    private val lineTime = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    // ------------------------------------------------------------------ Init

    @Synchronized
    fun init(context: Context, process: String) {
        if (appContext != null) return
        val ctx = context.applicationContext ?: context
        appContext = ctx
        processLabel = process.substringAfterLast(':', "main").ifEmpty { "main" }
        openSession(ctx)
        logSessionHeader(ctx, process)
        pruneOldFiles()
    }

    /** Nach Erteilen des Speicherzugriffs: auf /sdcard/CodeAssistLSP umschalten. */
    @Synchronized
    fun refreshStorage() {
        val ctx = appContext ?: return
        val publicDir = File(Environment.getExternalStorageDirectory(), DIR_NAME)
        if (logDir == publicDir) return
        if (!StorageAccess.hasPublicStorageAccess(ctx)) return
        i(TAG, "Speicherzugriff erteilt – wechsle Log-Ordner nach ${publicDir.absolutePath}")
        closeWriter()
        openSession(ctx)
        i(TAG, "Log-Ordner gewechselt: $storageDescription -> ${logDir?.absolutePath}")
    }

    private fun openSession(ctx: Context) {
        val (dir, description) = resolveLogDir(ctx)
        logDir = dir
        storageDescription = description
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "session_${stamp}_${processLabel}_${Process.myPid()}.log")
        sessionFile = file
        bytesWritten = 0
        writer = try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), Charsets.UTF_8), 8192)
        } catch (t: Throwable) {
            Log.e(TAG, "Log-Datei konnte nicht geöffnet werden: ${file.absolutePath}", t)
            null
        }
        openMirror(ctx, file.name, dir)
    }

    /**
     * Zusätzliche Kopie in /storage/emulated/0/Download/CodeAssistLSP/ via MediaStore.
     * Braucht KEINE Berechtigung (Android 10+) und ist in jeder Datei-App sichtbar –
     * auch wenn Android/data gesperrt ist und der Speicherzugriff nie erteilt wurde.
     */
    private fun openMirror(ctx: Context, fileName: String, primaryDir: File) {
        mirror = null
        mirrorDescription = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (primaryDir == File(Environment.getExternalStorageDirectory(), DIR_NAME)) return
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/$DIR_NAME")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                mirror = ctx.contentResolver.openOutputStream(uri, "wa")
                mirrorDescription = "/storage/emulated/0/Download/$DIR_NAME/$fileName"
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Download-Kopie nicht möglich: ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    /** Schreibt eine komplette Textdatei (z. B. Crash-Report) nach Download/CodeAssistLSP. */
    fun writeToDownloads(ctx: Context, fileName: String, text: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/$DIR_NAME")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            ctx.contentResolver.openOutputStream(uri, "w")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            "/storage/emulated/0/Download/$DIR_NAME/$fileName"
        } catch (t: Throwable) {
            Log.w(TAG, "Download-Datei nicht möglich: ${t.javaClass.simpleName}: ${t.message}")
            null
        }
    }

    private fun resolveLogDir(ctx: Context): Pair<File, String> {
        val candidates = ArrayList<Pair<File, String>>(3)
        if (StorageAccess.hasPublicStorageAccess(ctx)) {
            candidates += File(Environment.getExternalStorageDirectory(), DIR_NAME) to
                "Öffentlicher Speicher"
        }
        try {
            ctx.getExternalFilesDir(null)?.let {
                candidates += File(it, DIR_NAME) to "App-Ordner auf externem Speicher"
            }
        } catch (_: Throwable) { /* ignorieren */ }
        candidates += File(ctx.filesDir, DIR_NAME) to "Interner App-Speicher"

        for ((dir, description) in candidates) {
            try {
                if (!dir.exists()) dir.mkdirs()
                if (!dir.isDirectory || !dir.canWrite()) continue
                val probe = File(dir, ".probe")
                probe.writeText("ok")
                probe.delete()
                return dir to description
            } catch (t: Throwable) {
                Log.w(TAG, "Ordner nicht nutzbar: ${dir.absolutePath} (${t.javaClass.simpleName}: ${t.message})")
            }
        }
        return File(ctx.filesDir, DIR_NAME) to "Interner App-Speicher (Fallback)"
    }

    private fun logSessionHeader(ctx: Context, process: String) {
        val pkg = try {
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        } catch (_: Throwable) { null }
        i(TAG, "==================== SESSION START ====================")
        i(TAG, "Prozess: $process (pid=${Process.myPid()})")
        i(TAG, "Log-Ordner: ${logDir?.absolutePath}  [$storageDescription]")
        i(TAG, "Log-Datei:  ${sessionFile?.absolutePath}")
        i(TAG, "Kopie (ohne Berechtigung sichtbar): ${mirrorDescription ?: "nicht verfügbar"}")
        i(TAG, "App: ${ctx.packageName} v${pkg?.versionName} (${pkg?.longVersionCodeCompat()})")
        i(TAG, "Gerät: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE}), Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        i(TAG, "ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        i(TAG, "Öffentlicher Speicherzugriff: ${StorageAccess.hasPublicStorageAccess(ctx)}")
        i(TAG, "Heap: max=${Runtime.getRuntime().maxMemory() shr 20} MB")
        i(TAG, "=======================================================")
    }

    @Suppress("DEPRECATION")
    private fun android.content.pm.PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    // ------------------------------------------------------------------ API

    fun v(tag: String, msg: String) = log(Level.VERBOSE, tag, msg, null)
    fun d(tag: String, msg: String) = log(Level.DEBUG, tag, msg, null)
    fun i(tag: String, msg: String) = log(Level.INFO, tag, msg, null)
    fun w(tag: String, msg: String, t: Throwable? = null) = log(Level.WARN, tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = log(Level.ERROR, tag, msg, t)
    fun f(tag: String, msg: String, t: Throwable? = null) = log(Level.FATAL, tag, msg, t)

    /** Protokolliert Start, Ende (mit Dauer) oder Fehler eines Schritts und wirft Fehler weiter. */
    inline fun <T> step(tag: String, name: String, block: () -> T): T {
        i(tag, "▶ $name")
        val start = SystemClock.elapsedRealtime()
        try {
            val result = block()
            i(tag, "✔ $name (${SystemClock.elapsedRealtime() - start} ms)")
            return result
        } catch (t: Throwable) {
            e(tag, "✖ $name FEHLGESCHLAGEN nach ${SystemClock.elapsedRealtime() - start} ms", t)
            throw t
        }
    }

    fun log(level: Level, tag: String, msg: String, t: Throwable?) {
        try {
            if (t == null) Log.println(level.priority, tag, msg)
            else Log.println(level.priority, tag, msg + "\n" + Log.getStackTraceString(t))
        } catch (_: Throwable) { /* Logcat darf nie crashen */ }

        val sb = StringBuilder(msg.length + 64)
        sb.append(lineTime.get()!!.format(Date())).append(' ')
            .append(level.letter).append('/').append(tag)
            .append(" [").append(Thread.currentThread().name).append("] ")
            .append(msg)
        if (t != null) sb.append('\n').append(Log.getStackTraceString(t).trimEnd())
        val line = sb.toString()

        synchronized(ringLock) {
            ring.addLast(line)
            while (ring.size > RING_SIZE) ring.removeFirst()
        }
        try {
            executor.execute { writeLine(line) }
        } catch (_: Throwable) { /* Executor beendet */ }
    }

    // ------------------------------------------------------------------ Zugriff

    /** Die letzten [maxLines] Zeilen aus dem Speicher. */
    fun recentLines(maxLines: Int = 200): List<String> = synchronized(ringLock) {
        ring.toList().takeLast(maxLines)
    }

    /** Wartet (max. [timeoutMs]) bis alle bisherigen Zeilen auf Platte stehen. */
    fun flushBlocking(timeoutMs: Long = 1500) {
        try {
            executor.submit(Callable { writer?.flush(); mirror?.flush() }).get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: Throwable) { /* Timeout ist okay */ }
    }

    fun memorySummary(ctx: Context): String {
        val rt = Runtime.getRuntime()
        val sb = StringBuilder()
        sb.append("Heap: used=${(rt.totalMemory() - rt.freeMemory()) shr 20} MB, ")
            .append("total=${rt.totalMemory() shr 20} MB, max=${rt.maxMemory() shr 20} MB")
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            sb.append("\nSystem: verfügbar=${info.availMem shr 20} MB / ${info.totalMem shr 20} MB, lowMemory=${info.lowMemory}")
        } catch (_: Throwable) { }
        return sb.toString()
    }

    // ------------------------------------------------------------------ intern

    private fun writeLine(line: String) {
        try {
            mirror?.let { it.write((line + "\n").toByteArray(Charsets.UTF_8)); it.flush() }
        } catch (t: Throwable) {
            Log.w(TAG, "Schreiben der Download-Kopie fehlgeschlagen: ${t.message}")
            mirror = null
        }
        val w = writer ?: return
        try {
            if (bytesWritten > MAX_FILE_BYTES) return
            w.write(line)
            w.write("\n")
            w.flush()
            bytesWritten += line.length + 1
            if (bytesWritten > MAX_FILE_BYTES) {
                w.write("--- Log-Datei hat ${MAX_FILE_BYTES / 1024 / 1024} MB erreicht, weitere Zeilen werden verworfen ---\n")
                w.flush()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Schreiben ins Log fehlgeschlagen", t)
        }
    }

    private fun closeWriter() {
        try {
            executor.submit {
                writer?.flush(); writer?.close()
                mirror?.flush(); mirror?.close()
            }.get(1, TimeUnit.SECONDS)
        } catch (_: Throwable) { }
        writer = null
        mirror = null
    }

    private fun pruneOldFiles() {
        val dir = logDir ?: return
        executor.execute {
            try {
                prune(dir, "session_", KEEP_SESSION_FILES)
                prune(dir, "crash_", KEEP_CRASH_FILES)
            } catch (_: Throwable) { }
        }
    }

    private fun prune(dir: File, prefix: String, keep: Int) {
        val files = dir.listFiles { f -> f.isFile && f.name.startsWith(prefix) } ?: return
        files.sortedByDescending { it.lastModified() }.drop(keep).forEach { it.delete() }
    }
}
