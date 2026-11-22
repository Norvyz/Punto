# Fase 8 — Auditoría técnica y funcional (Punto)

Fecha: 2026-11-22  
Proyecto: `D:\Documento\Proyectos\Punto`  
Tipo: auditoría de integración/calidad sobre código existente. **No se declara el producto listo para publicación sin validación real en dispositivos.**

## 1. Estado de compilación y artefactos

| Verificación | Resultado | Evidencia |
|---|---|---|
| Compilación Kotlin debug | OK | `:app:compileDebugKotlin` previo exitoso |
| APK debug | OK | `:app:assembleDebug` BUILD SUCCESSFUL |
| AAB debug | OK | `:app:bundleDebug` BUILD SUCCESSFUL |
| Tests unitarios | Sin tests | `:app:testDebugUnitTest` NO-SOURCE |
| Lint | Falla por 1 error local / 43 warnings | `:app:lintDebug` |

Artefactos generados:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/bundle/debug/app-debug.aab`

> Nota: AAB debug para integración. Para publicación se requiere firma release, keystore, versionado y `bundleRelease`.

## 2. Pruebas ejecutadas en este entorno

Ejecutadas:
- Compilación/ensamblado Gradle con JDK 21.
- Lint Android (`:app:lintDebug`).
- Revisión estática dirigida de puntos críticos (notificaciones, permisos, Room, geofence).

No ejecutadas (requieren dispositivo/emulador físico con Play Services):
- Entrada/salida/permanencia real en geocercas, reentrada rápida, múltiples recordatorios por lugar.
- Notificación → repetición → posponer → realizado en dispositivo real.
- Recuperación tras reinicio/boot, app cerrada, actualización de paquete.
- Permisos runtime en Android 10/13+, denegado permanente, GPS off, revocación.
- Perfilado batería/memoria/fugas.

## 3. Hallazgos y estado

### Corregidos en pulido post-Fase 8
- **Room destructive migration eliminado**: se agregaron `MIGRATION_1_2` y `MIGRATION_2_3` y se quitó `fallbackToDestructiveMigration(true)`. Archivos: `data/local/db/PuntoDatabase.kt`, `di/DatabaseModule.kt`.
- **Canal de notificaciones Android 8+**: `ReminderNotificationHelper` ahora crea canal versionado por configuración (`sonido+vibración`), elimina canales viejos Punto y notifica usando el canal resuelto. El cambio de preferencias refresca canal desde `SettingsViewModel`.
- **`runBlocking` removido** del flujo de notificación: `showReminder`/`resolveChannelConfig` son suspend y leen prefs con `first()`.
- **Permisos runtime UX**: `PermissionsScreen` ahora solicita permisos reales (FG location, background con guía a Ajustes en Android 11+, POST_NOTIFICATIONS en 13+), refresca estado en `ON_RESUME`. `SettingsScreen` refresca estado de permisos en `ON_RESUME`.
- **Deduplicación básica**: `GeofenceEventProcessor.activate` y `ReminderNotificationScheduler.trigger` ignoran re-disparos dentro de ventanas cortas (60s/45s) para reducir duplicados por re-registro/eventos repetidos.
- **Borrado de ubicación**: confirmación en UI avisando que se eliminan recordatorios asociados por FK CASCADE.
- **Lint PropertyEscape gradle.properties**: corregido. `local.properties` sigue siendo local y lint lo reporta; no se versiona y depende del entorno.

### Pendientes / riesgos

1. **Validación real en dispositivo (CRÍTICO)**: sin esto no se puede cerrar Fase 5/6 funcionalmente.
2. **Lint error local**: `local.properties` PropertyEscape depende de formato local; compila, pero lint estricto falla. Se puede agregar `lint.baseline` o ajustar archivo local según tooling.
3. **43 warnings lint**: dependencias desactualizadas, strings no usados, typos, ObsoleteSdkInt. No bloquean build.
4. **Idempotencia fuerte**: la dedup actual es por ventana temporal en memoria/estado de recordatorio; para robustez total agregar tabla/log de eventos procesados.
5. **Permiso background Android 11+**: el sistema obliga a Ajustes/"Permitir siempre"; la app guía al usuario pero no puede concederlo directamente.
6. **Batería/OEM**: geofencing y WorkManager pueden retrasarse por Doze/fabricantes; requiere pruebas reales y guía al usuario.
7. **Tests automatizados**: no existen; recomendable para `GeofenceEventProcessor`, `ReminderNotificationScheduler`, DAOs y migraciones Room.
8. **Release**: falta firma release, keystore, `bundleRelease`, política de privacidad, y revisión de proguard/shrink.

## 4. Cobertura contra checklist solicitado

| Caso | Estado | Nota |
|---|---|---|
| Registro inicial de ubicaciones | Implementado | Validar en dispositivo |
| Crear/editar/eliminar recordatorios | Implementado | Room/UI |
| Entrada/salida/permanencia geocercas | Implementado | **Requiere prueba real** |
| Activación inmediata si ya dentro | Implementado | **Requiere prueba real** |
| Notificaciones/repetición/posponer/completar | Implementado | **Requiere prueba real** |
| Sonidos/configuración | Mejorado | Canales versionados; validar en Android 8+ real |
| Tema claro y oscuro | Implementado | DataStore + MainActivity |
| Recuperación tras reinicio | Implementado | **Requiere prueba real** |
| Permisos/GPS off/revocación | Mejorado | UX/runtime mejorada; validar casos extremos |
| App cerrada/dispositivo reiniciado | Implementado | **Requiere prueba real** |
| Ubicación eliminada con recordatorios | Implementado | Confirmación UI + CASCADE |
| Varios recordatorios mismo lugar | Implementado | **Requiere prueba real** |
| Entrada/salida rápida | Mejorado | Dedup básica; validar real |

## 5. Criterio de finalización de Fase 8

**No cumplido completamente** por falta de validación real en dispositivo y pruebas automatizadas/perfilado. El código compila y se aplicaron los pulidos técnicos solicitados; las pruebas físicas quedan pendientes para el usuario.