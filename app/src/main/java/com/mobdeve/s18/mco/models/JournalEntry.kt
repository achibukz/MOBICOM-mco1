package com.mobdeve.s18.mco.models

import android.net.Uri

data class JournalEntry(
    val id: String,
    val userId: String,
    var title: String,
    var notes: String,
    var mood: String,
    var timestamp: Long,
    var latitude: Double,
    var longitude: Double,
    var address: String? = null,
    var photos: MutableList<EntryPhoto> = mutableListOf(),
    var audioUri: Uri? = null
)
