package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.EmailLogEntity
import com.example.data.local.entities.EventEntity
import com.example.data.local.entities.RegistrationEntity
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        EventEntity::class,
        RegistrationEntity::class,
        TicketEntity::class,
        AuditLogEntity::class,
        EmailLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun registrationDao(): RegistrationDao
    abstract fun ticketDao(): TicketDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun emailLogDao(): EmailLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_connect.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
