package com.example.calendarapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "medication_intake",
    foreignKeys = [ForeignKey(
        entity = MedicationReminder::class,
        parentColumns = ["id"],
        childColumns = ["reminderId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MedicationIntake(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val intakeDateTime: LocalDateTime,
    val status: String,
    val actualIntakeDateTime: LocalDateTime? = null,
    val notes: String? = null,
    val location: String? = null,
    val taken: Boolean = false
)