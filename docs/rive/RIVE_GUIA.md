# Guía Rive para Punto

La app ya está lista para Rive. **Sin archivos .riv funciona igual** (usa las animaciones en Compose).
Cuando agregues los .riv, se usan automáticamente. No hay que tocar código.

## 1. Dónde van los archivos
`app/src/main/assets/rive/` (crea la carpeta):

| Archivo | Artboards | Uso |
|---|---|---|
| `punto_intro.riv` | `Intro` | Logo/mascota en cada apertura. Dispara el trigger `finished` al terminar (si no, la app lo cierra a los 4 s: `RIVE_INTRO_TIMEOUT_MS` en `PuntoIntro.kt`). |
| `punto_tutorial.riv` | `Step1`, `Step2`, `Step3` | Ilustraciones de los 3 pasos (280×220 dp, proporción 14:11). Se reproducen en bucle mientras `active = true`. |
| `punto_mascot.riv` | `Mascot` | Bienvenida (200 dp) y pantallas vacías (120 dp). |

Si falta un archivo o el artboard no existe → se usa la animación en Compose.

## 2. Data binding (View Model)
En cada artboard crea un View Model por defecto con estas propiedades (todas opcionales; si no existen se ignoran):

| Propiedad | Tipo | Dirección | Qué hace |
|---|---|---|---|
| `isDark` | Boolean | app → Rive | modo oscuro |
| `primary`, `onPrimary`, `primaryContainer`, `background`, `surface`, `onSurface` | Color | app → Rive | colores del tema actual (incluye el color de acento elegido) |
| `active` | Boolean | app → Rive | la página/pantalla está visible: arranca la animación |
| `mood` | Enum `idle, happy, sleepy, alert` | app → Rive | ánimo de la mascota |
| `finished` | Trigger | Rive → app | la animación terminó |

**Clave para el problema del verde:** en Rive, enlaza el relleno de las formas a `primary` (cuerpo), `primaryContainer` (cara), `onSurface` (contornos/pupilas), etc. Así el color siempre coincide con la app en claro/oscuro y con cada acento. No pongas colores fijos.

## 3. Mascota "Pinto"
`mascota-pinto.svg` (vista previa: `mascota-pinto-preview.png`). Es un pin con cara, brazos y pies. Cada parte es un grupo con id:
`zone`, `shadow`, `feet`, `arm_l`, `arm_r`, `body` (`body_shape`, `body_shade`, `body_shine`), `face` (`eye_l`, `eye_r`, `eyelids`, `cheek_*`, `mouth`), `antenna`.

Pasos en el editor de Rive:
1. Importa el SVG (arrastrar al artboard). Los grupos quedan como capas con nombre.
2. Rellenos: `body_shape`→`primary`, `face_patch`→`primaryContainer`, `body_shade`/`feet`/`arms`→`primary` con opacidad/oscurecido, trazos→`onSurface`.
3. Anima (State Machine `Main`):
   - `idle`: respiración (escala del cuerpo ±3 %), parpadeo cada ~3 s (`eyelids`), antena que oscila.
   - `happy`: salto + las ondas de `zone` se expanden.
   - `sleepy`: ojos entrecerrados, cuerpo más bajo.
   - `alert`: salta, antena brilla, ondas rápidas.
   Cambia de estado según el enum `mood`.
4. Artboard `Intro`: pin cae con rebote → ondas de `zone` → aparece el nombre "Punto" → trigger `finished` (~1.6 s).
5. Artboards `Step1..3` (bucle ~4 s): 1 = pin cae en el mapa y se abre la zona; 2 = se escribe el recordatorio y se activa; 3 = el punto entra en la zona y suena la notificación.
6. Exporta como `.riv`.

## 4. Verificar
Ejecuta la app: con los .riv colocados la intro, el tutorial y la mascota deben verse en Rive; cambia tema/acento en el tutorial (página 5) y los colores deben cambiar en vivo. Si algo se ve como antes, revisa nombres de archivo/artboard.

> Nota: esto no se pudo compilar ni probar en el entorno donde se escribió (sin Android SDK); valida el build.
