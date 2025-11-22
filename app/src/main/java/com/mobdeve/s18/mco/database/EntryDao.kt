package com.mobdeve.s18.mco.database

import androidx.room.*
import com.mobdeve.s18.mco.models.JournalEntry

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Query("DELETE FROM entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: String)

    @Query("SELECT * FROM entries WHERE id = :entryId")
    suspend fun getEntry(entryId: String): JournalEntry?

    @Query("SELECT * FROM entries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getEntriesByUser(userId: String): List<JournalEntry>

    @Query("SELECT * FROM entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEntries(userId: String, limit: Int): List<JournalEntry>

    // Range query for specific dates
    @Query("SELECT * FROM entries WHERE userId = :userId AND timestamp BETWEEN :dayStart AND :dayEnd ORDER BY timestamp DESC")
    suspend fun getEntriesForDate(userId: String, dayStart: Long, dayEnd: Long): List<JournalEntry>
}