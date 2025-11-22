package com.mobdeve.s18.mco.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // <--- Added this import
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch // <--- Added this import

class SignUpViewModel : ViewModel() {

    private val authRepository = PinJournalApp.instance.authRepository

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(username: String, password: String, confirmPassword: String) {
        // Validation
        when {
            username.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Please fill in all fields",
                    isLoading = false
                )
                return
            }
            password != confirmPassword -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Passwords don't match",
                    isLoading = false
                )
                return
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Password must be at least 6 characters",
                    isLoading = false
                )
                return
            }
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // FIX: Must use viewModelScope.launch because signUp is now a database operation (suspend)
        viewModelScope.launch {
            // Note: We are passing empty strings for firstName/lastName as they aren't in the UI form yet
            val result = authRepository.signUp(username, password)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSignUpSuccess = true,
                    user = result.getOrNull()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Sign up failed"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class SignUpUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignUpSuccess: Boolean = false,
    val user: User? = null
)