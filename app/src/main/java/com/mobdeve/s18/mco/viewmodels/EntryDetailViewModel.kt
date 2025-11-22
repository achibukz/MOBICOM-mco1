package com.mobdeve.s18.mco.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.models.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EntryDetailViewModel : ViewModel() {

    private val entryRepository = PinJournalApp.instance.entryRepository

    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState.asStateFlow()

    fun loadEntry(entryId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // FIX 1: Launch a coroutine to fetch data in background
        viewModelScope.launch {
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
    }

    fun deleteEntry() {
        val entry = _uiState.value.entry
        if (entry != null) {

            viewModelScope.launch {
                // Perform the delete
                entryRepository.deleteEntry(entry.id)

                // Update the UI to say "We are done"
                _uiState.value = _uiState.value.copy(isDeleted = true)
            }
        }
    }

    fun removePhoto(photoToRemove: com.mobdeve.s18.mco.models.EntryPhoto) {
        val entry = _uiState.value.entry
        if (entry != null) {
            viewModelScope.launch {
                // Remove the photo from the list
                val updatedPhotos = entry.photos.toMutableList()
                updatedPhotos.remove(photoToRemove)

                // Reindex remaining photos
                updatedPhotos.forEachIndexed { index, photo ->
                    updatedPhotos[index] = photo.copy(orderIndex = index)
                }

                // Update the entry with the new photo list
                val updatedEntry = entry.copy(photos = updatedPhotos)
                entryRepository.updateEntry(updatedEntry)

                // Update the UI state
                _uiState.value = _uiState.value.copy(entry = updatedEntry)
            }
        }
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
