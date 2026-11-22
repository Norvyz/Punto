package com.punto.app.presentation.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punto.app.R
import com.punto.app.presentation.components.AccentSwatches
import com.punto.app.presentation.components.StatusPill
import com.punto.app.presentation.components.ThemeSegmentedRow
import com.punto.app.presentation.permissions.PermissionsContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val PAGE_COUNT = 6

/**
 * Configuración inicial (solo la primera vez): bienvenida, tutorial animado de 3 pasos
 * (guardar lugar, crear recordatorio, recibir aviso), apariencia (tema + color) y permisos.
 * También se reutiliza desde Ajustes ("Ver tutorial").
 */
@Composable
fun SetupScreen(onFinished: () -> Unit, viewModel: SetupViewModel = hiltViewModel()) {
    val prefs by viewModel.uiState.collectAsStateWithLifecycle()
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
                TextButton(onClick = ::finish) { Text(stringResource(R.string.welcome_skip)) }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .pageTransition(pagerState, page),
                contentAlignment = Alignment.TopCenter,
            ) {
                val active = pagerState.currentPage == page
                when (page) {
                    0 -> WelcomePage(active)
                    1 -> TutorialPage(active, 1, R.string.onboarding_1_title, R.string.onboarding_1_desc) { PlaceAnimation(active) }
                    2 -> TutorialPage(active, 2, R.string.onboarding_2_title, R.string.onboarding_2_desc) { ReminderAnimation(active) }
                    3 -> TutorialPage(active, 3, R.string.onboarding_3_title, R.string.onboarding_3_desc) { AlertAnimation(active) }
                    4 -> AppearancePage(
                        active = active,
                        theme = prefs.theme,
                        accent = prefs.accent,
                        onTheme = viewModel::setTheme,
                        onAccent = viewModel::setAccent,
                    )
                    else -> PermissionsPage(active)
                }
            }
        }

        PagerDots(pagerState)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (isLast) finish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            val labelRes = when (pagerState.currentPage) {
                0 -> R.string.welcome_start
                PAGE_COUNT - 1 -> R.string.onboarding_done
                else -> R.string.onboarding_next
            }
            AnimatedContent(
                targetState = labelRes,
                transitionSpec = { (fadeIn(tween(180)) togetherWith fadeOut(tween(120))) },
                label = "setupButtonLabel",
            ) { res -> Text(stringResource(res), maxLines = 1) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Desvanece y encoge un poco la página mientras se desliza. */
private fun Modifier.pageTransition(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
    alpha = 1f - 0.7f * offset
    val s = 1f - 0.06f * offset
    scaleX = s
    scaleY = s
}

@Composable
private fun PagerDots(pagerState: PagerState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(PAGE_COUNT) { i ->
            val selected = pagerState.currentPage == i
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(Modifier.padding(horizontal = 4.dp).height(8.dp).width(width).clip(CircleShape).background(color))
        }
    }
}

/** Aparece con fundido + desplazamiento cuando su página pasa a ser la activa (sin mover el layout). */
@Composable
private fun Reveal(visible: Boolean, delayMillis: Int = 0, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible && !shown) {
            delay(delayMillis.toLong())
            shown = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "reveal",
    )
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24.dp.toPx()
        },
    ) { content() }
}

@Composable
private fun PageHeader(title: String, subtitle: String, active: Boolean, delayMillis: Int = 0) {
    Reveal(active, delayMillis) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ───────────────────────── Página 1: bienvenida ─────────────────────────

@Composable
private fun WelcomePage(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "welcomePulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Reveal(active) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(156.dp).scale(pulse).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)))
                Box(Modifier.size(116.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(56.dp))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Reveal(active, 150) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Reveal(active, 250) {
            Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(10.dp))
        Reveal(active, 350) {
            Text(stringResource(R.string.welcome_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ───────────────────────── Páginas 2-4: tutorial animado ─────────────────────────

/** Una página del tutorial: "Paso N de 3", la ilustración animada y su explicación. */
@Composable
private fun TutorialPage(
    active: Boolean,
    step: Int,
    titleRes: Int,
    descRes: Int,
    illustration: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Reveal(active) {
            Text(
                stringResource(R.string.tutorial_step, step),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(16.dp))
        Reveal(active, 100) { illustration() }
        Spacer(Modifier.height(24.dp))
        PageHeader(stringResource(titleRes), stringResource(descRes), active, 250)
    }
}

// ───────────────────────── Página 5: apariencia ─────────────────────────

@Composable
private fun AppearancePage(
    active: Boolean,
    theme: com.punto.app.domain.model.ThemePreference,
    accent: com.punto.app.domain.model.AccentColor,
    onTheme: (com.punto.app.domain.model.ThemePreference) -> Unit,
    onAccent: (com.punto.app.domain.model.AccentColor) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        PageHeader("Elige tu estilo", "Puedes cambiarlo después en Ajustes.", active)
        Spacer(Modifier.height(20.dp))
        Reveal(active, 150) {
            Column(Modifier.fillMaxWidth()) {
                Text("Tema", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                ThemeSegmentedRow(theme, onTheme)
            }
        }
        Spacer(Modifier.height(18.dp))
        Reveal(active, 300) {
            Column(Modifier.fillMaxWidth()) {
                Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                AccentSwatches(accent, onAccent)
            }
        }
        Spacer(Modifier.height(22.dp))
        Reveal(active, 450) { PreviewCard() }
    }
}

/** Vista previa en vivo: cambia al instante con el tema y el color elegidos. */
@Composable
private fun PreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Votar basura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Aseo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill("Activo", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("Casa · Al llegar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ───────────────────────── Página 6: permisos ─────────────────────────

@Composable
private fun PermissionsPage(active: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        PageHeader(stringResource(R.string.permissions_title), stringResource(R.string.permissions_subtitle), active)
        Spacer(Modifier.height(16.dp))
        Reveal(active, 150) { PermissionsContent() }
    }
}
