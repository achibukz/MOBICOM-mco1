package com.mobdeve.s18.mco

import android.app.Application
import com.mobdeve.s18.mco.repositories.AuthRepository
import com.mobdeve.s18.mco.repositories.EntryRepository
import com.mobdeve.s18.mco.session.SessionManager
import com.mobdeve.s18.mco.preferences.UserPreferences

class PinJournalApp : Application() {

    // Singleton instances
    val userPreferences by lazy { UserPreferences(this) }
    val authRepository by lazy { AuthRepository(userPreferences) }
    val entryRepository by lazy { EntryRepository() }
    val sessionManager by lazy { SessionManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PinJournalApp
            private set
    }
}
