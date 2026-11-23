package com.punto.app.presentation.setup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punto.app.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/*
 * Ilustraciones animadas del tutorial. Todo es Compose puro (Canvas + Animatable), sin librerías extra.
 *
 * Cada animación usa UNA sola línea de tiempo `t` (0→1) y cada elemento calcula su propio tramo con
 * phase(). Los valores de `t` se leen solo en la fase de dibujo (Canvas / graphicsLayer), así que
 * mientras se anima NO se recompone nada: es muy liviano. Solo corren mientras la página está activa.
 */

/** Rebote al caer (easeOutBounce). */
private val BounceOut = Easing { x ->
    val n1 = 7.5625f
    val d1 = 2.75f
    when {
        x < 1f / d1 -> n1 * x * x
        x < 2f / d1 -> { val y = x - 1.5f / d1; n1 * y * y + 0.75f }
        x < 2.5f / d1 -> { val y = x - 2.25f / d1; n1 * y * y + 0.9375f }
        else -> { val y = x - 2.625f / d1; n1 * y * y + 0.984375f }
    }
}

/** Progreso (0..1) de un tramo que ocurre entre [start] y [end] de la línea de tiempo [t]. */
private fun phase(t: Float, start: Float, end: Float, easing: Easing = FastOutSlowInEasing): Float =
    easing.transform(((t - start) / (end - start)).coerceIn(0f, 1f))

/** Línea de tiempo 0→1 que se repite mientras [active]; al volverse inactiva se detiene y se reinicia. */
@Composable
private fun rememberLoopingTimeline(active: Boolean, durationMs: Int, pauseMs: Long = 500): Animatable<Float, AnimationVector1D> {
    val t = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            while (true) {
                t.snapTo(0f)
                t.animateTo(1f, tween(durationMs, easing = LinearEasing))
                delay(pauseMs)
            }
        } else {
            t.snapTo(0f)
        }
    }
    return t
}

@Composable
private fun IllustrationCard(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content,
    )
}

/** Fondo tipo mapa: cuadrícula suave y una calle curva. */
private fun DrawScope.drawMapBackdrop(gridColor: Color, roadColor: Color) {
    val step = 28.dp.toPx()
    var x = step
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        x += step
    }
    var y = step
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        y += step
    }
    val road = Path().apply {
        moveTo(0f, size.height * 0.80f)
        cubicTo(size.width * 0.30f, size.height * 0.55f, size.width * 0.55f, size.height * 0.98f, size.width, size.height * 0.62f)
    }
    drawPath(road, roadColor, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
}

/** Pin de ubicación: [tip] es la punta inferior y [height] su altura total. */
private fun DrawScope.drawPin(tip: Offset, height: Float, color: Color, alpha: Float) {
    if (alpha <= 0f) return
    val r = height * 0.32f
    val center = Offset(tip.x, tip.y - height + r)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(center.x - r * 0.86f, center.y + r * 0.5f)
        arcTo(
            rect = Rect(Offset(center.x - r, center.y - r), Size(r * 2f, r * 2f)),
            startAngleDegrees = 150f,
            sweepAngleDegrees = 240f,
            forceMoveTo = false,
        )
        close()
    }
    drawPath(path, color.copy(alpha = alpha))
    drawCircle(Color.White.copy(alpha = alpha), r * 0.4f, center)
}

// ───────────────────────── 1) Guardar un lugar ─────────────────────────

@Composable
internal fun PlaceAnimation(active: Boolean) {
    val t = rememberLoopingTimeline(active, durationMs = 3800)
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val road = MaterialTheme.colorScheme.surfaceContainerHighest

    IllustrationCard {
        Canvas(Modifier.fillMaxSize()) {
            drawMapBackdrop(grid, road)
            val fade = 1f - phase(t.value, 0.93f, 1f, LinearEasing)
            val tip = Offset(size.width * 0.5f, size.height * 0.68f)

            // La zona (radio de detección) se abre después de que el pin cae.
            val z = phase(t.value, 0.32f, 0.62f)
            if (z > 0f) {
                val r = 82.dp.toPx() * z
                drawCircle(primary.copy(alpha = 0.14f * fade), r, tip)
                drawCircle(
                    primary.copy(alpha = 0.65f * fade),
                    r,
                    tip,
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))),
                )
            }

            // El pin cae con rebote y deja una sombrita.
            val drop = phase(t.value, 0.05f, 0.32f, BounceOut)
            drawOval(
                color = Color.Black.copy(alpha = 0.10f * drop * fade),
                topLeft = Offset(tip.x - 16.dp.toPx(), tip.y - 4.dp.toPx()),
                size = Size(32.dp.toPx(), 8.dp.toPx()),
            )
            val lift = (1f - drop) * 130.dp.toPx()
            drawPin(Offset(tip.x, tip.y - lift), 58.dp.toPx(), primary, fade * phase(t.value, 0.05f, 0.12f, LinearEasing))
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .graphicsLayer {
                    val p = phase(t.value, 0.62f, 0.78f)
                    alpha = p * (1f - phase(t.value, 0.93f, 1f, LinearEasing))
                    scaleX = 0.8f + 0.2f * p
                    scaleY = 0.8f + 0.2f * p
                },
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.tutorial_place_chip),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ───────────────────────── 2) Crear un recordatorio ─────────────────────────

