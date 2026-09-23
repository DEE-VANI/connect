package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_logs")
data class EmailLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val recipientEmail: String,
    val subject: String,
    val body: String,
    val emailType: String, // "REGISTRATION_CONFIRMATION", "CHECK_IN_CONFIRMATION", "EVENT_UPDATE"
    val sentAt: Long = System.currentTimeMillis()
)
