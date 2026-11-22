package com.punto.app.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punto.app.domain.model.AccentColor
import com.punto.app.domain.model.ThemePreference
import com.punto.app.domain.repository.UserPreferences
import com.punto.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<UserPreferences> = prefs.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly, UserPreferences(),
    )

    fun setTheme(theme: ThemePreference) { viewModelScope.launch { prefs.setTheme(theme) } }
    fun setAccent(accent: AccentColor) { viewModelScope.launch { prefs.setAccent(accent) } }

    /** Marca la configuración inicial como hecha; así la bienvenida no vuelve a salir. */
    fun complete() {
        viewModelScope.launch { withContext(NonCancellable) { prefs.setOnboardingCompleted(true) } }
    }
}
