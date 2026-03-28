package com.example.calendarapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "reminder_logs",
    foreignKeys = [ForeignKey(
        entity = Reminder::class,
        parentColumns = ["id"],
        childColumns = ["reminderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("reminderId")]
)
data class ReminderLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val title: String,
    val logDateTime: LocalDateTime,
    val completed: Boolean = false
)
