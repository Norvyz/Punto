# Cambios: rendimiento del teclado, splash/intro animada y tutorial animado

> Nota: estos cambios se escribieron sin poder compilar (sin Android SDK en el entorno). Los archivos pasaron
> una revisión de sintaxis, pero hay que compilar y probar en un dispositivo.

## 1. Teclado lento
Causa: el texto viajaba ViewModel → StateFlow → `collectAsState` en cada tecla (salto asíncrono + recomposición de toda la pantalla).

- `presentation/components/PuntoTextFields.kt` (NUEVO): `StateOutlinedTextField`, campo con estado local; solo se recompone él al teclear.
- `createreminder/CreateReminderScreen.kt`: nombre, asunto y descripción en estado local (`rememberSaveable`). Se copian al
  ViewModel solo al guardar: `viewModel.save(name, subject, description)`. Al editar, se siembran una sola vez desde la base de datos.
  `ReminderSummary` ahora recibe valores simples (antes recibía todo el estado y se recomponía con cada letra).
- `createreminder/CreateReminderViewModel.kt` y `CreateReminderUiState.kt`: se quitan `onName/onSubject/onDescription` y `canSave`;
  se agrega `initialLoaded`.
- `map/MapScreen.kt` y `MapViewModel.kt`: la hoja "Guardar lugar" usa estado local para nombre y dirección; `saveDraft(name, address)`.
- `reminders/RemindersScreen.kt`: el buscador es instantáneo y el filtro se aplica con debounce de 250 ms; la lista filtrada va en `remember`.
- `app/build.gradle.kts`: release con `isMinifyEnabled = true` y `isShrinkResources = true`, y se agrega `androidx.profileinstaller`.
  IMPORTANTE: mide la fluidez en un build **release**; Compose en debug es mucho más lento.
  Si algo falla solo en release, pon esas dos banderas en `false` y revisa reglas keep en `proguard-rules.pro`.

## 2. Splash del sistema + animación de apertura
- Dependencia nueva: `androidx.core:core-splashscreen` (`gradle/libs.versions.toml` y `app/build.gradle.kts`).
- `res/values/themes.xml` y `values-night/themes.xml`: tema `Theme.Punto.Starting`. `AndroidManifest.xml`: `MainActivity` lo usa.
- `res/values/colors.xml` y `values-night/colors.xml` (NUEVOS): solo `punto_splash_background` (mismo fondo que el tema claro/oscuro). `res/drawable/ic_splash_empty.xml`: ícono transparente (el splash del sistema es SOLO color de fondo).
- `MainActivity.kt`: `installSplashScreen()`; el splash se queda hasta leer las preferencias (con tope de 2 s por seguridad).
- `presentation/intro/PuntoIntro.kt`: `PuntoIntroOverlay`, intro en CADA apertura (también la primera, antes del tutorial). Usa `MaterialTheme`
  (claro/oscuro + acento), por eso ya no desentona el verde. Corrige el bug anterior (usaba el modo del sistema y dibujaba un 2º pin sobre el del splash).
  - Un toque la salta; se omite si las animaciones del sistema están desactivadas; no se repite al rotar.

## 3. Tutorial animado
- `presentation/setup/TutorialAnimations.kt` (NUEVO): 3 ilustraciones en Compose puro (Canvas + Animatable, sin librerías):
  guardar un lugar (pin que cae y zona), crear un recordatorio (se escribe solo y elige "Al llegar"), y aviso al llegar
  (una persona entra a la zona y baja la notificación). Solo se animan mientras la página está activa.
- `setup/SetupScreen.kt`: ahora son 6 páginas (bienvenida, 3 pasos animados, apariencia, permisos). Se reemplazó "Así funciona".
- "Ver tutorial" en Ajustes: nueva ruta `PuntoRoutes.TUTORIAL`, cableada en `PuntoApp`, `PuntoNavHost` y `SettingsScreen`.
- Textos nuevos en `strings.xml` (prefijo `tutorial_`, `settings_help`, `settings_replay_tutorial`).

## 4. Rive (opcional en runtime)
- Dependencia `app.rive:rive-android:11.12.1` (API Compose, beta) + regla keep en `proguard-rules.pro`.
- `presentation/rive/PuntoRive.kt`: `ProvidePuntoRive` y `RiveOrFallback`. Sin .riv en `assets/rive/` → animaciones en Compose; con .riv → Rive, recibiendo los colores del tema por data binding.
- Usado en: intro (`Intro`), tutorial (`Step1..3`), bienvenida y pantallas vacías (`Mascot`).
- Mascota "Pinto": `docs/rive/mascota-pinto.svg` (+ preview). Pasos y contrato en `docs/rive/RIVE_GUIA.md`. Los .riv hay que crearlos en el editor de Rive.

## Qué validar
1. Sincronizar Gradle y compilar (versiones nuevas: core-splashscreen 1.0.1, profileinstaller 1.4.1).
2. Crear y editar un recordatorio: escribir rápido, rotar la pantalla a mitad, guardar. Al editar, los textos deben cargar bien.
3. Lugares (hoja de guardar) y buscador de recordatorios.
4. Primera apertura: tutorial de 6 páginas. Toda apertura: splash (solo fondo) → intro animada → tutorial/app; revisa claro y oscuro. Ajustes → "Ver tutorial".
5. Modo claro y oscuro; Android 12+ y uno anterior (el splash cambia un poco).
6. APK release (minify activado).
