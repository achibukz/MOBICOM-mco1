package com.mobdeve.s18.mco.models

import android.net.Uri

data class EntryPhoto(
    val uriString: String, // CHANGED: Store as String, not Uri
    val orderIndex: Int
) {
    // Helper function to get a Uri back when you need to display it in an ImageView
    fun getUri(): Uri {
        return Uri.parse(uriString)
    }
}