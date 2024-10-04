package com.example.calendarapp.data.model

import androidx.room.*
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
    val status: String,
    val actualIntakeDateTime: LocalDateTime? = null,
    val notes: String? = null,
    val location: String? = null,
    val taken: Boolean = false
)