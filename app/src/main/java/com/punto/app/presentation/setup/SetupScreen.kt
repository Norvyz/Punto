package com.punto.app.presentation.setup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.punto.app.R
import com.punto.app.presentation.components.AccentSwatches
import com.punto.app.presentation.components.ThemeSegmentedRow
import com.punto.app.presentation.permissions.PermissionsContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.absoluteValue

private const val PAGE_COUNT = 7

@Composable
fun SetupScreen(onFinished: () -> Unit, viewModel: SetupViewModel = hiltViewModel()) {
    val prefs by viewModel.uiState.collectAsStateWithLifecycle()
    val flow by viewModel.flowState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGE_COUNT - 1

    fun finish() {
        viewModel.complete()
        onFinished()
    }

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(visible = !isLast, enter = fadeIn(), exit = fadeOut()) {
                TextButton(onClick = ::finish) { Text("Saltar por ahora") }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), userScrollEnabled = true) { page ->
            Box(Modifier.fillMaxSize().pageTransition(pagerState, page), contentAlignment = Alignment.TopCenter) {
                val active = pagerState.currentPage == page
                when (page) {
                    0 -> WelcomePage(active)
                    1 -> LocationStepPage(active, flow, viewModel)
                    2 -> ReminderStepPage(active, flow, viewModel)
                    3 -> TestStepPage(active, flow)
                    4 -> AppearancePage(active, prefs.theme, prefs.accent, viewModel::setTheme, viewModel::setAccent)
                    5 -> PermissionsPage(active)
                    6 -> FinalPage(active, flow.sampleLocationName, flow.sampleReminderName)
                }
            }
        }

        if (flow.error != null) {
            Text(flow.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }

        PagerDots(pagerState)
        Spacer(Modifier.height(16.dp))
        val btnEnabled = when (pagerState.currentPage) {
            1 -> flow.locationSaved
            2 -> flow.reminderSaved
            else -> true
        }
        val btnScale by animateFloatAsState(if (btnEnabled) 1f else 0.98f, label = "btnScale")
        Button(
            onClick = {
                if (isLast) finish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            enabled = btnEnabled,
            modifier = Modifier.fillMaxWidth().height(54.dp).graphicsLayer { scaleX = btnScale; scaleY = btnScale },
        ) {
            val label = when (pagerState.currentPage) {
                0 -> "Empezar guía"
                1 -> if (flow.locationSaved) "Continuar" else "Guarda un lugar para continuar"
                2 -> if (flow.reminderSaved) "Continuar" else "Crea un recordatorio para continuar"
                PAGE_COUNT - 1 -> "Comenzar a usar Punto"
                else -> "Continuar"
            }
            AnimatedContent(targetState = label, transitionSpec = { fadeIn(tween(220)) + slideInVertically { it / 2 } togetherWith fadeOut(tween(120)) }, label = "btn") {
                Text(it, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun Modifier.pageTransition(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
    alpha = 1f - 0.7f * offset
    val s = 1f - 0.06f * offset
    scaleX = s; scaleY = s
}

@Composable
private fun PagerDots(pagerState: PagerState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(PAGE_COUNT) { i ->
            val selected = pagerState.currentPage == i
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dw")
            val color by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "dc")
            Box(Modifier.padding(horizontal = 4.dp).height(8.dp).width(width).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun Reveal(visible: Boolean, delayMillis: Int = 0, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible && !shown) { delay(delayMillis.toLong()); shown = true } }
    val progress by animateFloatAsState(if (shown) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "rev")
    Box(Modifier.graphicsLayer { alpha = progress; translationY = (1f - progress) * 24.dp.toPx() }) { content() }
}

@Composable
private fun WelcomePage(active: Boolean) {
    val pulse by rememberInfiniteTransition("pulse").animateFloat(1f, 1.14f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), "p")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Reveal(active) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(164.dp).scale(pulse).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)))
                Box(Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(58.dp))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Reveal(active, 120) { Text("Bienvenido a Punto", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        Reveal(active, 220) { Text("Recordatorios que te encuentran\nen el lugar correcto", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(12.dp))
        Reveal(active, 320) { Text("Vamos a configurar tu primer recordatorio juntos, paso a paso. Será rápido y interactivo.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun StepHeader(step: Int, total: Int, title: String, subtitle: String, active: Boolean) {
    Reveal(active) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Paso $step de $total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LocationStepPage(active: Boolean, flow: SetupUiState, vm: SetupViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var isLocating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Mi casa") }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(1, 4, "Guarda tu primer lugar", "Usa tu ubicación actual para crear un lugar real. Así Punto sabrá dónde recordarte.", active)
        Spacer(Modifier.height(20.dp))
        Reveal(active, 120) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre del lugar") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (!hasPermission) {
                        OutlinedButton(onClick = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Permitir ubicación") }
                    } else {
                        Button(onClick = {
                            scope.launch {
                                isLocating = true
                                val loc = getCurrentLocation(context)
                                isLocating = false
                                if (loc != null) { lat = loc.first; lng = loc.second; address = loc.third } else vm.clearError()
                            }
                        }, enabled = !isLocating, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.MyLocation, null); Spacer(Modifier.width(8.dp))
                            Text(if (isLocating) "Obteniendo ubicación..." else "Usar mi ubicación actual")
                        }
                    }
                    if (lat != null && lng != null) {
                        Text("Lat: ${"%.6f".format(lat)}  Lng: ${"%.6f".format(lng)}", style = MaterialTheme.typography.bodySmall)
                        Text(address ?: "Dirección no disponible, pero las coordenadas ya sirven.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { vm.saveSampleLocation(name, lat ?: return@Button, lng ?: return@Button, address) }, enabled = lat != null && !flow.isSavingLocation && !flow.locationSaved, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (flow.locationSaved) Icons.Outlined.CheckCircle else Icons.Outlined.AddLocationAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (flow.locationSaved) "Lugar guardado" else if (flow.isSavingLocation) "Guardando..." else "Guardar lugar")
                    }
                    if (flow.locationSaved) StatusChip("Lugar listo: ${flow.sampleLocationName ?: name}")
                }
            }
        }
    }
}

@Composable
private fun ReminderStepPage(active: Boolean, flow: SetupUiState, vm: SetupViewModel) {
    var name by remember { mutableStateOf("Comprar leche") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(2, 4, "Crea tu primer recordatorio", "Asocia una tarea al lugar que acabas de guardar. Cuando llegues, Punto te lo recordará.", active)
        Spacer(Modifier.height(20.dp))
        Reveal(active, 120) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lugar: ${flow.sampleLocationName ?: "Primero guarda un lugar"}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(name, { name = it }, label = { Text("¿Qué quieres recordar?") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("Activación: Al llegar • Repetición: cada 15 min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { vm.saveSampleReminder(name) }, enabled = flow.locationSaved && !flow.isSavingReminder && !flow.reminderSaved, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (flow.reminderSaved) Icons.Outlined.CheckCircle else Icons.Outlined.NotificationsActive, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (flow.reminderSaved) "Recordatorio creado" else if (flow.isSavingReminder) "Guardando..." else "Crear recordatorio")
                    }
                    if (flow.reminderSaved) StatusChip("Recordatorio listo: ${flow.sampleReminderName ?: name}")
                }
            }
        }
    }
}

@Composable
private fun TestStepPage(active: Boolean, flow: SetupUiState) {
    var step by remember { mutableStateOf(0) }
    var showAlert by remember { mutableStateOf(false) }
    LaunchedEffect(active, flow.reminderSaved) {
        if (active && flow.reminderSaved) {
            step = 0; showAlert = false
            delay(500); step = 1
            delay(800); step = 2
            delay(800); step = 3
            delay(500); showAlert = true
        } else if (!active) { showAlert = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(3, 4, "Mira cómo funcionará", "Esta es una simulación guiada del flujo real: llegas al lugar, Punto detecta y te avisa.", active)
        Spacer(Modifier.height(20.dp))
        Reveal(active, 120) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SimRow("Llegando a ${flow.sampleLocationName ?: "tu lugar"}", step >= 1)
                    SimRow("Recordatorio detectado: ${flow.sampleReminderName ?: "tu recordatorio"}", step >= 2)
                    SimRow("Notificación enviada con acciones: Hecho / Posponer", step >= 3)

                    AnimatedVisibility(
                        visible = showAlert,
                        enter = fadeIn(tween(260)) + androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = tween(320, easing = FastOutSlowInEasing)) + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(320, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(180))
                    ) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Punto • ahora", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(flow.sampleReminderName ?: "Tu recordatorio", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Estás en . ¿Quieres marcarlo como hecho?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                                        Text("Hecho", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)) {
                                        Text("Posponer", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun SimRow(text: String, done: Boolean) {
    val color by animateColorAsState(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "sim")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CheckCircle, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppearancePage(active: Boolean, theme: com.punto.app.domain.model.ThemePreference, accent: com.punto.app.domain.model.AccentColor, onTheme: (com.punto.app.domain.model.ThemePreference) -> Unit, onAccent: (com.punto.app.domain.model.AccentColor) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(4, 4, "Dale tu estilo", "Elige tema y color. Puedes cambiarlo después en Ajustes.", active)
        Spacer(Modifier.height(20.dp))
        Reveal(active, 120) {
            Column(Modifier.fillMaxWidth()) {
                Text("Tema", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                ThemeSegmentedRow(theme, onTheme)
                Spacer(Modifier.height(18.dp))
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                AccentSwatches(accent, onAccent)
            }
        }
    }
}

@Composable
private fun PermissionsPage(active: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Reveal(active) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Permisos importantes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("Para que Punto funcione bien, necesita estos permisos. Puedes concederlos ahora o después en Ajustes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(16.dp))
        Reveal(active, 120) { PermissionsContent() }
    }
}

@Composable
private fun FinalPage(active: Boolean, locationName: String?, reminderName: String?) {
    val scale by animateFloatAsState(if (active) 1f else 0.9f, tween(500, easing = FastOutSlowInEasing), label = "fs")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.scale(scale).size(110.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("¡Todo listo!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            if (locationName != null && reminderName != null) "Ya guardaste \"$locationName\" y creaste \"$reminderName\". Punto te acompañará en el momento justo."
            else "Punto está listo para ayudarte a recordar lo importante en el lugar correcto.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private suspend fun getCurrentLocation(context: Context): Triple<Double, Double, String?>? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
        val current = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
        val loc = current ?: client.lastLocation.await() ?: return null
        val address = withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont -> geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { cont.resume(it) } }
                } else @Suppress("DEPRECATION") { geocoder.getFromLocation(loc.latitude, loc.longitude, 1) }
                results?.firstOrNull()?.getAddressLine(0)
            }.getOrNull()
        }
        Triple(loc.latitude, loc.longitude, address)
    } catch (_: Exception) { null }
}