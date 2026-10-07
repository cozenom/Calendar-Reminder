package com.davidp.simpleweeklyreminders.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.davidp.simpleweeklyreminders.data.dao.ReminderDao
import com.davidp.simpleweeklyreminders.data.dao.ReminderLogDao
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog

/** v9: per-reminder notification tone. Nullable, so existing rows keep their level's tone. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN sound TEXT")
    }
}

/**
 * v10: EVERY_N_DAYS -> INTERVAL with a unit. Old rows become "every N days" (the DEFAULTs).
 * The type rename must run: an unknown enum name crashes ReminderType.valueOf on read.
 * Also per-reminder snooze length: nullable, so existing rows keep the global length.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE reminders SET reminderType = 'INTERVAL' WHERE reminderType = 'EVERY_N_DAYS'")
        db.execSQL("ALTER TABLE reminders ADD COLUMN intervalUnit TEXT NOT NULL DEFAULT 'DAYS'")
        db.execSQL("ALTER TABLE reminders ADD COLUMN monthlyMode TEXT NOT NULL DEFAULT 'DAY_OF_MONTH'")
        db.execSQL("ALTER TABLE reminders ADD COLUMN snoozeMinutes INTEGER")
    }
}

@Database(
    entities = [Reminder::class, ReminderLog::class],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderLogDao(): ReminderLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v8 is the launch schema and a permanent floor: devices already on it can't
        // be retroactively migrated, so every schema change from here on needs a real
        // Migration (addMigrations) + a migration test. No destructive fallback on
        // purpose -- a missing migration must crash in testing, not wipe user data.
        // app/schemas/*.json (exportSchema) is what those migrations are written from.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).addMigrations(MIGRATION_8_9, MIGRATION_9_10).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
