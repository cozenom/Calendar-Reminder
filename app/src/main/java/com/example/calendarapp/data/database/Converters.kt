package com.example.calendarapp.data.database


import androidx.room.TypeConverter
import com.example.calendarapp.data.model.RefillEvent
import com.example.calendarapp.data.model.RefillInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val gson = Gson()

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

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? {
        return value?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
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

    @TypeConverter
    fun fromRefillInfo(refillInfo: RefillInfo?): String? {
        return gson.toJson(refillInfo)
    }

    @TypeConverter
    fun toRefillInfo(refillInfoString: String?): RefillInfo? {
        if (refillInfoString == null) return null
        val type = object : TypeToken<RefillInfo>() {}.type
        return gson.fromJson(refillInfoString, type)
    }

    @TypeConverter
    fun fromRefillEventList(refillEvents: List<RefillEvent>?): String? {
        return gson.toJson(refillEvents)
    }

    @TypeConverter
    fun toRefillEventList(refillEventsString: String?): List<RefillEvent>? {
        if (refillEventsString == null) return null
        val type = object : TypeToken<List<RefillEvent>>() {}.type
        return gson.fromJson(refillEventsString, type)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        return value?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }
}