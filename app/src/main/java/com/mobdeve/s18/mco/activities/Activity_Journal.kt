package com.mobdeve.s18.mco.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.adapters.JournalAdapter
import com.mobdeve.s18.mco.viewmodels.JournalViewModel
import kotlinx.coroutines.launch

class Activity_Journal : AppCompatActivity() {

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal)

        setupToolbar()
        setupRecyclerView()
        setupUI()
        setupBottomNavigation()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshEntries()
        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_journal
    }

    private fun setupToolbar() {
        title = "Journal"
    }

    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter { entry ->
            val intent = Intent(this, Activity_EntryDetail::class.java)
            intent.putExtra("entryId", entry.id)
            startActivity(intent)
        }

        findViewById<RecyclerView>(R.id.rvJournalEntries).apply {
            adapter = journalAdapter
            layoutManager = LinearLayoutManager(this@Activity_Journal)
        }
    }

    private fun setupUI() {
        findViewById<MaterialButton>(R.id.btnToggleView).setOnClickListener {
            viewModel.toggleTimelineView()
        }

        findViewById<CalendarView>(R.id.calendarView).setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            viewModel.filterByDate(calendar.timeInMillis)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_journal

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Activity_Home::class.java))
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, Activity_Map::class.java))
                    true
                }
                R.id.nav_journal -> true
                R.id.nav_add -> {
                    startActivity(Intent(this, Activity_AddEntry::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update toggle button text
                val toggleBtn = findViewById<MaterialButton>(R.id.btnToggleView)
                toggleBtn.text = if (state.isTimelineView) {
                    getString(R.string.list_view)
                } else {
                    getString(R.string.timeline_view)
                }

                // Show/hide calendar
                findViewById<CalendarView>(R.id.calendarView).visibility = if (state.isTimelineView) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Update adapter
                journalAdapter.submitList(state.journalItems)

                // Handle date filter
                if (state.isFiltered) {
                    // Show clear filter option in toolbar or as button
                    supportActionBar?.subtitle = "Filtered by date"
                } else {
                    supportActionBar?.subtitle = null
                }
            }
        }
    }
}
