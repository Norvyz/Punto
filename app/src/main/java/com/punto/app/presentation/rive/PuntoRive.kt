package com.punto.app.presentation.rive

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import app.rive.Artboard
import app.rive.Fit
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.RivePointerInputMode
import app.rive.ViewModelInstance
import app.rive.ViewModelSource
import app.rive.core.RiveWorker
import app.rive.rememberArtboardResult
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import app.rive.rememberViewModelInstanceResult
import app.rive.runtime.kotlin.core.Rive as RiveCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/*
 * Integración de Rive (API Compose, versión 11.x).
 *
 * Idea central: la app funciona IGUAL con o sin archivos .riv.
 *   - Si NO hay .riv en app/src/main/assets/rive/  → se usan las animaciones en Compose de siempre (fallback).
 *   - Si SÍ hay                                    → se usa Rive, y la app le pasa los colores del tema
 *                                                    (claro/oscuro + color de acento) por data binding.
 * Si un .riv falta, está dañado o no tiene el artboard esperado, se cae al fallback en silencio.
 * El contrato exacto (nombres de artboards y de propiedades) está en RIVE_GUIA.md.
 */

/** Archivos .riv que la app busca en app/src/main/assets/rive/. */
object PuntoRiveFiles {
    const val DIR = "rive"

    /** Artboard "Intro": logo/mascota que se ve al abrir la app. */
    const val INTRO = "punto_intro.riv"

    /** Artboards "Step1", "Step2", "Step3": los tres pasos del tutorial. */
    const val TUTORIAL = "punto_tutorial.riv"

    /** Artboard "Mascot": la mascota (bienvenida y pantallas vacías). */
    const val MASCOT = "punto_mascot.riv"
}

/** Propiedades del View Model de Rive (data binding) que la app lee o escribe. Todas son opcionales. */
object PuntoRiveProps {
    // App → Rive: colores del tema actual (AARRGGBB) y modo.
    const val IS_DARK = "isDark" // Boolean
    const val PRIMARY = "primary" // Color
    const val ON_PRIMARY = "onPrimary" // Color
    const val PRIMARY_CONTAINER = "primaryContainer" // Color
    const val BACKGROUND = "background" // Color
    const val SURFACE = "surface" // Color
    const val ON_SURFACE = "onSurface" // Color

    /** App → Rive: true cuando la página/pantalla está visible (para arrancar la animación al verla). */
    const val ACTIVE = "active" // Boolean

    /** Rive → App: la animación terminó (la app cierra la intro). */
    const val FINISHED = "finished" // Trigger

    /** App → Rive: estado de ánimo de la mascota. */
    const val MOOD = "mood" // Enum: idle | happy | sleepy | alert
}

enum class MascotMood(val value: String) { IDLE("idle"), HAPPY("happy"), SLEEPY("sleepy"), ALERT("alert") }

// ───────────────────────── Estado global de Rive ─────────────────────────

sealed interface PuntoRiveState {
    /** Todavía se está mirando si hay archivos .riv (tarda milisegundos). */
    data object Checking : PuntoRiveState

    /** No hay .riv, o Rive no pudo iniciar: se usan las animaciones de siempre. */
    data object Unavailable : PuntoRiveState

    /** Rive listo: [worker] es el hilo de Rive compartido por todas las animaciones. */
    class Ready internal constructor(
        internal val worker: RiveWorker,
        private val context: Context,
    ) : PuntoRiveState {
        private val cache = ConcurrentHashMap<String, ByteArray>()

        /** Bytes de assets/rive/[name], o null si ese archivo no existe. */
        suspend fun bytes(name: String): ByteArray? {
            cache[name]?.let { return it }
            val loaded = withContext(Dispatchers.IO) {
                runCatching { context.assets.open("${PuntoRiveFiles.DIR}/$name").use { it.readBytes() } }.getOrNull()
            }
            if (loaded != null) cache[name] = loaded
            return loaded
        }
    }
}

val LocalPuntoRive = staticCompositionLocalOf<PuntoRiveState> { PuntoRiveState.Unavailable }

private object RiveNative {
    @Volatile private var ready = false

    /** Carga la librería nativa de Rive una sola vez por proceso. false si no se pudo. */
    @Synchronized
    fun ensure(context: Context): Boolean {
        if (ready) return true
        return try {
            RiveCore.init(context.applicationContext)
            ready = true
            true
        } catch (t: Throwable) {
            false
        }
    }
}

/**
 * Pone Rive a disposición del resto de la app. Si no hay .riv en assets/rive/ no carga nada
 * (ni la librería nativa ni el hilo de Rive), así que no cuesta arranque ni memoria.
 */
@Composable
fun ProvidePuntoRive(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext

    val hasFiles by produceState<Boolean?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.list(PuntoRiveFiles.DIR).orEmpty().any { it.endsWith(".riv") } }
                .getOrDefault(false)
        }
    }
    var nativeReady by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(hasFiles) {
        if (hasFiles == true) nativeReady = withContext(Dispatchers.Default) { RiveNative.ensure(context) }
    }

    val state: PuntoRiveState = when {
        hasFiles == null -> PuntoRiveState.Checking
        hasFiles == false -> PuntoRiveState.Unavailable
        nativeReady == null -> PuntoRiveState.Checking
        nativeReady == false -> PuntoRiveState.Unavailable
        else -> {
            val error = remember { mutableStateOf<Throwable?>(null) }
            val worker = rememberRiveWorkerOrNull(error)
            if (worker == null) PuntoRiveState.Unavailable else remember(worker) { PuntoRiveState.Ready(worker, context) }
        }
    }

    CompositionLocalProvider(LocalPuntoRive provides state, content = content)
}

// ───────────────────────── Mostrar un .riv (o el fallback) ─────────────────────────

