package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY issuedAt DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE userId = :userId ORDER BY issuedAt DESC")
    fun getTicketsForUser(userId: Long): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE eventId = :eventId ORDER BY attendeeName ASC")
    fun getTicketsForEvent(eventId: Long): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE eventId = :eventId ORDER BY attendeeName ASC")
    suspend fun getTicketsListForEvent(eventId: Long): List<TicketEntity>

    @Query("SELECT * FROM tickets WHERE ticketToken = :token LIMIT 1")
    suspend fun getTicketByToken(token: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: Long): TicketEntity?

    @Query("SELECT * FROM tickets WHERE eventId = :eventId AND userId = :userId AND status != 'CANCELLED' LIMIT 1")
    suspend fun getActiveTicketForUserAndEvent(eventId: Long, userId: Long): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<TicketEntity>)

    @Update
    suspend fun updateTicket(ticket: TicketEntity)

    @Query("UPDATE tickets SET status = :status, checkedInAt = :checkedInAt, checkedInBy = :checkedInBy WHERE id = :ticketId")
    suspend fun updateTicketCheckIn(ticketId: Long, status: TicketStatus, checkedInAt: Long?, checkedInBy: String?)

    @Query("UPDATE tickets SET status = 'CANCELLED' WHERE registrationId = :registrationId")
    suspend fun cancelTicketByRegistration(registrationId: Long)
}
