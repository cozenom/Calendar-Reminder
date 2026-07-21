package com.davidp.simpleweeklyreminders.data.repository

import com.davidp.simpleweeklyreminders.data.dao.ReminderDao
import com.davidp.simpleweeklyreminders.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** In-memory stand-in for Room's generated ReminderDao, for JVM repository tests. */
class FakeReminderDao : ReminderDao {
    private val reminders = MutableStateFlow<List<Reminder>>(emptyList())
    private var nextId = 1

    override fun getAllReminders(): Flow<List<Reminder>> =
        reminders.map { it.sortedWith(compareBy({ r -> r.sortOrder }, { r -> r.createdAt })) }

    override suspend fun insertReminder(reminder: Reminder): Long {
        val id = if (reminder.id != 0) reminder.id else nextId
        if (id >= nextId) nextId = id + 1
        reminders.value = reminders.value.filterNot { it.id == id } + reminder.copy(id = id)
        return id.toLong()
    }

    override suspend fun updateReminder(reminder: Reminder) {
        reminders.value = reminders.value.map { if (it.id == reminder.id) reminder else it }
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        reminders.value = reminders.value.filterNot { it.id == reminder.id }
    }

    override fun getActiveReminders(date: LocalDate): Flow<List<Reminder>> =
        reminders.map { list ->
            list.filter { it.startDate <= date && (it.endDate == null || it.endDate >= date) }
        }

    override suspend fun getReminderByIdOnce(id: Int): Reminder? =
        reminders.value.find { it.id == id }

    override suspend fun getAllRemindersList(): List<Reminder> = reminders.value

    override suspend fun updateSortOrder(id: Int, sortOrder: Int) {
        reminders.value = reminders.value.map { if (it.id == id) it.copy(sortOrder = sortOrder) else it }
    }
}
