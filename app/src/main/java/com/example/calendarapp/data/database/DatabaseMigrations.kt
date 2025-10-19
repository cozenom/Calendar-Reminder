package com.example.calendarapp.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for AppDatabase.
 *
 * Each migration handles schema changes between versions while preserving user data.
 * Test all migrations thoroughly before releasing to production.
 */
object DatabaseMigrations {

    /**
     * Migration from version 5 to 6.
     *
     * This is a placeholder migration that doesn't change the schema.
     * It's included to establish the migration infrastructure and verify it works.
     *
     * Changes: None - schema remains identical
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema changes in this migration
            // This migration exists to establish the migration infrastructure
            // and replace fallbackToDestructiveMigration()

            // Future migrations will include actual SQL ALTER TABLE statements here
        }
    }

    /**
     * Migration from version 6 to 7.
     *
     * Adds inventory tracking functionality.
     *
     * Changes:
     * - Add inventory fields to medication_reminders table
     * - Create prescription_refills table for refill history
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add inventory tracking columns to medication_reminders
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN dosagePerIntake INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN currentInventory INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN inventoryTrackingEnabled INTEGER NOT NULL DEFAULT 0")

            // Create prescription_refills table
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

            // Create index for foreign key
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prescription_refills_reminderId ON prescription_refills(reminderId)")
        }
    }

    /**
     * Migration from version 8 to 9.
     *
     * Adds customizable refill period to medication reminders.
     *
     * Changes:
     * - Add refillPeriodDays column to medication_reminders table
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add refill period column with default of 30 days
            db.execSQL("ALTER TABLE medication_reminders ADD COLUMN refillPeriodDays INTEGER NOT NULL DEFAULT 30")
        }
    }
}
