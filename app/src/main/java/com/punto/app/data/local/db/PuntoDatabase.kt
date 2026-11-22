package com.punto.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.punto.app.data.local.db.dao.LocationDao
import com.punto.app.data.local.db.dao.ReminderDao
import com.punto.app.data.local.db.entity.LocationEntity
import com.punto.app.data.local.db.entity.ReminderEntity

@Database(
    entities = [LocationEntity::class, ReminderEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class PuntoDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val NAME = "punto.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE locations ADD COLUMN createdAtEpochMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN notificationCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN lastNotifiedAtEpochMillis INTEGER")
                db.execSQL("ALTER TABLE reminders ADD COLUMN snoozeUntilEpochMillis INTEGER")
            }
        }
    }
}
