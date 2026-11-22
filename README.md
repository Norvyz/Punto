# Punto

Aplicación Android nativa de recordatorios inteligentes basados en ubicación.

## Estado actual

**Fase 1 completada (fundación técnica)**.

La app compila y abre con navegación base (5 secciones), tema Material 3, y cimientos de arquitectura para fases posteriores.

## Stack técnico (Fase 1)

| Área | Tecnología | Versión |
|---|---:|---|
| Lenguaje | Kotlin | 2.1.10 |
| Build | Android Gradle Plugin | 8.13.0 |
| Gradle | Gradle Wrapper | 9.6.0 |
| UI | Jetpack Compose + Material 3 (BOM) | 2025.05.00 |
| Navegación | Navigation Compose | 2.9.0 |
| DI | Hilt | 2.56.2 |
| DB local | Room | 2.7.1 |
| Preferencias | DataStore Preferences | 1.1.7 |
| Background | WorkManager | 2.10.1 |
| Ubicación | Play Services Location | 21.3.0 |
| Mapas | Maps Compose / Play Services Maps | 6.6.0 / 19.2.0 |
| Async | Coroutines | 1.10.2 |
| SDK | compile/target/min | 36 / 36 / 26 |

> Nota: versiones seleccionadas por compatibilidad estable en el ecosistema actual; se validan por compilación. `MAPS_API_KEY` se inyecta por placeholder de manifest (`local.properties` o variable de entorno).

## Arquitectura

Estructura por capas y responsabilidades:

```text
app/src/main/java/com/punto/app/
  core/
    common/        # AppResult, utilidades transversales
    error/         # AppError, runAppCatching
    ui/            # Componentes UI compartidos (futuro)
    util/          # Utilidades (futuro)
  data/
    local/
      db/          # Room Database, DAO, Entity
      datastore/   # DataStore
    mapper/        # Entity <-> Domain
    repository/    # Implementaciones Room/DataStore
  di/              # Módulos Hilt
  domain/
    model/         # Modelos de dominio
    repository/    # Contratos
    usecase/       # Casos de uso (futuro)
  presentation/
    navigation/    # Destinos, NavHost, App scaffold
    theme/         # Material 3 color/type/theme
    core/          # PlaceholderScreen y UI base
    home/map/reminders/history/settings/
```

## Convenciones

- **Capas**: Presentation -> Domain <- Data. `domain` no depende de Android framework.
- **Errores**: operaciones de datos retornan `AppResult<T>`; errores tipados en `AppError`.
- **DI**: Hilt con `@HiltAndroidApp`, módulos en `di/`, bindings por interfaz.
- **Persistencia**: Room para entidades; DataStore para preferencias. Mappers explícitos.
- **UI**: Compose + Material 3, navegación con estado preservado en bottom bar.
- **Privacidad**: datos locales, sin backend; permisos de ubicación declarados para fases futuras pero no usados aún.

## Build

Requiere Android SDK (Platform 36, Build Tools) y JDK 17+.

```powershell
./gradlew :app:assembleDebug
```

## Fase 1 - Fuera de alcance

- Motor de geofencing / GPS.
- CRUD completo de ubicaciones y recordatorios.
- Notificaciones inteligentes.
- Integración real de Google Maps en UI (SDK configurado, pantalla placeholder).
