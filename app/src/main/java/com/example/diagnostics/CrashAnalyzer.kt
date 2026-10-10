package com.example.diagnostics

import java.io.FileNotFoundException
import java.util.IdentityHashMap

/** Leitet aus einem Throwable verständliche Hinweise auf die Ursache ab (reine Heuristik). */
object CrashAnalyzer {

    data class Result(
        val chain: List<Throwable>,
        val root: Throwable,
        val ownFrame: StackTraceElement?,
        val hints: List<String>
    )

    fun analyze(t: Throwable, ownPackage: String = "com.example"): Result {
        val chain = causeChain(t)
        val root = chain.last()
        val own = (listOf(root) + chain.reversed())
            .flatMap { it.stackTrace.asList() }
            .firstOrNull { it.className.startsWith(ownPackage) }
        return Result(chain, root, own, hints(chain, root))
    }

    fun causeChain(t: Throwable): List<Throwable> {
        val seen = IdentityHashMap<Throwable, Boolean>()
        val out = ArrayList<Throwable>()
        var cur: Throwable? = t
        while (cur != null && seen.put(cur, true) == null && out.size < 25) {
            out += cur
            cur = cur.cause
        }
        return out
    }

    private fun hints(chain: List<Throwable>, root: Throwable): List<String> {
        val hints = LinkedHashSet<String>()
        val allText = chain.joinToString("\n") { "${it.javaClass.name}: ${it.message}" }
        val rootStack = root.stackTrace

        for (e in chain) {
            val name = e.javaClass.name
            val msg = e.message.orEmpty()
            when {
                name.endsWith("InflateException") || msg.contains("Binary XML file line") ->
                    hints += "Layout-Fehler beim Aufblasen einer XML-Datei ($msg). Typische Gründe: ungültiges " +
                        "Attribut, fehlende/falsche View-Klasse, Theme passt nicht zur View (z. B. Material-View " +
                        "ohne Material-Theme) oder fehlende Ressource."
                msg.contains("You need to use a Theme.AppCompat") ->
                    hints += "Die Activity/View benötigt ein AppCompat-/Material-Theme. Theme in AndroidManifest.xml " +
                        "und themes.xml prüfen."
                name.endsWith("UninitializedPropertyAccessException") ->
                    hints += "Eine lateinit-Eigenschaft wurde vor der Initialisierung gelesen ($msg). " +
                        "Reihenfolge in onCreate()/init-Blöcken prüfen."
                name == "java.lang.ExceptionInInitializerError" ->
                    hints += "Fehler in einem statischen Initialisierer (object/companion/static). " +
                        "Die eigentliche Ursache steht unter „Caused by“."
                name.endsWith("ClassNotFoundException") || name.endsWith("NoClassDefFoundError") ->
                    hints += "Klasse zur Laufzeit nicht gefunden ($msg). Abhängigkeit fehlt im Build, " +
                        "R8/ProGuard hat sie entfernt oder eine View-Klasse im Layout ist falsch geschrieben."
                name.endsWith("NoSuchMethodError") || name.endsWith("AbstractMethodError") ||
                    name.endsWith("IncompatibleClassChangeError") || name.endsWith("NoSuchFieldError") ->
                    hints += "Binär-Inkompatibilität: zwei Bibliotheksversionen passen nicht zusammen ($msg). " +
                        "Versionen in libs.versions.toml prüfen (`./gradlew :app:dependencies`)."
                name.endsWith("UnsatisfiedLinkError") ->
                    hints += "Native Bibliothek fehlt oder passt nicht zur ABI des Geräts ($msg)."
                name.endsWith("Resources\$NotFoundException") ->
                    hints += "Ressource nicht gefunden ($msg). Ressourcen-ID, Variante (night/v26) und Namespace prüfen."
                name.endsWith("SecurityException") ->
                    hints += "Fehlende Berechtigung oder verbotener Zugriff ($msg). Manifest und Laufzeit-Berechtigungen prüfen."
                e is OutOfMemoryError ->
                    hints += "Speicher erschöpft. Große Dateien/Bitmaps/Indizes? Siehe Heap-Werte im Abschnitt Gerät."
                e is StackOverflowError ->
                    hints += "Endlosrekursion: dieselben Frames wiederholen sich im Stacktrace (Parser/Resolver/Adapter prüfen)."
                e is FileNotFoundException || msg.contains("EACCES") || msg.contains("ENOENT") ->
                    hints += "Datei-/Pfadzugriff fehlgeschlagen ($msg). Existiert der Pfad, ist er beschreibbar, " +
                        "ist Speicherzugriff erteilt (Scoped Storage)?"
                e is NullPointerException ->
                    hints += "NullPointerException${if (msg.isNotEmpty()) ": $msg" else ""}. " +
                        "Wert war null – bei Java/Android-APIs (Plattformtypen) auf `?.`/Null-Check achten."
                e is ClassCastException ->
                    hints += "Falscher Typ beim Cast ($msg). Beim Binding: View-ID im Layout hat anderen Typ als erwartet."
                e is IndexOutOfBoundsException ->
                    hints += "Index außerhalb des gültigen Bereichs ($msg). Listen-/Adapter-/Cursor-Positionen prüfen."
                e is IllegalStateException && msg.contains("Already attached") ->
                    hints += "View wurde zweimal an einen Parent gehängt."
                e is IllegalStateException ->
                    hints += "IllegalStateException${if (msg.isNotEmpty()) ": $msg" else ""} – Objekt war im falschen Zustand."
                e is IllegalArgumentException ->
                    hints += "IllegalArgumentException${if (msg.isNotEmpty()) ": $msg" else ""} – ungültiges Argument."
            }
        }

        if (rootStack.any { it.className.startsWith("io.github.rosemoe") } ||
            allText.contains("io.github.rosemoe")
        ) {
            hints += "Beteiligt: Sora-Editor (io.github.rosemoe). Konfiguration in MainActivity.initCodeEditor() " +
                "(Farbschema, Sprache, inputType) und Sora-Version prüfen."
        }
        if (allText.contains("ActivityThread") || chain.first().message.orEmpty().startsWith("Unable to start activity")) {
            hints += "Start der Activity gescheitert – die eigentliche Ursache steht in der untersten „Caused by“-Zeile."
        }
        if (hints.isEmpty()) {
            hints += "Keine bekannte Fehlerklasse erkannt. Maßgeblich ist die unterste „Caused by“-Zeile " +
                "und der erste Frame im eigenen Code (siehe Übersicht)."
        }
        return hints.toList()
    }
}
