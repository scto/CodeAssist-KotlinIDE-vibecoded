# Analyse des App-Start-Absturzes

Dieses Dokument beschreibt die Ursache und Lösung für das Problem, bei dem die App sofort beim Start abstürzte, ohne Berechtigungen abzufragen oder Logs und Crash-Reports zu generieren.

## Problembeschreibung
Die App stürzte direkt nach der Installation und dem Start ab. Es passierte Folgendes (bzw. passierte *nicht*):
1. **Kein Log & Keine CrashActivity:** Der eigens geschriebene `CrashHandler` (sowie der `AppLogger`) wurde scheinbar komplett ignoriert. Es erschienen keine Logs im Diagnose-Ordner und es wurde keine Notfall-UI geladen.
2. **Keine Berechtigungsabfrage:** Die `PermissionActivity`, die als Launcher-Activity konfiguriert war und eigentlich als erstes den Speicherzugriff anfordern sollte, wurde nie angezeigt.

## Die Ursache
Der Fehler lag nicht im Kotlin-Code (weder in der `CodeAssistApp`, dem `CrashHandler` noch in den Activities), sondern in der **Build-Konfiguration**.

In den Gradle-Dateien fehlte das **Kotlin-Android-Plugin** (`org.jetbrains.kotlin.android`).
Da nur das `com.android.application` Plugin aktiv war, hat das Android Gradle Plugin (AGP) **nur Java-Dateien** kompiliert und sämtliche `.kt`-Dateien vollständig ignoriert. 

Die daraus resultierende APK-Datei enthielt somit **keine der geschriebenen Kotlin-Klassen**.

Beim App-Start passierte im Android-System (Dalvik/ART) Folgendes:
1. Das System las die `AndroidManifest.xml` und versuchte, die Haupt-Applikationsklasse `com.example.CodeAssistApp` zu laden.
2. Da diese Klasse in der APK (bzw. der zugehörigen `classes.dex`) nicht existierte, warf das System sofort eine `java.lang.ClassNotFoundException`.
3. Da dieser Fehler ganz zu Beginn der App-Initialisierung – weit vor jeglichem von uns geschriebenem Code – stattfand, existierte der `CrashHandler` noch gar nicht. Die App wurde vom Betriebssystem einfach sofort beendet.
4. Folglich konnten auch keine Activities geladen werden und keine Berechtigungen abgefragt werden.

## Die Lösung
Das fehlende Kotlin-Android-Plugin wurde zum Build-Prozess hinzugefügt.

1. **`gradle/libs.versions.toml`**
   Das Plugin wurde deklariert:
   ```toml
   kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
   ```

2. **`build.gradle.kts` (Root)**
   Das Plugin wurde für das Projekt bereitgestellt:
   ```kotlin
   alias(libs.plugins.kotlin.android) apply false
   ```

3. **`app/build.gradle.kts` (App-Modul)**
   Das Plugin wurde für das App-Modul aktiviert:
   ```kotlin
   alias(libs.plugins.kotlin.android)
   ```

Durch diese Änderung werden alle Kotlin-Quelldateien wieder regulär kompiliert, dem Dex-Prozess hinzugefügt und stehen zur Laufzeit zur Verfügung, sodass der App-Start wie vorgesehen funktioniert.
