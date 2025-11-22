package com.mobdeve.s18.mco.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_session_prefs", // Changed name to avoid conflict with old file
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    // Save session (Login)
    fun saveSession(userId: String) {
        prefs.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }

    // Get current session
    fun getSessionUserId(): String? {
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }

    // Clear session (Logout)
    fun clearSession() {
        prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
    }
}