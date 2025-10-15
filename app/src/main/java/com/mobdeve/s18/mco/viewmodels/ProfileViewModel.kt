package com.mobdeve.s18.mco.viewmodels

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

    fun updateProfile(username: String, password: String) {
        val currentUser = _uiState.value.user
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No user found")
            return
        }

        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields")
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val updatedUser = currentUser.copy(username = username, passwordPlain = password)
        val result = authRepository.updateUser(updatedUser)

        if (result.isSuccess) {
            viewModelScope.launch {
                sessionManager.saveSession(username) // Update session with new username
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = updatedUser,
                    username = username,
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
