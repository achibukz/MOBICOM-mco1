package com.mobdeve.s18.mco.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // <--- IMPORT THIS
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.adapters.JournalAdapter
import com.mobdeve.s18.mco.models.JournalEntry
import com.mobdeve.s18.mco.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch // <--- IMPORT THIS

class JournalViewModel : ViewModel() {

    private val entryRepository = PinJournalApp.instance.entryRepository
    private val authRepository = PinJournalApp.instance.authRepository

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {

            // FIX 1: Launch coroutine
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // FIX 2: Use 'getEntriesByUser' instead of 'getEntriesByUserSortedByDate'
                // (The SQL query we wrote earlier already includes "ORDER BY timestamp DESC")
                val entries = entryRepository.getEntriesByUser(currentUser.id)

                val journalItems = createJournalItems(entries)
                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    journalItems = journalItems,
                    isLoading = false
                )
            }
        }
    }

    private fun createJournalItems(entries: List<JournalEntry>): List<JournalAdapter.JournalItem> {
        val items = mutableListOf<JournalAdapter.JournalItem>()
        var currentMonth: String? = null

        entries.forEach { entry ->
            val monthYear = DateUtils.formatMonthYear(entry.timestamp)

            if (monthYear != currentMonth) {
                items.add(JournalAdapter.JournalItem.Header(monthYear))
                currentMonth = monthYear
            }

            items.add(JournalAdapter.JournalItem.Entry(entry))
        }

        return items
    }

    fun toggleTimelineView() {
        _uiState.value = _uiState.value.copy(
            isTimelineView = !_uiState.value.isTimelineView
        )
    }

    fun filterByDate(dateTimestamp: Long) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {

            // FIX 3: Launch coroutine for filtering as well
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val filteredEntries = entryRepository.getEntriesForDate(currentUser.id, dateTimestamp)
                val journalItems = createJournalItems(filteredEntries)

                _uiState.value = _uiState.value.copy(
                    journalItems = journalItems,
                    selectedDate = dateTimestamp,
                    isFiltered = true,
                    isLoading = false
                )
            }
        }
    }

    fun clearDateFilter() {
        _uiState.value = _uiState.value.copy(
            selectedDate = null,
            isFiltered = false
        )
        loadEntries()
    }

    fun refreshEntries() {
        if (_uiState.value.isFiltered && _uiState.value.selectedDate != null) {
            filterByDate(_uiState.value.selectedDate!!)
        } else {
            loadEntries()
        }
    }
}

data class JournalUiState(
    val isLoading: Boolean = true,
    val entries: List<JournalEntry> = emptyList(),
    val journalItems: List<JournalAdapter.JournalItem> = emptyList(),
    val isTimelineView: Boolean = false,
    val selectedDate: Long? = null,
    val isFiltered: Boolean = false,
    val errorMessage: String? = null
)