package com.example.calendarapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.calendarapp.data.dao.ReminderDao
import com.example.calendarapp.data.dao.ReminderLogDao
import com.example.calendarapp.data.model.Reminder
import com.example.calendarapp.data.model.ReminderLog

@Database(
    entities = [Reminder::class, ReminderLog::class],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderLogDao(): ReminderLogDao

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
                    .addMigrations(
                        DatabaseMigrations.MIGRATION_5_6,
                        DatabaseMigrations.MIGRATION_6_7,
                        DatabaseMigrations.MIGRATION_8_9,
                        DatabaseMigrations.MIGRATION_9_10,
                        DatabaseMigrations.MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
