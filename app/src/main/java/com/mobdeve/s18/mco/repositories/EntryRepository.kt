package com.mobdeve.s18.mco.repositories

import com.mobdeve.s18.mco.database.EntryDao
import com.mobdeve.s18.mco.models.JournalEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

// Now takes the DAO as a parameter instead of an empty list
class EntryRepository(private val entryDao: EntryDao) {

    // Marked as suspend because database operations must run in background
    suspend fun addEntry(entry: JournalEntry): String {
        val newId = if (entry.id.isEmpty()) UUID.randomUUID().toString() else entry.id
        val newEntry = entry.copy(id = newId)
        entryDao.insertEntry(newEntry)
        return newEntry.id
    }

    suspend fun updateEntry(entry: JournalEntry) {
        entryDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entryId: String) {
        entryDao.deleteEntry(entryId)
    }

    suspend fun getEntry(entryId: String): JournalEntry? {
        return entryDao.getEntry(entryId)
    }

    suspend fun getEntriesByUser(userId: String): List<JournalEntry> {
        return entryDao.getEntriesByUser(userId)
    }

    suspend fun getRecentEntries(userId: String, limit: Int = 10): List<JournalEntry> {
        return entryDao.getRecentEntries(userId, limit)
    }

    suspend fun getEntriesForDate(userId: String, dateTimestamp: Long): List<JournalEntry> {
        val dayStart = (dateTimestamp / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000)
        val dayEnd = dayStart + (24 * 60 * 60 * 1000) - 1

        return entryDao.getEntriesForDate(userId, dayStart, dayEnd)
    }
}