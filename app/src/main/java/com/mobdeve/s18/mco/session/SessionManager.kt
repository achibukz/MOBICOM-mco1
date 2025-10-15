package com.mobdeve.s18.mco.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionManager(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("session")
        private val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        private val ACTIVE_USERNAME = stringPreferencesKey("active_username")
    }

    suspend fun saveSession(username: String) {
        context.dataStore.edit { preferences ->
            preferences[IS_SIGNED_IN] = true
            preferences[ACTIVE_USERNAME] = username
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[IS_SIGNED_IN] = false
            preferences.remove(ACTIVE_USERNAME)
        }
    }

    val isSignedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_SIGNED_IN] ?: false
    }

    val activeUsername: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_USERNAME]
    }
}
