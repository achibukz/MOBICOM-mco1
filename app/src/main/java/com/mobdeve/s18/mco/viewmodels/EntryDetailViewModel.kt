package com.mobdeve.s18.mco.viewmodels

import androidx.lifecycle.ViewModel
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.models.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EntryDetailViewModel : ViewModel() {

    private val entryRepository = PinJournalApp.instance.entryRepository

    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState.asStateFlow()

    fun loadEntry(entryId: String) {
        val entry = entryRepository.getEntry(entryId)
        if (entry != null) {
            _uiState.value = _uiState.value.copy(
                entry = entry,
                isLoading = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Entry not found"
            )
        }
    }

    fun deleteEntry(): Boolean {
        val entry = _uiState.value.entry
        if (entry != null) {
            val success = entryRepository.deleteEntry(entry.id)
            if (success) {
                _uiState.value = _uiState.value.copy(isDeleted = true)
            }
            return success
        }
        return false
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class EntryDetailUiState(
    val isLoading: Boolean = true,
    val entry: JournalEntry? = null,
    val errorMessage: String? = null,
    val isDeleted: Boolean = false
)