private sealed interface BytesState {
    data object Loading : BytesState
    data object Missing : BytesState
    class Loaded(val bytes: ByteArray) : BytesState
}

/**
 * Muestra el artboard [artboard] de assets/rive/[file] con los colores del tema de la app.
 * Si no hay Rive, no existe el archivo o algo falla, muestra [fallback].
 *
 * @param active true cuando la animación debe verse/arrancar (ej. la página actual del tutorial).
 * @param enumProps propiedades enum del View Model a escribir (ej. mood = happy).
 * @param onFinished se llama cuando el .riv dispara el trigger "finished".
 * @param reserveSpace mientras carga, reservar el espacio de [modifier] para que no salte el diseño.
 */
@Composable
fun RiveOrFallback(
    file: String,
    artboard: String?,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    fit: Fit = Fit.Contain(),
    enumProps: Map<String, String> = emptyMap(),
    onFinished: (() -> Unit)? = null,
    reserveSpace: Boolean = true,
    fallback: @Composable () -> Unit,
) {
    when (val rive = LocalPuntoRive.current) {
        PuntoRiveState.Checking -> if (reserveSpace) Box(modifier)
        PuntoRiveState.Unavailable -> fallback()
        is PuntoRiveState.Ready -> {
            val bytes by produceState<BytesState>(BytesState.Loading, rive, file) {
                value = rive.bytes(file)?.let { BytesState.Loaded(it) } ?: BytesState.Missing
            }
            when (val b = bytes) {
                BytesState.Loading -> if (reserveSpace) Box(modifier)
                BytesState.Missing -> fallback()
                is BytesState.Loaded -> RiveScene(
                    rive = rive,
                    bytes = b.bytes,
                    artboardName = artboard,
                    modifier = modifier,
                    active = active,
                    fit = fit,
                    enumProps = enumProps,
                    onFinished = onFinished,
                    reserveSpace = reserveSpace,
                    fallback = fallback,
                )
            }
        }
    }
}

@Composable
private fun RiveScene(
    rive: PuntoRiveState.Ready,
    bytes: ByteArray,
    artboardName: String?,
    modifier: Modifier,
    active: Boolean,
    fit: Fit,
    enumProps: Map<String, String>,
    onFinished: (() -> Unit)?,
    reserveSpace: Boolean,
    fallback: @Composable () -> Unit,
) {
    val fileResult = rememberRiveFile(RiveFileSource.Bytes(bytes), rive.worker)
    // Si el nombre del artboard no existe en el .riv, esto da Error y se usa el fallback.
    val artboardResult = fileResult.andThen { file -> rememberArtboardResult(file, artboardName) }

    when (val resources = fileResult.zip(artboardResult)) {
        is Result.Loading -> if (reserveSpace) Box(modifier)
        is Result.Error -> fallback()
        is Result.Success -> {
            val (file, artboard) = resources.value
            val source = remember(artboard) { ViewModelSource.DefaultForArtboard(artboard).defaultInstance() }
            when (val vmiResult = rememberViewModelInstanceResult(file, source)) {
                is Result.Loading -> if (reserveSpace) Box(modifier)
                // El .riv no tiene View Model: se muestra igual, solo que sin recibir los colores del tema.
                is Result.Error -> RiveCanvas(file, artboard, null, modifier, active, fit, enumProps, onFinished)
                is Result.Success -> RiveCanvas(file, artboard, vmiResult.value, modifier, active, fit, enumProps, onFinished)
            }
        }
    }
}

@Composable
private fun RiveCanvas(
    file: RiveFile,
    artboard: Artboard,
    vmi: ViewModelInstance?,
    modifier: Modifier,
    active: Boolean,
    fit: Fit,
    enumProps: Map<String, String>,
    onFinished: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f

    if (vmi != null) {
        // Colores del tema de la app → Rive. Así el verde/azul/violeta... y el modo claro/oscuro SIEMPRE coinciden con la app
        // (y cambian en vivo si la persona cambia el tema o el color de acento).
        LaunchedEffect(
            vmi, isDark, colors.primary, colors.onPrimary, colors.primaryContainer,
            colors.background, colors.surface, colors.onSurface,
        ) {
            runCatching {
                vmi.setBoolean(PuntoRiveProps.IS_DARK, isDark)
                vmi.setColor(PuntoRiveProps.PRIMARY, colors.primary.toArgb())
                vmi.setColor(PuntoRiveProps.ON_PRIMARY, colors.onPrimary.toArgb())
                vmi.setColor(PuntoRiveProps.PRIMARY_CONTAINER, colors.primaryContainer.toArgb())
                vmi.setColor(PuntoRiveProps.BACKGROUND, colors.background.toArgb())
                vmi.setColor(PuntoRiveProps.SURFACE, colors.surface.toArgb())
                vmi.setColor(PuntoRiveProps.ON_SURFACE, colors.onSurface.toArgb())
            }
        }
        LaunchedEffect(vmi, active) { runCatching { vmi.setBoolean(PuntoRiveProps.ACTIVE, active) } }
        LaunchedEffect(vmi, enumProps) {
            runCatching { enumProps.forEach { (name, value) -> vmi.setEnum(name, value) } }
        }
        if (onFinished != null) {
            val latest by rememberUpdatedState(onFinished)
            LaunchedEffect(vmi) { vmi.getTriggerFlow(PuntoRiveProps.FINISHED).collect { latest() } }
        }
    }

    Rive(
        file = file,
        modifier = modifier,
        artboard = artboard,
        viewModelInstance = vmi,
        fit = fit,
        // Observe: Rive mira los toques pero no los consume, así el pager del tutorial sigue deslizando y el toque para saltar la intro funciona.
        pointerInputMode = RivePointerInputMode.Observe,
    )
}
