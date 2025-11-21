package com.mobdeve.s18.mco.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobdeve.s18.mco.models.User

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_prefs",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    companion object {
        private const val KEY_USERS = "users_list"
    }

    // Save all users
    fun saveUsers(users: Map<String, User>) {
        val json = gson.toJson(users)
        prefs.edit().putString(KEY_USERS, json).apply()
    }

    // Load all users
    fun loadUsers(): MutableMap<String, User> {
        val json = prefs.getString(KEY_USERS, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, User>>() {}.type
        return gson.fromJson(json, type) ?: mutableMapOf()
    }

    // Clear all users
    fun clearUsers() {
        prefs.edit().remove(KEY_USERS).apply()
    }
}