package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole {
    STUDENT,
    ORGANIZER,
    ADMIN
}

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val fullName: String,
    val studentId: String, // Register Number / ID e.g. "STU-2024-8841"
    val department: String, // e.g. "Computer Science & Engineering"
    val yearOfStudy: String, // e.g. "Class of 2026"
    val semester: String = "Semester 6", // e.g. "Semester 6", "Semester 4"
    val phone: String = "+1 (555) 234-5678",
    val role: UserRole,
    val isActive: Boolean = true,
    val avatarColorHex: String = "#1E3A8A",
    val passwordHash: String = "campus123",
    val createdAt: Long = System.currentTimeMillis()
)
