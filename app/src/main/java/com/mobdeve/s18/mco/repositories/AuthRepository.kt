package com.mobdeve.s18.mco.repositories

import com.mobdeve.s18.mco.database.UserDao
import com.mobdeve.s18.mco.models.User
import com.mobdeve.s18.mco.preferences.UserPreferences
import com.mobdeve.s18.mco.session.SessionManager
import java.util.UUID

class AuthRepository(
    private val userDao: UserDao,
    private val userPreferences: UserPreferences,
    private val sessionManager: SessionManager
) {

    private var currentUser: User? = null

    // Check if user is already logged in (Session persistence)
    suspend fun checkAutoLogin(): User? {
        val savedId = userPreferences.getSessionUserId()
        if (savedId != null) {
            val user = userDao.getUserById(savedId)
            if (user != null) {
                currentUser = user
                // Also update SessionManager with username
                sessionManager.saveSession(user.username)
                return user
            }
        }
        return null
    }

    suspend fun signIn(username: String, password: String): Result<User> {
        // 1. Find user in DB
        val user = userDao.getUserByUsername(username)

        // 2. Verify password
        return if (user != null && user.passwordPlain == password) {
            currentUser = user
            userPreferences.saveSession(user.id)
            // Save username to SessionManager for display purposes
            sessionManager.saveSession(user.username)
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    suspend fun signUp(username: String, password: String, firstName: String = "", lastName: String = ""): Result<User> {
        // 1. Check if username exists
        val existing = userDao.getUserByUsername(username)
        if (existing != null) {
            return Result.failure(Exception("Username already exists"))
        }

        // 2. Create new User object
        val newUser = User(
            id = UUID.randomUUID().toString(),
            username = username,
            passwordPlain = password,
            firstName = firstName,
            lastName = lastName
        )

        // 3. Save to DB
        try {
            userDao.insertUser(newUser)
            // Auto-login after signup
            currentUser = newUser
            userPreferences.saveSession(newUser.id)
            // Save username to SessionManager
            sessionManager.saveSession(newUser.username)
            return Result.success(newUser)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<User> {
        return try {
            userDao.updateUser(user)
            currentUser = user // Update local cache
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        currentUser = null
        userPreferences.clearSession()
        sessionManager.clearSession()
    }

    fun getCurrentUser(): User? = currentUser
}