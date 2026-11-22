package com.punto.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punto.app.domain.model.AccentColor
import com.punto.app.domain.model.ThemePreference
import com.punto.app.domain.repository.UserPreferences
import com.punto.app.domain.repository.UserPreferencesRepository
import com.punto.app.presentation.navigation.PuntoApp
import com.punto.app.presentation.theme.PuntoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    if (current != null) PuntoApp(onboardingCompleted = current.onboardingCompleted)
                }
            }
        }
    }
}
