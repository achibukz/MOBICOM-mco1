package com.mobdeve.s18.mco.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // <--- IMPORT THIS
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.models.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch // <--- IMPORT THIS

class MapViewModel : ViewModel() {

    private val entryRepository = PinJournalApp.instance.entryRepository
    private val authRepository = PinJournalApp.instance.authRepository

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {

            // FIX: Launch a coroutine to fetch entries from the database
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val entries = entryRepository.getEntriesByUser(currentUser.id)

                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    isLoading = false
                )
            }
        }
    }

    fun refreshEntries() {
        loadEntries()
    }

    fun selectEntry(entry: JournalEntry) {
        _uiState.value = _uiState.value.copy(selectedEntry = entry)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedEntry = null)
    }
}

data class MapUiState(
    val isLoading: Boolean = true,
    val entries: List<JournalEntry> = emptyList(),
    val selectedEntry: JournalEntry? = null,
    val errorMessage: String? = null
)