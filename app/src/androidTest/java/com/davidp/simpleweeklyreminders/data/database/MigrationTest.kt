package com.davidp.simpleweeklyreminders.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds a real database from an older app/schemas/<n>.json, migrates it, and lets Room check
 * the result matches the next schema. A mismatch fails here instead of crashing users.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate8To9_keepsRowsAndAddsNullSound() {
        helper.createDatabase(DB_NAME, 8).use { db ->
            db.execSQL(
                """
                INSERT INTO reminders (id, title, reminderTimes, startDate, reminderDays,
                    reminderType, isActive, createdAt, sortOrder, importance)
                VALUES (1, 'Water plants', '09:00', '2026-01-01', '1,2,3', 'SPECIFIC_DAYS',
                    1, '2026-01-01T08:00', 0, 'HIGH')
                """.trimIndent()
            )
        }

        // Validates the migrated schema against 9.json (dropAllTables = true: no leftovers)
        helper.runMigrationsAndValidate(DB_NAME, 9, true, MIGRATION_8_9).use { db ->
            db.query("SELECT title, sound FROM reminders WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Water plants", cursor.getString(0))
                // Existing reminders keep their importance level's tone
                assertTrue(cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrate9To10_everyNDaysBecomesIntervalInDays() {
        helper.createDatabase(DB_NAME, 9).use { db ->
            db.execSQL(
                """
                INSERT INTO reminders (id, title, reminderTimes, startDate, reminderDays, dayInterval,
                    reminderType, isActive, createdAt, sortOrder, importance)
                VALUES (1, 'Water plants', '09:00', '2026-01-01', '1,2,3', 3, 'EVERY_N_DAYS',
                    1, '2026-01-01T08:00', 0, 'HIGH'),
                    (2, 'Gym', '07:00', '2026-01-01', '1,3', NULL, 'SPECIFIC_DAYS',
                    1, '2026-01-01T08:00', 1, 'HIGH')
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 10, true, MIGRATION_9_10).use { db ->
            db.query(
                "SELECT reminderType, dayInterval, intervalUnit, monthlyMode, snoozeMinutes FROM reminders ORDER BY id"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("INTERVAL", cursor.getString(0))
                assertEquals(3, cursor.getInt(1))
                assertEquals("DAYS", cursor.getString(2))
                assertEquals("DAY_OF_MONTH", cursor.getString(3))
                // Existing reminders keep the global snooze length
                assertTrue(cursor.isNull(4))
                // Other types are untouched
                assertTrue(cursor.moveToNext())
                assertEquals("SPECIFIC_DAYS", cursor.getString(0))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
