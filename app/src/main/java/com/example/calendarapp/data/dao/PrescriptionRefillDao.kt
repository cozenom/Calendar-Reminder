package com.example.calendarapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.calendarapp.data.model.PrescriptionRefill
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PrescriptionRefillDao {
    @Query("SELECT * FROM prescription_refills WHERE reminderId = :reminderId ORDER BY pickupDate DESC, id DESC")
    fun getRefillsForReminder(reminderId: Int): Flow<List<PrescriptionRefill>>

    @Query("SELECT * FROM prescription_refills WHERE reminderId = :reminderId ORDER BY pickupDate DESC, id DESC LIMIT 1")
    suspend fun getLatestRefill(reminderId: Int): PrescriptionRefill?

    @Query("SELECT * FROM prescription_refills WHERE reminderId = :reminderId ORDER BY pickupDate DESC, id DESC LIMIT 1")
    fun getLatestRefillFlow(reminderId: Int): Flow<PrescriptionRefill?>

    @Query("SELECT * FROM prescription_refills WHERE pickupDate BETWEEN :start AND :end ORDER BY pickupDate ASC")
    fun getRefillsForDateRange(start: LocalDate, end: LocalDate): Flow<List<PrescriptionRefill>>

    @Query("SELECT * FROM prescription_refills ORDER BY pickupDate ASC")
    fun getAllRefills(): Flow<List<PrescriptionRefill>>

    @Query("UPDATE prescription_refills SET totalRefillsAuthorized = :totalRefills, refillsRemaining = :refillsRemaining WHERE id = :refillId")
    suspend fun updateRefillCounts(refillId: Int, totalRefills: Int, refillsRemaining: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(refill: PrescriptionRefill): Long

    @Update
    suspend fun update(refill: PrescriptionRefill)

    @Delete
    suspend fun delete(refill: PrescriptionRefill)
}
