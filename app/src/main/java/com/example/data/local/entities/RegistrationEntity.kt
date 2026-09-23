package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RegistrationStatus {
    CONFIRMED,
    CANCELLED
}

@Entity(
    tableName = "registrations",
    indices = [
        Index(value = ["eventId", "userId"]),
        Index(value = ["userId"]),
        Index(value = ["registrationNumber"], unique = true)
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
        )
    ]
)
data class RegistrationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: Long,
    val userId: Long,
    val registrationNumber: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val status: RegistrationStatus = RegistrationStatus.CONFIRMED
)
