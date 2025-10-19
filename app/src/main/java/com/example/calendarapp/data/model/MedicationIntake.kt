package com.example.calendarapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "medication_intake",
    foreignKeys = [ForeignKey(
        entity = MedicationReminder::class,
        parentColumns = ["id"],
        childColumns = ["reminderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("reminderId")]
)
data class MedicationIntake(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val medicationName: String,
    val intakeDateTime: LocalDateTime,
    val taken: Boolean = false
)