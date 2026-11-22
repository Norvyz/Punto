package com.punto.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.punto.app.domain.model.AccentColor
import com.punto.app.domain.model.ThemePreference
import com.punto.app.domain.repository.UserPreferences
import com.punto.app.domain.repository.UserPreferencesRepository
import com.punto.app.presentation.intro.PuntoIntro
import com.punto.app.presentation.navigation.PuntoApp
import com.punto.app.presentation.theme.PuntoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    /** El splash del sistema se queda en pantalla hasta que las preferencias estén leídas. */
    @Volatile private var prefsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Debe ir ANTES de super.onCreate().
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { !prefsReady }
        // Seguro: si por algún motivo las preferencias tardaran demasiado, no dejamos el splash colgado.
        lifecycleScope.launch {
            delay(2_000)
            prefsReady = true
        }

        setContent {
            // null = todavía leyendo preferencias; así no parpadea la bienvenida.
            val prefs by userPreferencesRepository.preferences.collectAsStateWithLifecycle<UserPreferences?>(initialValue = null)
            val current = prefs
            val darkTheme = when (current?.theme ?: ThemePreference.SYSTEM) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            // Iconos de barras de estado/navegación según el tema de la app (no solo el del sistema).
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkTheme },
                )
                onDispose { }
            }

            PuntoTheme(darkTheme = darkTheme, accent = current?.accent ?: AccentColor.TEAL) {
                // Surface fija el color de contenido: sin esto los textos salían negros sobre fondo oscuro.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (current != null) {
                        // Ya hay preferencias: la app está compuesta, se puede quitar el splash del sistema.
                        SideEffect { prefsReady = true }

                        // La intro animada solo sale si el tutorial YA se completó antes de abrir la app
                        // (en la primera apertura se ve el tutorial). Se decide una sola vez al arrancar.
                        val startedWithSetupDone = remember { current.onboardingCompleted }
                        // rememberSaveable: al rotar la pantalla no se vuelve a reproducir.
                        var introDone by rememberSaveable { mutableStateOf(false) }
                        LaunchedEffect(Unit) { if (!startedWithSetupDone) introDone = true }

                        Box(Modifier.fillMaxSize()) {
                            PuntoApp(onboardingCompleted = current.onboardingCompleted)
                            if (startedWithSetupDone && !introDone) {
                                PuntoIntro(onFinished = { introDone = true })
                            }
                        }
                    }
                }
            }
        }
    }
}
