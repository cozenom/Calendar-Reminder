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

@Database(
    entities = [Reminder::class, ReminderLog::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderLogDao(): ReminderLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Published apps must migrate, never fall back destructively — the fallback
        // below wipes user data on any version bump without a matching Migration
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminder_logs ADD COLUMN snoozedUntil TEXT")
            }
        }

        // reminderType replaces dayInterval-nullness as the recurrence-mode discriminant
        // (dayInterval remains the interval payload, only meaningful when EVERY_N_DAYS).
        // frequency is dropped: it only ever stored distinctTimes.size and was never read back.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN reminderType TEXT NOT NULL DEFAULT 'SPECIFIC_DAYS'")
                db.execSQL("UPDATE reminders SET reminderType = 'EVERY_N_DAYS' WHERE dayInterval IS NOT NULL")
                db.execSQL("ALTER TABLE reminders DROP COLUMN frequency")
            }
        }

        // archivedAt: precise archive timestamp for manual archive() — lets the archive
        // badge/notice tell same-day archives apart from ones already seen (endDate alone
        // is day-only). Null for pre-existing rows and for auto-lapsed reminders, which
        // fall back to endDate-derived, day-granularity timing (see Reminder.archivedSince()).
        //
        // importance: schema only for now (2026-07-21) — Low/Medium/High, stored as TEXT
        // via Converters (matching reminderType), defaulting existing rows to HIGH so
        // nothing's behavior changes until the importance-driven notification work (2.2)
        // actually ships.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN archivedAt TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN importance TEXT NOT NULL DEFAULT 'HIGH'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
