package com.mobdeve.s18.mco.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobdeve.s18.mco.models.JournalEntry
import com.mobdeve.s18.mco.models.User

// FIX: Added User::class to entities
@Database(entities = [JournalEntry::class, User::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun userDao(): UserDao // FIX: Expose the UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pinjournal_database"
                )
                    .fallbackToDestructiveMigration() // IMPORTANT: Wipes data on schema change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}