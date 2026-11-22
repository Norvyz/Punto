# Documento de Arquitectura Técnica - Punto (Fase 1)

## 1. Objetivo

Establecer la fundación técnica de Punto: proyecto Android nativo, arquitectura limpia, dependencias base, navegación, persistencia local, DI y manejo de errores. Sin implementar lógica de geofencing ni CRUD funcional completo.

## 2. Decisiones técnicas

| Decisión | Elección | Motivo |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Nativo moderno, coherente con requisitos y animaciones futuras. |
| Arquitectura | Clean Architecture pragmática por paquetes | Separa responsabilidades y permite escalar a features/módulos. |
| DI | Hilt | Integración oficial con Android, ViewModel, WorkManager. |
| DB | Room + KSP | Tipado, migraciones, Flow, buen fit con Kotlin. |
| Preferencias | DataStore Preferences | Reemplazo moderno de SharedPreferences, async/Flow. |
| Navegación | Navigation Compose | Integración natural con Compose y bottom bar. |
| Ubicación/Mapas | Play Services Location + Maps Compose | Requisito del proyecto; configurado, no usado en Fase 1. |
| Background | WorkManager | Base para tareas diferibles fiables en fases posteriores. |
| Errores | `AppResult` + `AppError` | Contrato explícito entre data/domain/presentation. |
| minSdk | 26 | Balance entre cobertura y APIs modernas para notificaciones/geofencing. |
| compile/target | 36 | Alineado con SDK instalado y prácticas actuales. |

## 3. Estructura de carpetas

```text
Punto/
  app/
    build.gradle.kts
    proguard-rules.pro
    src/main/
      AndroidManifest.xml
      java/com/punto/app/
        PuntoApplication.kt
        MainActivity.kt
        core/
          common/AppResult.kt
          error/AppError.kt
          ui/
          util/
        data/
          local/
            db/
              PuntoDatabase.kt
              dao/LocationDao.kt, ReminderDao.kt
              entity/LocationEntity.kt, ReminderEntity.kt
            datastore/UserPreferencesDataSource.kt
          mapper/LocationMapper.kt, ReminderMapper.kt
          repository/RoomLocationRepository.kt, RoomReminderRepository.kt, DataStoreUserPreferencesRepository.kt
        di/DatabaseModule.kt, RepositoryModule.kt
        domain/
          model/Location.kt, Reminder.kt, PlaceCategory.kt, ReminderStatus.kt, TriggerType.kt, ThemePreference.kt
          repository/LocationRepository.kt, ReminderRepository.kt, UserPreferencesRepository.kt
          usecase/
        presentation/
          navigation/PuntoApp.kt, PuntoNavHost.kt, PuntoDestination.kt
          theme/Color.kt, Type.kt, Theme.kt
          core/PlaceholderScreen.kt
          home/HomeScreen.kt
          map/MapScreen.kt
          reminders/RemindersScreen.kt
          history/HistoryScreen.kt
          settings/SettingsScreen.kt
      res/
        values/strings.xml, themes.xml
        values-night/themes.xml
        drawable/ic_launcher_*.xml
        mipmap-anydpi-v26/ic_launcher*.xml
        xml/backup_rules.xml, data_extraction_rules.xml
  docs/arquitectura-fase-1.md
  gradle/libs.versions.toml
  build.gradle.kts
  settings.gradle.kts
```

## 4. Capas y responsabilidades

- **Presentation**: Compose UI, navegación, ViewModels (futuro). Solo conoce Domain.
- **Domain**: modelos, enums, contratos de repositorio, casos de uso. Sin dependencias Android/Room.
- **Data**: Room/DataStore, mappers, implementaciones de repositorio. Mapea excepciones a `AppError`.
- **Core**: utilidades y tipos transversales (`AppResult`, `AppError`).
- **DI**: provee DB, DAOs y bindings de repositorios.

## 5. Persistencia preparada

- **Room**: `locations`, `reminders` con FK `reminders.locationId -> locations.id`, `CASCADE`.
- **Índices**: `reminders.locationId`, `reminders.status`.
- **Schema export**: habilitado vía plugin Room (`app/schemas`).
- **DataStore**: tema y defaults (radio, dwell, repetición) como base de configuración.

## 6. Navegación

Bottom navigation con 5 destinos: Inicio, Mapa, Recordatorios, Historial, Configuración. Estado preservado con `saveState/restoreState` y `launchSingleTop`.

## 7. Manejo de errores

- `AppResult<T>`: `Success | Error(AppError) | Loading`.
- `AppError`: `Database | Storage | Location | Permission | Unknown`.
- `runAppCatching` envuelve operaciones suspend y mapea excepciones.

## 8. Seguridad y privacidad (base)

- Sin backend; datos locales.
- Permisos de ubicación/notificación declarados para fases futuras; no se solicitan en Fase 1.
- `MAPS_API_KEY` por placeholder, no hardcodeada.
- Backup rules presentes; se endurecerán al persistir datos sensibles.

## 9. Deuda / siguientes fases

- Implementar CRUD real de ubicaciones y recordatorios (Fase 2+).
- Solicitud de permisos con rationale.
- Geofencing/WorkManager/NotificationManager.
- Pruebas unitarias de mappers/repositorios y UI tests de navegación.
- Icono final de marca (actual: placeholder adaptativo).
- Evaluar modularización por feature cuando crezca el código.