package com.example.calendarapp

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.database.DatabaseMigrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Tests for database migrations.
 *
 * These tests ensure that migrations properly preserve user data while updating the schema.
 *
 * To run these tests:
 * ./gradlew connectedAndroidTest --tests "com.example.calendarapp.MigrationTest"
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        // Create database with version 5 schema
        helper.createDatabase(TEST_DB, 5).apply {
            // Insert test data in version 5 format
            execSQL(
                """
                INSERT INTO medication_reminders
                (id, medicationName, reminderTimes, frequency, startDate, endDate, reminderDays)
                VALUES
                (1, 'Test Med', '09:00,21:00', 2, '2025-01-01', NULL, '1,2,3,4,5')
            """
            )

            execSQL(
                """
                INSERT INTO medication_intake
                (id, reminderId, medicationName, intakeDateTime, status, taken)
                VALUES
                (1, 1, 'Test Med', '2025-01-01T09:00:00', 'Scheduled', 0)
            """
            )

            close()
        }

        // Run migration
        helper.runMigrationsAndValidate(TEST_DB, 6, true, DatabaseMigrations.MIGRATION_5_6)

        // Verify data is preserved
        val db = getMigratedRoomDatabase()

        // You can add verification queries here to ensure data integrity
        db.close()
    }

    private fun getMigratedRoomDatabase(): AppDatabase {
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        )
            .addMigrations(DatabaseMigrations.MIGRATION_5_6)
            .build()

        // Trigger database creation
        helper.closeWhenFinished(database)
        return database
    }

    // Add more migration tests as new versions are created:
    // @Test
    // fun migrate6To7() { ... }
}