@Composable
internal fun ReminderAnimation(active: Boolean) {
    val t = rememberLoopingTimeline(active, durationMs = 4200)
    val primary = MaterialTheme.colorScheme.primary
    val sample = stringResource(R.string.tutorial_reminder_sample)
    // Solo cambia ~15 veces por vuelta (una por letra), no en cada frame.
    val typed by remember(sample) {
        derivedStateOf { (phase(t.value, 0.12f, 0.46f, LinearEasing) * sample.length).toInt().coerceIn(0, sample.length) }
    }
    val arriveSelected by remember { derivedStateOf { t.value >= 0.56f } }

    IllustrationCard {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .graphicsLayer { alpha = 1f - phase(t.value, 0.93f, 1f, LinearEasing) },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Lugar elegido.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer { alpha = phase(t.value, 0f, 0.08f, LinearEasing) },
            ) {
                Icon(Icons.Outlined.LocationOn, null, tint = primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.tutorial_place_name), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            // Campo de nombre: se "escribe" solo.
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, primary),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                    Text(sample.take(typed), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                }
            }
            // Cuándo avisar.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriggerChip(stringResource(R.string.tutorial_trigger_arrive), selected = arriveSelected)
                TriggerChip(stringResource(R.string.tutorial_trigger_leave), selected = false)
            }
        }

        // Check de "guardado".
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(44.dp)
                .graphicsLayer {
                    val p = phase(t.value, 0.76f, 0.88f, FastOutSlowInEasing)
                    val s = p * (1f - phase(t.value, 0.93f, 1f, LinearEasing))
                    scaleX = s
                    scaleY = s
                    alpha = s
                }
                .clip(CircleShape)
                .background(primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun TriggerChip(label: String, selected: Boolean) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "chipContainer",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContent",
    )
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

// ───────────────────────── 3) Aviso al llegar ─────────────────────────

@Composable
internal fun AlertAnimation(active: Boolean) {
    val t = rememberLoopingTimeline(active, durationMs = 4800)
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val road = MaterialTheme.colorScheme.surfaceContainerHighest

    IllustrationCard {
        Canvas(Modifier.fillMaxSize()) {
            drawMapBackdrop(grid, road)
            val fade = 1f - phase(t.value, 0.94f, 1f, LinearEasing)
            val zoneCenter = Offset(size.width * 0.66f, size.height * 0.68f)
            val zoneRadius = 62.dp.toPx()

            // Zona fija.
            drawCircle(primary.copy(alpha = 0.14f * fade), zoneRadius, zoneCenter)
            drawCircle(
                primary.copy(alpha = 0.65f * fade),
                zoneRadius,
                zoneCenter,
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))),
            )
            // Onda cuando la persona entra a la zona.
            val pulse = phase(t.value, 0.40f, 0.66f, LinearOutSlowInEasing)
            if (pulse > 0f && pulse < 1f) {
                drawCircle(
                    primary.copy(alpha = 0.5f * (1f - pulse) * fade),
                    zoneRadius + 44.dp.toPx() * pulse,
                    zoneCenter,
                    style = Stroke(3.dp.toPx()),
                )
            }

            // La persona camina hacia la zona.
            val start = Offset(size.width * 0.12f, size.height * 0.88f)
            val walk = phase(t.value, 0.08f, 0.60f, LinearEasing)
            val pos = Offset(start.x + (zoneCenter.x - start.x) * walk, start.y + (zoneCenter.y - start.y) * walk)
            val appear = phase(t.value, 0f, 0.06f, LinearEasing) * fade
            drawLine(
                color = primary.copy(alpha = 0.35f * appear),
                start = start,
                end = pos,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
            drawCircle(Color.White.copy(alpha = appear), 11.dp.toPx(), pos)
            drawCircle(primary.copy(alpha = appear), 7.dp.toPx(), pos)
        }

        // Notificación que baja desde arriba cuando se entra a la zona.
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = phase(t.value, 0.42f, 0.55f)
                    translationY = -(1f - p) * 90.dp.toPx()
                    alpha = p * (1f - phase(t.value, 0.94f, 1f, LinearEasing))
                },
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            // La campana se sacude y se va calmando.
                            val w = phase(t.value, 0.50f, 0.76f, LinearEasing)
                            rotationZ = sin(w * 6f * PI.toFloat()) * 18f * (1f - w)
                        },
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.tutorial_alert_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.tutorial_alert_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
