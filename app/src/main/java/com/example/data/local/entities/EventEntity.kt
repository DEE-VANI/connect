package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventCategory(val label: String, val iconName: String) {
    TECH("Tech & Innovation", "terminal"),
    SPORTS("Sports & Athletics", "fitness_center"),
    ARTS("Arts & Culture", "palette"),
    CAREER("Career & Networking", "work"),
    ACADEMIC("Academic & Research", "school"),
    SOCIAL("Social & Campus Life", "groups")
}

enum class EventStatus {
    ACTIVE,
    FEATURED,
    ARCHIVED,
    CANCELLED
}

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: EventCategory,
    val location: String,
    val dateTimeMillis: Long,
    val endDateTimeMillis: Long = 0L,
    val durationHours: Double = 2.0,
    val capacity: Int,
    val organizerId: Long,
    val organizerName: String,
    val organizerEmail: String,
    val organizerContact: String = "",
    val coordinators: String = "", // Student Coordinators with Name, Email & Phone
    val facultyAdvisor: String = "", // Faculty / Dept Advisor Name & Email
    val bannerTag: String = "default",
    val status: EventStatus = EventStatus.ACTIVE,
    val tags: String = "Campus,Event",
    val createdAt: Long = System.currentTimeMillis()
)
