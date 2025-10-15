package com.mobdeve.s18.mco.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.models.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val entryRepository = PinJournalApp.instance.entryRepository
    private val authRepository = PinJournalApp.instance.authRepository
    private val sessionManager = PinJournalApp.instance.sessionManager

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRecentEntries()
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.activeUsername.collect { username ->
                _uiState.value = _uiState.value.copy(username = username)
            }
        }
    }

    fun loadRecentEntries() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            val recentEntries = entryRepository.getRecentEntries(currentUser.id, 10)
            _uiState.value = _uiState.value.copy(
                recentEntries = recentEntries,
                isLoading = false
            )
        }
    }

    fun refreshEntries() {
        loadRecentEntries()
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val recentEntries: List<JournalEntry> = emptyList(),
    val username: String? = null,
    val errorMessage: String? = null
)
