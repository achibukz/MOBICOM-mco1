package com.mobdeve.s18.mco

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.mobdeve.s18.mco.preferences.ThemePreferences
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

        // Apply saved theme preference at app startup so activities use correct theme
        val themePref = ThemePreferences(this)
        val isDark = themePref.isDarkMode()
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        instance = this
    }

    companion object {
        lateinit var instance: PinJournalApp
            private set
    }
}
