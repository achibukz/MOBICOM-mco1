package com.mobdeve.s18.mco.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.models.EntryPhoto
import com.mobdeve.s18.mco.models.JournalEntry
import com.mobdeve.s18.mco.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AddEntryViewModel : ViewModel() {

    private val entryRepository = PinJournalApp.instance.entryRepository
    private val authRepository = PinJournalApp.instance.authRepository

    private val _uiState = MutableStateFlow(AddEntryUiState())
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun updateMood(mood: String) {
        _uiState.value = _uiState.value.copy(mood = mood)
    }

    fun updateLocation(latitude: Double, longitude: Double, address: String? = null) {
        _uiState.value = _uiState.value.copy(
            latitude = latitude,
            longitude = longitude,
            address = address
        )
    }

    fun addPhotos(uris: List<Uri>) {
        val currentPhotos = _uiState.value.photos.toMutableList()
        val newPhotos = uris.mapIndexed { index, uri ->
            EntryPhoto(uri, currentPhotos.size + index)
        }
        currentPhotos.addAll(newPhotos)
        _uiState.value = _uiState.value.copy(photos = currentPhotos)
    }

    fun removePhoto(photo: EntryPhoto) {
        val updatedPhotos = _uiState.value.photos.toMutableList()
        updatedPhotos.remove(photo)
        // Reindex remaining photos
        updatedPhotos.forEachIndexed { index, entryPhoto ->
            updatedPhotos[index] = entryPhoto.copy(orderIndex = index)
        }
        _uiState.value = _uiState.value.copy(photos = updatedPhotos)
    }

    fun setAudioFile(uri: Uri?, filename: String?) {
        _uiState.value = _uiState.value.copy(
            audioUri = uri,
            audioFilename = filename
        )
    }

    fun saveEntry(): Boolean {
        val state = _uiState.value
        val currentUser = authRepository.getCurrentUser()

        if (currentUser == null) {
            _uiState.value = state.copy(errorMessage = "User not signed in")
            return false
        }

        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Title is required")
            return false
        }

        if (state.latitude == 0.0 && state.longitude == 0.0) {
            _uiState.value = state.copy(errorMessage = "Location is required")
            return false
        }

        val entry = JournalEntry(
            id = "", // Will be set by repository
            userId = currentUser.id,
            title = state.title,
            notes = state.notes,
            mood = state.mood,
            timestamp = DateUtils.getCurrentTimestamp(),
            latitude = state.latitude,
            longitude = state.longitude,
            address = state.address,
            photos = state.photos.toMutableList(),
            audioUri = state.audioUri
        )

        entryRepository.addEntry(entry)
        _uiState.value = state.copy(isSaved = true)
        return true
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class AddEntryUiState(
    val title: String = "",
    val notes: String = "",
    val mood: String = "Happy",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val photos: List<EntryPhoto> = emptyList(),
    val audioUri: Uri? = null,
    val audioFilename: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)
