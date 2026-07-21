package com.davidp.simpleweeklyreminders.data.repository

import com.davidp.simpleweeklyreminders.data.dao.ReminderLogDao
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/** In-memory stand-in for Room's generated ReminderLogDao, for JVM repository tests. */
class FakeReminderLogDao : ReminderLogDao {
    private val logs = MutableStateFlow<List<ReminderLog>>(emptyList())
    private var nextId = 1

    override suspend fun insert(log: ReminderLog) {
        val id = if (log.id != 0) log.id else nextId
        if (id >= nextId) nextId = id + 1
        logs.value = logs.value + log.copy(id = id)
    }

    override fun getLogsForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<ReminderLog>> =
        logs.map { list -> list.filter { it.logDateTime >= start && it.logDateTime <= end } }

    override suspend fun updateCompletedStatus(logId: Int, completed: Boolean) {
        logs.value = logs.value.map { if (it.id == logId) it.copy(completed = completed) else it }
    }

    override suspend fun getLogById(logId: Int): ReminderLog? =
        logs.value.find { it.id == logId }

    override suspend fun getNextLogForReminder(reminderId: Int, after: LocalDateTime): ReminderLog? =
        logs.value.filter { it.reminderId == reminderId && it.logDateTime > after }
            .minByOrNull { it.logDateTime }

    override suspend fun getMissedLogsList(since: LocalDateTime, now: LocalDateTime): List<ReminderLog> =
        logs.value.filter { it.logDateTime > since && it.logDateTime < now && !it.completed }

    override suspend fun deleteFutureLogsForReminder(reminderId: Int, fromDateTime: LocalDateTime) {
        logs.value = logs.value.filterNot { it.reminderId == reminderId && it.logDateTime > fromDateTime }
    }

    override suspend fun deleteFutureIncompleteLogsForReminder(reminderId: Int, fromDateTime: LocalDateTime) {
        logs.value = logs.value.filterNot {
            it.reminderId == reminderId && it.logDateTime > fromDateTime && !it.completed
        }
    }

    override suspend fun getExistingLogDateTimesForReminder(reminderId: Int, from: LocalDateTime): List<LocalDateTime> =
        logs.value.filter { it.reminderId == reminderId && it.logDateTime >= from }.map { it.logDateTime }

    override suspend fun getFutureLogsForReminder(reminderId: Int, after: LocalDateTime): List<ReminderLog> =
        logs.value.filter { it.reminderId == reminderId && it.logDateTime > after }

    override suspend fun getLogsForReminderInRange(reminderId: Int, start: LocalDateTime, end: LocalDateTime): List<ReminderLog> =
        logs.value.filter { it.reminderId == reminderId && it.logDateTime >= start && it.logDateTime <= end }

    override suspend fun deleteLogsByIds(ids: List<Int>) {
        logs.value = logs.value.filterNot { it.id in ids }
    }

    override suspend fun updateTitleForReminder(reminderId: Int, title: String) {
        logs.value = logs.value.map { if (it.reminderId == reminderId) it.copy(title = title) else it }
    }

    override suspend fun updateSnoozedUntil(logId: Int, snoozedUntil: LocalDateTime?) {
        logs.value = logs.value.map { if (it.id == logId) it.copy(snoozedUntil = snoozedUntil) else it }
    }

    override suspend fun getSnoozedLogsList(): List<ReminderLog> =
        logs.value.filter { it.snoozedUntil != null && !it.completed }
}
