package com.punto.app.presentation.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punto.app.R
import com.punto.app.presentation.rive.PuntoRiveFiles
import com.punto.app.presentation.rive.RiveOrFallback
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Trazo del pin (viewport de 108 x 108). */
private const val PIN_PATH =
    "M54,24C43.51,24 35,32.51 35,43c0,14.25 19,33 19,33s19,-18.75 19,-33c0,-10.49 -8.51,-19 -19,-19z" +
        "M54,50.5c-4.14,0 -7.5,-3.36 -7.5,-7.5s3.36,-7.5 7.5,-7.5 7.5,3.36 7.5,7.5 -3.36,7.5 -7.5,7.5z"

/** Tiempo máximo que se espera a que un .riv de intro termine (por si no dispara "finished"). */
private const val RIVE_INTRO_TIMEOUT_MS = 4_000L

/**
 * Intro de marca que se ve en CADA apertura de la app (también la primera, antes del tutorial).
 *
 * El splash del sistema ahora es solo el color de fondo del tema (sin ícono), así que esta intro
 * es la única animación y no hay dos pines superpuestos. Todos sus colores salen de
 * MaterialTheme (modo claro/oscuro + color de acento elegido), por eso siempre cuadra con la app.
 *
 * Si existe assets/rive/punto_intro.riv se usa Rive; si no, la animación en Compose de abajo.
 * Se puede saltar tocando la pantalla.
 */
@Composable
fun PuntoIntroOverlay(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    var finished by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(onFinished)
    val finish: () -> Unit = {
        if (!finished) {
            finished = true
            latest()
        }
    }
    val alpha = remember { Animatable(1f) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(closing) {
        if (closing) {
            alpha.animateTo(0f, tween(250))
            finish()
        }
    }
    val close: () -> Unit = { closing = true }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(MaterialTheme.colorScheme.background)
            // Toque = saltar; además evita que los toques atraviesen hacia la app.
            .pointerInput(Unit) { detectTapGestures { close() } },
    ) {
        RiveOrFallback(
            file = PuntoRiveFiles.INTRO,
            artboard = "Intro",
            modifier = Modifier.fillMaxSize(),
            onFinished = close,
            reserveSpace = false,
            fallback = { PuntoIntroCompose(onDone = close) },
        )
        // Seguro: si hay .riv pero nunca avisa que terminó, se cierra igual.
        LaunchedEffect(Unit) {
            delay(RIVE_INTRO_TIMEOUT_MS)
            close()
        }
    }
}

@Composable
private fun PuntoIntroCompose(onDone: () -> Unit) {
    val pinColor = MaterialTheme.colorScheme.primary
    val titleColor = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val pinPath = remember { PathParser().parsePathString(PIN_PATH).toPath().apply { fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd } }
    val latestDone by rememberUpdatedState(onDone)

    val hop = remember { Animatable(0f) }
    val ripple1 = remember { Animatable(0f) }
    val ripple2 = remember { Animatable(0f) }
    val zone = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // 1) El pin salta y cae con un pequeño rebote.
            launch {
                hop.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
                hop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
            // 2) Al caer, se abre la zona: dos ondas y un círculo suave.
            launch {
                delay(300)
                launch { ripple1.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) }
                launch { delay(180); ripple2.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) }
                launch { zone.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
            }
            // 3) Nombre y frase.
            launch { delay(450); title.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
            launch { delay(700); tagline.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
        }
        delay(500)
        latestDone()
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = 150.dp.toPx() / 108f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val origin = Offset(cx - 54f * unit, cy - 54f * unit)
            val tip = Offset(cx, cy + 22f * unit) // punta del pin (54, 76)

            // Zona (radio del geofence).
            drawCircle(
                color = accent.copy(alpha = 0.12f * zone.value),
                radius = 96.dp.toPx() * (0.6f + 0.4f * zone.value),
                center = tip,
            )
            // Ondas que se expanden y se desvanecen.
            listOf(ripple1.value, ripple2.value).forEach { p ->
                if (p > 0f && p < 1f) {
                    drawCircle(
                        color = accent.copy(alpha = 0.45f * (1f - p)),
                        radius = 40.dp.toPx() + 130.dp.toPx() * p,
                        center = tip,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
            // Sombra bajo el pin: se achica cuando el pin sube.
            val shadowW = 44.dp.toPx() * (1f - 0.3f * hop.value)
            val shadowH = 12.dp.toPx() * (1f - 0.3f * hop.value)
            drawOval(
                color = Color.Black.copy(alpha = 0.12f * (1f - hop.value)),
                topLeft = Offset(tip.x - shadowW / 2f, tip.y - shadowH / 2f),
                size = Size(shadowW, shadowH),
            )
            // Pin (con salto).
            val lift = hop.value * 30.dp.toPx()
            withTransform({
                translate(origin.x, origin.y - lift)
                scale(unit, unit, pivot = Offset.Zero)
            }) {
                drawPath(pinPath, color = pinColor)
            }
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .offset(y = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 16.dp.toPx()
                },
            )
            Text(
                text = stringResource(R.string.intro_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = titleColor.copy(alpha = 0.7f),
                modifier = Modifier.graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 12.dp.toPx()
                },
            )
        }
    }
}
