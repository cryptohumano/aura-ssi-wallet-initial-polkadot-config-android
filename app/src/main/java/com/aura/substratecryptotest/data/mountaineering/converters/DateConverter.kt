package com.aura.substratecryptotest.data.mountaineering.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Convertidor para fechas en Room
 */
class DateConverter {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
