package com.mobdeve.s18.mco.repositories

import com.mobdeve.s18.mco.models.User
import com.mobdeve.s18.mco.preferences.UserPreferences
import java.util.UUID

class AuthRepository(private val userPreferences: UserPreferences) {
    private var users = mutableMapOf<String, User>()
    private var currentUser: User? = null

    init {
        // Load existing users from SharedPreferences
        users = userPreferences.loadUsers()

        // Add default test account if no users exist
        if (users.isEmpty()) {
            val testUser = User(
                id = UUID.randomUUID().toString(),
                username = "test",
                passwordPlain = "123456"
            )
            users["test"] = testUser
            saveUsers() // Save the test user
        }
    }

    // Persist the users map to SharedPreferences via UserPreferences
    private fun saveUsers() {
        userPreferences.saveUsers(users)
    }

    fun signUp(username: String, password: String): Result<User> {
        if (users.containsKey(username)) {
            return Result.failure(Exception("Username already exists"))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            passwordPlain = password
        )

        users[username] = user
        saveUsers() // persist new user
        return Result.success(user)
    }

    fun signIn(username: String, password: String): Result<User> {
        val user = users[username]
        return if (user != null && user.passwordPlain == password) {
            currentUser = user
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    fun getCurrentUser(): User? = currentUser

    fun updateUser(updatedUser: User): Result<User> {
        val oldUsername = currentUser?.username
        if (oldUsername != null && users.containsKey(oldUsername)) {
            users.remove(oldUsername)
            users[updatedUser.username] = updatedUser
            currentUser = updatedUser
            saveUsers() // persist changes
            return Result.success(updatedUser)
        }
        return Result.failure(Exception("User not found"))
    }

    fun signOut() {
        currentUser = null
    }
}
