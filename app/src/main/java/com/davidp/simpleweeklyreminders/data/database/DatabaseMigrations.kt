package com.davidp.simpleweeklyreminders.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema changes in this migration
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN dosagePerIntake INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN currentInventory INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN inventoryTrackingEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS prescription_refills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    reminderId INTEGER NOT NULL,
                    pickupDate TEXT NOT NULL,
                    pillsPerRefill INTEGER NOT NULL,
                    totalRefillsAuthorized INTEGER NOT NULL,
                    refillsRemaining INTEGER NOT NULL,
                    FOREIGN KEY(reminderId) REFERENCES medication_reminders(id) ON DELETE CASCADE
                )
            """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prescription_refills_reminderId ON prescription_refills(reminderId)")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN refillPeriodDays INTEGER NOT NULL DEFAULT 30")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN prescriptionPillsPerRefill INTEGER NOT NULL DEFAULT 60")
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN prescriptionTotalRefills INTEGER NOT NULL DEFAULT 5")
        }
    }

    /**
     * Migration from version 10 to 11.
     *
     * Generalises the app from medication-only to a general reminder app.
     *
     * Changes:
     * - Rename medication_reminders → reminders; medicationName → title; drop all prescription fields
     * - Add optional fields: notes, color, completedColor, icon
     * - Rename medication_intake → reminder_logs; medicationName → title; intakeDateTime → logDateTime; taken → completed
     * - Drop prescription_refills table
     */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create new reminders table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    reminderTimes TEXT NOT NULL,
                    frequency INTEGER NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT,
                    reminderDays TEXT NOT NULL,
                    notes TEXT,
                    color TEXT,
                    completedColor TEXT,
                    icon TEXT
                )
                """.trimIndent()
            )

            // Copy data from old table, mapping medicationName → title
            db.execSQL(
                """
                INSERT INTO reminders (id, title, reminderTimes, frequency, startDate, endDate, reminderDays)
                SELECT id, medicationName, reminderTimes, frequency, startDate, endDate, reminderDays
                FROM medication_reminders
                """.trimIndent()
            )

            db.execSQL("DROP TABLE IF EXISTS medication_reminders")

            // Create new reminder_logs table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS reminder_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    reminderId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    logDateTime TEXT NOT NULL,
                    completed INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(reminderId) REFERENCES reminders(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_reminder_logs_reminderId ON reminder_logs(reminderId)")

            // Copy data from old table, mapping medicationName → title, intakeDateTime → logDateTime, taken → completed
            db.execSQL(
                """
                INSERT INTO reminder_logs (id, reminderId, title, logDateTime, completed)
                SELECT id, reminderId, medicationName, intakeDateTime, taken
                FROM medication_intake
                """.trimIndent()
            )

            db.execSQL("DROP TABLE IF EXISTS medication_intake")
            db.execSQL("DROP TABLE IF EXISTS prescription_refills")
        }
    }
}
