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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punto.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Mismo trazo del pin que res/drawable/ic_splash_pin.xml (viewport de 108 x 108). */
private const val PIN_PATH =
    "M54,24C43.51,24 35,32.51 35,43c0,14.25 19,33 19,33s19,-18.75 19,-33c0,-10.49 -8.51,-19 -19,-19z" +
        "M54,50.5c-4.14,0 -7.5,-3.36 -7.5,-7.5s3.36,-7.5 7.5,-7.5 7.5,3.36 7.5,7.5 -3.36,7.5 -7.5,7.5z"

/**
 * Animación que se ve cada vez que se abre la app (después del tutorial).
 *
 * Continúa el splash del sistema: arranca con el mismo pin, mismo fondo y misma posición. Luego el
 * pin da un salto, cae y se expande la "zona" (las ondas son el radio del geofence), y aparece el
 * nombre. Dura ~1.6 s y se puede saltar tocando la pantalla.
 *
 * Se dibuja ENCIMA de la app, así que mientras suena la app ya está cargando sus datos.
 */
@Composable
fun PuntoIntro(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val background = colorResource(R.color.punto_splash_background)
    val pinColor = colorResource(R.color.punto_splash_icon)
    val accent = MaterialTheme.colorScheme.primary
    val pinPath = remember { PathParser().parsePathString(PIN_PATH).toPath() }

    val hop = remember { Animatable(0f) }
    val ripple1 = remember { Animatable(0f) }
    val ripple2 = remember { Animatable(0f) }
    val zone = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }

    var finished by remember { mutableStateOf(false) }
    val latestOnFinished by rememberUpdatedState(onFinished)
    fun finish() {
        if (!finished) {
            finished = true
            latestOnFinished()
        }
    }

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
        delay(150)
        overlayAlpha.animateTo(0f, tween(250))
        finish()
    }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(background)
            // Toque = saltar. También evita que los toques "atraviesen" hacia la app que hay debajo.
            .pointerInput(Unit) { detectTapGestures { finish() } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = 288.dp.toPx() / 108f // misma escala que el ícono del splash (288dp = 108 unidades)
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
                drawCircle(color = pinColor, radius = 3.5f, center = Offset(54f, 43f))
            }
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .offset(y = 130.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = pinColor,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 16.dp.toPx()
                },
            )
            Text(
                text = stringResource(R.string.intro_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = pinColor.copy(alpha = 0.75f),
                modifier = Modifier.graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 12.dp.toPx()
                },
            )
        }
    }
}
