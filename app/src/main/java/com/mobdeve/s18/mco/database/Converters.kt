package com.mobdeve.s18.mco.database

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobdeve.s18.mco.models.EntryPhoto

class Converters {

    private val gson = Gson()

    // --- Room Converters ---

    // 1. Handle Audio URI (Keep this as is)
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return uriString?.let { Uri.parse(it) }
    }

    // 2. Handle List<EntryPhoto>
    @TypeConverter
    fun fromPhotoList(photos: List<EntryPhoto>?): String? {
        // Gson can now easily save this because it's just Strings and Ints
        return gson.toJson(photos)
    }

    @TypeConverter
    fun toPhotoList(photosString: String?): List<EntryPhoto>? {
        if (photosString == null) return emptyList()
        val type = object : TypeToken<List<EntryPhoto>>() {}.type
        return gson.fromJson(photosString, type)
    }
}