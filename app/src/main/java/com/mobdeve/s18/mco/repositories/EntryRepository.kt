package com.mobdeve.s18.mco.repositories

import com.mobdeve.s18.mco.models.JournalEntry
import java.util.UUID

class EntryRepository {
    private val entries = mutableListOf<JournalEntry>()

    fun addEntry(entry: JournalEntry): String {
        val newEntry = entry.copy(id = UUID.randomUUID().toString())
        entries.add(newEntry)
        return newEntry.id
    }

    fun updateEntry(entry: JournalEntry): Boolean {
        val index = entries.indexOfFirst { it.id == entry.id }
        return if (index != -1) {
            entries[index] = entry
            true
        } else {
            false
        }
    }

    fun deleteEntry(entryId: String): Boolean {
        return entries.removeAll { it.id == entryId }
    }

    fun getEntry(entryId: String): JournalEntry? {
        return entries.find { it.id == entryId }
    }

    fun getAllEntries(): List<JournalEntry> {
        return entries.toList()
    }

    fun getEntriesByUser(userId: String): List<JournalEntry> {
        return entries.filter { it.userId == userId }
    }

    fun getEntriesByUserSortedByDate(userId: String): List<JournalEntry> {
        return entries.filter { it.userId == userId }
            .sortedByDescending { it.timestamp }
    }

    fun getRecentEntries(userId: String, limit: Int = 10): List<JournalEntry> {
        return entries.filter { it.userId == userId }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    fun getEntriesForDate(userId: String, dateTimestamp: Long): List<JournalEntry> {
        val dayStart = (dateTimestamp / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000)
        val dayEnd = dayStart + (24 * 60 * 60 * 1000) - 1

        return entries.filter { entry ->
            entry.userId == userId && entry.timestamp in dayStart..dayEnd
        }.sortedByDescending { it.timestamp }
    }
}
