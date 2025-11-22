package com.mobdeve.s18.mco.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries") // This defines the SQL table name
data class JournalEntry(
    @PrimaryKey val id: String, // ID is now the Primary Key
    val userId: String,
    val title: String,
    val notes: String,
    val mood: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val photos: MutableList<EntryPhoto>, // Handled by Converters
    val audioUri: Uri? // Handled by Converters
)