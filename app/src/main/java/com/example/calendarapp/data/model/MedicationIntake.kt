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
    val intakeDateTime: LocalDateTime,
    val taken: Boolean = false
)