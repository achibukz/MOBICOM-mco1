package com.mobdeve.s18.mco.repositories

import com.mobdeve.s18.mco.models.User
import java.util.UUID

class AuthRepository {
    private val users = mutableMapOf<String, User>()
    private var currentUser: User? = null

    init {
        // Add a default test account for easier testing
        val testUser = User(
            id = UUID.randomUUID().toString(),
            username = "test",
            passwordPlain = "123456"
        )
        users["test"] = testUser
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
            return Result.success(updatedUser)
        }
        return Result.failure(Exception("User not found"))
    }

    fun signOut() {
        currentUser = null
    }
}
