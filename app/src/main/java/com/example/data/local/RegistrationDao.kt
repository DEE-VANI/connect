package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.RegistrationEntity
import com.example.data.local.entities.RegistrationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM registrations ORDER BY registeredAt DESC")
    fun getAllRegistrations(): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE eventId = :eventId AND status = 'CONFIRMED'")
    fun getActiveRegistrationsForEvent(eventId: Long): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE eventId = :eventId")
    fun getAllRegistrationsForEvent(eventId: Long): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE userId = :userId ORDER BY registeredAt DESC")
    fun getRegistrationsForUser(userId: Long): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE eventId = :eventId AND userId = :userId AND status = 'CONFIRMED' LIMIT 1")
    suspend fun getActiveRegistration(eventId: Long, userId: Long): RegistrationEntity?

    @Query("SELECT COUNT(*) FROM registrations WHERE eventId = :eventId AND status = 'CONFIRMED'")
    fun getActiveRegistrationCount(eventId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM registrations WHERE eventId = :eventId AND status = 'CONFIRMED'")
    suspend fun getActiveRegistrationCountDirect(eventId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: RegistrationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistrations(registrations: List<RegistrationEntity>)

    @Update
    suspend fun updateRegistration(registration: RegistrationEntity)

    @Query("UPDATE registrations SET status = :status WHERE id = :id")
    suspend fun updateRegistrationStatus(id: Long, status: RegistrationStatus)
}
