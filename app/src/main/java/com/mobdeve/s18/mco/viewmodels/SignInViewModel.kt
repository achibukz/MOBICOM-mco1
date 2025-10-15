package com.mobdeve.s18.mco.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {

    private val authRepository = PinJournalApp.instance.authRepository
    private val sessionManager = PinJournalApp.instance.sessionManager

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please fill in all fields",
                isLoading = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val result = authRepository.signIn(username, password)
        if (result.isSuccess) {
            viewModelScope.launch {
                sessionManager.saveSession(username)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSignedIn = true,
                    user = result.getOrNull()
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Invalid username or password"
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class SignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignedIn: Boolean = false,
    val user: User? = null
)
