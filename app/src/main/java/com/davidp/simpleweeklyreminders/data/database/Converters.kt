package com.davidp.simpleweeklyreminders.data.database

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, dateTimeFormatter) }
    }

    @TypeConverter
    fun toLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun toLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    // New converters for Set<Int>
    @TypeConverter
    fun fromSetInt(value: Set<Int>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toSetInt(value: String?): Set<Int> {
        return value?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    @TypeConverter
    fun fromListLocalTime(value: List<LocalTime>?): String? {
        return value?.joinToString(",") { it.format(timeFormatter) }
    }

    @TypeConverter
    fun toListLocalTime(value: String?): List<LocalTime> {
        return value?.split(",")?.mapNotNull { LocalTime.parse(it, timeFormatter) } ?: emptyList()
    }
}