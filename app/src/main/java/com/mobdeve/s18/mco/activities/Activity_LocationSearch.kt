package com.mobdeve.s18.mco.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.appbar.MaterialToolbar
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.adapters.LocationSearchAdapter
import com.mobdeve.s18.mco.utils.LocationSearchUtils
import kotlinx.coroutines.launch

class Activity_LocationSearch : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var layoutLoading: View
    private lateinit var searchAdapter: LocationSearchAdapter

    companion object {
        const val EXTRA_SELECTED_LOCATION = "selected_location"
        const val EXTRA_LOCATION_NAME = "location_name"
        const val EXTRA_LOCATION_LAT = "location_lat"
        const val EXTRA_LOCATION_LON = "location_lon"
        const val EXTRA_LOCATION_DISPLAY = "location_display"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_search)

        setupViews()
        setupRecyclerView()
        setupListeners()
    }

    private fun setupViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutLoading = findViewById(R.id.layoutLoading)
    }

    private fun setupRecyclerView() {
        searchAdapter = LocationSearchAdapter { searchResult ->
            // Return selected location to parent activity
            val resultIntent = Intent().apply {
                putExtra(EXTRA_LOCATION_NAME, searchResult.name)
                putExtra(EXTRA_LOCATION_LAT, searchResult.latitude)
                putExtra(EXTRA_LOCATION_LON, searchResult.longitude)
                putExtra(EXTRA_LOCATION_DISPLAY, searchResult.displayName)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        rvSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@Activity_LocationSearch)
        }
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            performSearch()
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim()
        if (query.isBlank()) {
            Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val results = LocationSearchUtils.searchLocation(query)
                runOnUiThread {
                    showLoading(false)
                    if (results.isNotEmpty()) {
                        showResults(results)
                        Toast.makeText(this@Activity_LocationSearch, "Found ${results.size} locations", Toast.LENGTH_SHORT).show()
                    } else {
                        showEmpty("No locations found for '$query'.\n\nTry:\n• Different spelling\n• More specific terms\n• Adding 'Philippines' to your search")
                        Toast.makeText(this@Activity_LocationSearch, "No results found. Check your internet connection.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    val errorMsg = "Search failed: ${e.message}\n\nPlease check your internet connection and try again."
                    showEmpty(errorMsg)
                    Toast.makeText(this@Activity_LocationSearch, "Search failed. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            layoutLoading.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            rvSearchResults.visibility = View.GONE
        } else {
            layoutLoading.visibility = View.GONE
        }
    }

    private fun showResults(results: List<LocationSearchUtils.Companion.SearchResult>) {
        searchAdapter.submitList(results)
        rvSearchResults.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
        layoutLoading.visibility = View.GONE
    }

    private fun showEmpty(message: String? = null) {
        layoutEmpty.visibility = View.VISIBLE
        rvSearchResults.visibility = View.GONE
        layoutLoading.visibility = View.GONE

        if (message != null) {
            val tvEmptyMessage = layoutEmpty.findViewById<TextView>(R.id.tvEmptyMessage)
            tvEmptyMessage?.text = message
        }
    }
}
