package com.example.calendarapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.calendarapp.data.dao.MedicationReminderDao
import com.example.calendarapp.data.dao.MedicationIntakeDao
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.model.MedicationIntake

// TODO : Database migrations

@Database(entities = [MedicationReminder::class, MedicationIntake::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun medicationIntakeDao(): MedicationIntakeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration() // This line enables destructive migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}