package com.mobdeve.s18.mco.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import android.widget.TextView
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.adapters.RecentEntriesAdapter
import com.mobdeve.s18.mco.viewmodels.HomeViewModel
import kotlinx.coroutines.launch

class Activity_Home : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var recentEntriesAdapter: RecentEntriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupRecyclerView()
        setupUI()
        setupBottomNavigation()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshEntries()
        // Highlight the correct tab
        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_home
    }

    private fun setupRecyclerView() {
        recentEntriesAdapter = RecentEntriesAdapter { entry ->
            val intent = Intent(this, Activity_EntryDetail::class.java)
            intent.putExtra("entryId", entry.id)
            startActivity(intent)
        }

        findViewById<RecyclerView>(R.id.rvRecentEntries).apply {
            adapter = recentEntriesAdapter
            layoutManager = LinearLayoutManager(this@Activity_Home)
        }
    }

    private fun setupUI() {
        findViewById<MaterialCardView>(R.id.cardNewEntry).setOnClickListener {
            startActivity(Intent(this, Activity_AddEntry::class.java))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_map -> {
                    startActivity(Intent(this, Activity_Map::class.java))
                    true
                }
                R.id.nav_journal -> {
                    startActivity(Intent(this, Activity_Journal::class.java))
                    true
                }
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
                state.username?.let { username ->
                    findViewById<TextView>(R.id.tvWelcome).text = "Welcome back, $username!"
                }

                recentEntriesAdapter.submitList(state.recentEntries)
            }
        }
    }
}
