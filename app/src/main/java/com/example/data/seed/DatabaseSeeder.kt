package com.example.data.seed

import com.example.data.local.AppDatabase

object DatabaseSeeder {

    /**
     * In Real-World Production mode, the database starts completely clean and empty.
     * Users create real accounts via the registration page, and organizers publish real events.
     */
    suspend fun seedIfEmpty(database: AppDatabase) {
        // Real-world clean state: No mock events or mock tickets are auto-seeded.
    }

    /**
     * Empties all tables in the database to guarantee a completely clean, pristine slate
     * for real-world campus usage.
     */
    suspend fun emptyDatabase(database: AppDatabase) {
        database.clearAllTables()
    }
}
