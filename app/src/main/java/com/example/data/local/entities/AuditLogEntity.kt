package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actorId: Long,
    val actorName: String,
    val actorRole: String,
    val action: String, // e.g., "CHECK_IN", "REGISTER_EVENT", "CANCEL_REGISTRATION", "CREATE_EVENT", "ARCHIVE_EVENT"
    val targetResource: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
