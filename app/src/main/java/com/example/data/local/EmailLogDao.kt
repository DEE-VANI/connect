package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.EmailLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailLogDao {
    @Query("SELECT * FROM email_logs WHERE userId = :userId ORDER BY sentAt DESC")
    fun getEmailsForUser(userId: Long): Flow<List<EmailLogEntity>>

    @Query("SELECT * FROM email_logs ORDER BY sentAt DESC LIMIT 100")
    fun getAllEmails(): Flow<List<EmailLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmailLog(emailLog: EmailLogEntity): Long
}
