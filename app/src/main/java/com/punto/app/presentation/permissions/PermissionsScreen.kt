package com.punto.app.presentation.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.punto.app.R

/** Tarjetas de permisos (sin título ni botones: los pone la pantalla que las aloja). */
@Composable
fun PermissionsContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refreshTick++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // refreshTick se lee aquí para recalcular al volver de Ajustes o del diálogo del sistema.
    val fine = refreshTick >= 0 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val bg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED else true
    val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true

    var showBgRationale by remember { mutableStateOf(false) }
    val fgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshTick++ }
    val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshTick++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshTick++ }

    Column(modifier.animateContentSize()) {
        PermCard(Icons.Outlined.LocationOn, R.string.perm_location_title, R.string.perm_location_desc, granted = fine) {
            fgLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        PermCard(Icons.Outlined.MyLocation, R.string.perm_background_title, R.string.perm_background_desc, granted = bg) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!fine) fgLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) showBgRationale = true
                else bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
        PermCard(Icons.Outlined.Notifications, R.string.perm_notifications_title, R.string.perm_notifications_desc, granted = notif) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (showBgRationale && !bg) {
            Text(
                "Android pide conceder la ubicación en segundo plano desde Ajustes, eligiendo 'Permitir siempre'.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = {
                showBgRationale = false
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            }, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Abrir Ajustes") }
        }
    }
}

@Composable
private fun PermCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: Int,
    desc: Int,
    granted: Boolean,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!granted) {
                    TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) { Text("Permitir") }
                } else {
                    Spacer(Modifier.padding(top = 4.dp))
                }
            }
            AnimatedVisibility(visible = granted, enter = scaleIn() + fadeIn()) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Concedido", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp).size(24.dp))
            }
        }
    }
}
