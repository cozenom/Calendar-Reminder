package com.example.calendarapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.calendarapp.data.dao.MedicationReminderDao
import com.example.calendarapp.data.model.MedicationReminder

@Database(entities = [MedicationReminder::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationReminderDao(): MedicationReminderDao

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
                    .fallbackToDestructiveMigration()  // This will delete the old database and create a new one
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}