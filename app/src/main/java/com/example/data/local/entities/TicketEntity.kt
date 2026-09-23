package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TicketStatus {
    ACTIVE,
    CHECKED_IN,
    CANCELLED
}

@Entity(
    tableName = "tickets",
    indices = [
        Index(value = ["ticketToken"], unique = true),
        Index(value = ["eventId"]),
        Index(value = ["userId"]),
        Index(value = ["registrationId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RegistrationEntity::class,
            parentColumns = ["id"],
            childColumns = ["registrationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TicketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ticketToken: String,
    val registrationId: Long,
    val eventId: Long,
    val userId: Long,
    val eventTitle: String,
    val eventLocation: String,
    val eventDateTimeMillis: Long,
    val attendeeName: String,
    val attendeeStudentId: String, // Register Number / ID
    val attendeeEmail: String = "",
    val attendeeDepartment: String,
    val attendeeSemester: String = "Semester 6",
    val status: TicketStatus = TicketStatus.ACTIVE,
    val checkedInAt: Long? = null,
    val checkedInBy: String? = null,
    val antiProxyNonce: String = "", // Dynamic anti-proxy verification token
    val proxyFlagged: Boolean = false,
    val qrPayload: String,
    val issuedAt: Long = System.currentTimeMillis()
)
