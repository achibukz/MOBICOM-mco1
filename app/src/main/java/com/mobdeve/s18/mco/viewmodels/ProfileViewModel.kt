package com.mobdeve.s18.mco.viewmodels

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val authRepository = PinJournalApp.instance.authRepository
    private val sessionManager = PinJournalApp.instance.sessionManager

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            _uiState.value = _uiState.value.copy(
                user = currentUser,
                username = currentUser.username,
                isLoading = false
            )
        }
    }

    // Unified update method: can update username/password and/or profile fields
    fun updateProfile(
        username: String? = null,
        password: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        mobile: String? = null,
        profileImageUri: String? = null
    ) {
        val currentUser = _uiState.value.user
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No user found")
            return
        }

        // Check auth fields independently so user can change username OR password (not necessarily both)
        val updatingAuth = username != null || password != null
        if (updatingAuth) {
            if (username != null) {
                if (username.isBlank()) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Please provide a valid username")
                    return
                }
            }

            if (password != null) {
                if (password.isBlank()) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Please provide a valid password")
                    return
                }
                if (password.length < 6) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters")
                    return
                }
            }
        }

        // If updating profile details, require all profile fields provided (matches previous behaviour)
        val updatingProfile = firstName != null || lastName != null || email != null || mobile != null
        if (updatingProfile) {
            if (firstName.isNullOrBlank() || lastName.isNullOrBlank() || email.isNullOrBlank() || mobile.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields")
                return
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email")
                return
            }
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Build updated user by copying existing values and applying provided ones
        val updatedUser = currentUser.copy(
            username = username ?: currentUser.username,
            passwordPlain = password ?: currentUser.passwordPlain,
            firstName = firstName ?: currentUser.firstName,
            lastName = lastName ?: currentUser.lastName,
            email = email ?: currentUser.email,
            mobile = mobile ?: currentUser.mobile,
            profileImageUri = profileImageUri ?: currentUser.profileImageUri
        )

        val result = authRepository.updateUser(updatedUser)

        if (result.isSuccess) {
            viewModelScope.launch {
                // If username changed, update session
                if (updatingAuth && username != null) {
                    sessionManager.saveSession(username)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = updatedUser,
                    username = updatedUser.username,
                    isUpdateSuccess = true
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message ?: "Update failed"
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            sessionManager.clearSession()
            _uiState.value = _uiState.value.copy(isSignedOut = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearUpdateSuccess() {
        _uiState.value = _uiState.value.copy(isUpdateSuccess = false)
    }
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val username: String = "",
    val errorMessage: String? = null,
    val isUpdateSuccess: Boolean = false,
    val isSignedOut: Boolean = false
)
