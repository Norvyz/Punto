package com.punto.app.presentation.settings

import android.Manifest
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.media.Ringtone
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punto.app.R
import com.punto.app.domain.model.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refreshTick++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = if (Build.VERSION.SDK_INT >= 33) result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        else @Suppress("DEPRECATION") result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        viewModel.setSound(uri?.toString())
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsGroup(Icons.Outlined.DarkMode, stringResource(R.string.settings_appearance)) {
                    val options = listOf(ThemePreference.SYSTEM to R.string.settings_theme_system, ThemePreference.LIGHT to R.string.settings_theme_light, ThemePreference.DARK to R.string.settings_theme_dark)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, (value, res) ->
                            SegmentedButton(selected = prefs.theme == value, onClick = { viewModel.setTheme(value) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) { Text(stringResource(res)) }
                        }
                    }
                }
            }
            item {
                SettingsGroup(Icons.Outlined.Notifications, stringResource(R.string.settings_notifications)) {
                    val soundName = prefs.notificationSoundUri?.let { RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) } ?: "Predeterminado"
                    SettingRow(stringResource(R.string.settings_sound), soundName)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, prefs.notificationSoundUri?.let(Uri::parse))
                            }
                            soundPicker.launch(intent)
                        }) { Text("Seleccionar") }
                        TextButton(onClick = {
                            runCatching {
                                val ringtone: Ringtone? = RingtoneManager.getRingtone(context, prefs.notificationSoundUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                                ringtone?.play()
                            }
                        }) { Text("Probar") }
                    }
                    SettingToggle(stringResource(R.string.settings_vibration), prefs.vibrationEnabled, viewModel::setVibration)
                }
            }
            item {
                SettingsGroup(Icons.Outlined.Tune, "Valores predeterminados") {
                    OptionRow("Radio predeterminado", "${prefs.defaultRadiusMeters} m", listOf(50, 100, 150, 300)) { viewModel.setDefaultRadius(it) }
                    OptionRow("Permanencia predeterminada", "${prefs.defaultDwellMinutes} min", listOf(5, 10, 15, 30, 60)) { viewModel.setDefaultDwell(it) }
                    OptionRow("Repetición predeterminada", "${prefs.defaultRepeatIntervalMinutes} min", listOf(5, 15, 30, 60)) { viewModel.setDefaultRepeat(it) }
                    OptionRow("Posponer por defecto", "${prefs.defaultSnoozeMinutes} min", listOf(5, 10, 15, 30, 60)) { viewModel.setDefaultSnooze(it) }
                    val limitLabel = prefs.defaultMaxNotifications?.let { "$it avisos" } ?: "Sin límite"
                    OptionRow("Límite de avisos", limitLabel, listOf<Int?>(null, 3, 5, 10)) { viewModel.setDefaultMax(it) }
                }
            }
            item {
                SettingsGroup(Icons.Outlined.PrivacyTip, stringResource(R.string.settings_privacy)) {
                    refreshTick // force recomposition on resume
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val bg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED else true
                    val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED else true
                    Text(stringResource(R.string.settings_privacy_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    StatusRow("Ubicación", if (fine) "Concedido" else "Pendiente")
                    StatusRow("Ubicación en segundo plano", if (bg) "Concedido" else "Pendiente / Solo con 'Permitir siempre'")
                    StatusRow("Notificaciones", if (notif) "Concedido" else "Pendiente")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }) { Text("Abrir ajustes de Android") }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SettingsGroup(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.padding(horizontal = 6.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp)); content()
        }
    }
}

@Composable private fun SettingRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun StatusRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, style = MaterialTheme.typography.bodyMedium); Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) } }
@Composable private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label); Switch(checked = checked, onCheckedChange = onCheckedChange) } }

@Composable
private fun <T> OptionRow(label: String, current: String, options: List<T>, render: (T) -> String = { it.toString() }, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label); TextButton(onClick = { expanded = !expanded }) { Text(current) }
        }
        if (expanded) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { opt -> TextButton(onClick = { onSelect(opt); expanded = false }) { Text(render(opt)) } }
        }
    }
}
