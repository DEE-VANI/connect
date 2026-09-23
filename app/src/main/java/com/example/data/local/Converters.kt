package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.local.entities.EventCategory
import com.example.data.local.entities.EventStatus
import com.example.data.local.entities.RegistrationStatus
import com.example.data.local.entities.TicketStatus
import com.example.data.local.entities.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.STUDENT)

    @TypeConverter
    fun fromEventCategory(value: EventCategory): String = value.name

    @TypeConverter
    fun toEventCategory(value: String): EventCategory = runCatching { EventCategory.valueOf(value) }.getOrDefault(EventCategory.TECH)

    @TypeConverter
    fun fromEventStatus(value: EventStatus): String = value.name

    @TypeConverter
    fun toEventStatus(value: String): EventStatus = runCatching { EventStatus.valueOf(value) }.getOrDefault(EventStatus.ACTIVE)

    @TypeConverter
    fun fromRegistrationStatus(value: RegistrationStatus): String = value.name

    @TypeConverter
    fun toRegistrationStatus(value: String): RegistrationStatus = runCatching { RegistrationStatus.valueOf(value) }.getOrDefault(RegistrationStatus.CONFIRMED)

    @TypeConverter
    fun fromTicketStatus(value: TicketStatus): String = value.name

    @TypeConverter
    fun toTicketStatus(value: String): TicketStatus = runCatching { TicketStatus.valueOf(value) }.getOrDefault(TicketStatus.ACTIVE)
}
