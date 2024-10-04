package com.example.calendarapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calendarapp.data.dao.MedicationReminderDao
import com.example.calendarapp.data.dao.MedicationIntakeDao
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.model.MedicationIntake

@Database(entities = [MedicationReminder::class, MedicationIntake::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun medicationIntakeDao(): MedicationIntakeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the medication_intake table if it doesn't exist
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS medication_intake (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reminderId INTEGER NOT NULL,
                        intakeDateTime TEXT NOT NULL,
                        status TEXT NOT NULL,
                        actualIntakeDateTime TEXT,
                        notes TEXT,
                        location TEXT,
                        FOREIGN KEY(reminderId) REFERENCES medication_reminders(id) ON DELETE CASCADE
                    )
                """)

                // Add the 'taken' column to the medication_intake table
                database.execSQL("ALTER TABLE medication_intake ADD COLUMN taken INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration() // Add this line for testing purposes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}