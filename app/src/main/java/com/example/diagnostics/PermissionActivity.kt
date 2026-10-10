package com.example.diagnostics

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.MainActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Launcher-Einstieg: fragt als allererstes den Speicherzugriff für die Logs ab (Accompanist),
 * danach startet MainActivity.
 *
 * Accompanist deckt normale Laufzeit-Berechtigungen ab (Android 8–10: READ/WRITE_EXTERNAL_STORAGE).
 * "Zugriff auf alle Dateien" (Android 11+, MANAGE_EXTERNAL_STORAGE) ist eine Spezial-Berechtigung
 * und lässt sich nur über die Systemeinstellungen erteilen – dafür wird der Settings-Intent genutzt.
 *
 * Scheitert der Compose-Start, wird auf die klassische [StartupActivity] ausgewichen.
 */
@OptIn(ExperimentalPermissionsApi::class)
class PermissionActivity : ComponentActivity() {

    private var mainLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("CA/Boot", "PermissionActivity.onCreate: start")
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContent {
                CodeAssistPermissionTheme {
                    StoragePermissionGate(onFinished = ::launchMain)
                }
            }
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Compose-Start fehlgeschlagen – Fallback auf StartupActivity", t)
            startActivity(Intent(this, StartupActivity::class.java))
            finish()
        }
    }

    private fun launchMain() {
        if (mainLaunched) return
        mainLaunched = true
        AppLogger.i(TAG, "Starte MainActivity (Log-Ordner: ${AppLogger.logDir?.absolutePath})")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val TAG = "PermissionActivity"
    }
}

// ---------------------------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------------------------

@Immutable
data class StoragePermissionUiState(
    val needsAllFilesAccess: Boolean,
    val permanentlyDenied: Boolean,
    val logDir: String,
    val mirrorPath: String
)

@Composable
private fun CodeAssistPermissionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7F52FF),
            background = Color(0xFF1E1F22),
            surface = Color(0xFF2B2D30),
            onBackground = Color(0xFFE6E6E6),
            onSurface = Color(0xFFE6E6E6)
        ),
        content = content
    )
}

/** Verbindet Accompanist-Zustand und System-Einstellungen mit dem zustandslosen Screen. */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun StoragePermissionGate(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val needsAllFiles = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    var hasAccess by remember { mutableStateOf(StorageAccess.hasPublicStorageAccess(context)) }
    var requestedOnce by remember { mutableStateOf(false) }

    // Android 8–10: normale Laufzeit-Berechtigung über Accompanist
    val legacyState = if (!needsAllFiles) {
        rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) { hasAccess = StorageAccess.hasPublicStorageAccess(context) }
    } else {
        null
    }

    // Nach Rückkehr aus den Einstellungen neu prüfen
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAccess = StorageAccess.hasPublicStorageAccess(context)
    }

    LaunchedEffect(hasAccess) {
        if (hasAccess) {
            AppLogger.refreshStorage()
            onFinished()
        }
    }

    val uiState = StoragePermissionUiState(
        needsAllFilesAccess = needsAllFiles,
        permanentlyDenied = legacyState != null && requestedOnce &&
            !legacyState.allPermissionsGranted && !legacyState.shouldShowRationale,
        logDir = AppLogger.logDir?.absolutePath ?: "?",
        mirrorPath = AppLogger.mirrorDescription
            ?: "/storage/emulated/0/Download/${AppLogger.DIR_NAME}/ (nicht verfügbar)"
    )

    StoragePermissionScreen(
        state = uiState,
        onRequest = {
            requestedOnce = true
            if (legacyState != null) legacyState.launchMultiplePermissionRequest()
            else StorageAccess.request(activity)
        },
        onOpenAppSettings = {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}"))
            )
        },
        onSkip = onFinished,
        modifier = modifier
    )
}

@Composable
private fun StoragePermissionScreen(
    state: StoragePermissionUiState,
    onRequest: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Log-Dateien",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Logs und Crash-Reports werden im Ordner „${AppLogger.DIR_NAME}“ gespeichert. " +
                    if (state.needsAllFilesAccess) {
                        "Für /storage/emulated/0/${AppLogger.DIR_NAME} ist „Zugriff auf alle Dateien“ nötig " +
                            "(öffnet die Systemeinstellungen)."
                    } else {
                        "Dafür wird der Speicherzugriff benötigt."
                    },
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Ohne Zugriff:\n${state.logDir}\n\nKopie ohne Berechtigung:\n${state.mirrorPath}",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Zugriff erteilen")
            }
            if (state.permanentlyDenied) {
                OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("App-Einstellungen öffnen")
                }
            }
            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Ohne Zugriff fortfahren")
            }
        }
    }
}
