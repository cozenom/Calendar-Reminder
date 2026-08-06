package com.davidp.simpleweeklyreminders.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

        // No Migrations by design, pre-launch: the app is unpublished, so no device
        // is on an older schema and every install builds v7 straight from the
        // entities. A schema change wipes local data instead — see todo.txt
        // PRE-LAUNCH before publishing, where this reverses:
        //   - the launch schema becomes a permanent floor (no retroactive path
        //     from a version already on someone's device)
        //   - so fallbackToDestructiveMigration comes off, and every change from
        //     then on needs a real Migration + a migration test
        // exportSchema stays on meanwhile: app/schemas/*.json is the prerequisite
        // for writing those migrations later, and can't be reconstructed after the fact.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
