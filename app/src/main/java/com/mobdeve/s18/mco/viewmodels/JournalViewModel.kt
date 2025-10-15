package com.mobdeve.s18.mco.viewmodels

import androidx.lifecycle.ViewModel
import com.mobdeve.s18.mco.PinJournalApp
import com.mobdeve.s18.mco.adapters.JournalAdapter
import com.mobdeve.s18.mco.models.JournalEntry
import com.mobdeve.s18.mco.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
            val entries = entryRepository.getEntriesByUserSortedByDate(currentUser.id)
            val journalItems = createJournalItems(entries)
            _uiState.value = _uiState.value.copy(
                entries = entries,
                journalItems = journalItems,
                isLoading = false
            )
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
            val filteredEntries = entryRepository.getEntriesForDate(currentUser.id, dateTimestamp)
            val journalItems = createJournalItems(filteredEntries)
            _uiState.value = _uiState.value.copy(
                journalItems = journalItems,
                selectedDate = dateTimestamp,
                isFiltered = true
            )
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
