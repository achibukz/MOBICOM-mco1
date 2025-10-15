package com.mobdeve.s18.mco.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.adapters.PhotoGridAdapter
import com.mobdeve.s18.mco.utils.DateUtils
import com.mobdeve.s18.mco.viewmodels.EntryDetailViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class Activity_EntryDetail : AppCompatActivity() {

    private val viewModel: EntryDetailViewModel by viewModels()
    private lateinit var photoAdapter: PhotoGridAdapter
    private var entryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_entry_detail)

        entryId = intent.getStringExtra("entryId")

        setupToolbar()
        setupMap()
        setupPhotoGrid()
        setupUI()
        observeViewModel()

        entryId?.let { viewModel.loadEntry(it) }
    }

    override fun onResume() {
        super.onResume()
        findViewById<MapView>(R.id.mapView).onResume()
    }

    override fun onPause() {
        super.onPause()
        findViewById<MapView>(R.id.mapView).onPause()
    }

    private fun setupToolbar() {
        title = "Entry Details"
    }

    private fun setupMap() {
        val mapView = findViewById<MapView>(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
    }

    private fun setupPhotoGrid() {
        photoAdapter = PhotoGridAdapter { /* No remove functionality in detail view */ }

        findViewById<RecyclerView>(R.id.rvPhotos).apply {
            adapter = photoAdapter
            layoutManager = GridLayoutManager(this@Activity_EntryDetail, 3)
        }
    }

    private fun setupUI() {
        findViewById<MaterialButton>(R.id.btnEdit).setOnClickListener {
            // For this prototype, we'll just show a toast
            // In a full implementation, you'd navigate to AddEntry with edit mode
            Toast.makeText(this, "Edit functionality would be implemented here", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this entry? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteEntry()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.entry?.let { entry ->
                    // Populate UI with entry data
                    findViewById<TextView>(R.id.tvTitle).text = entry.title
                    findViewById<TextView>(R.id.tvDateTime).text = DateUtils.formatDateTime(entry.timestamp)
                    findViewById<TextView>(R.id.tvNotes).text = entry.notes.ifEmpty { "No notes" }

                    // Setup map
                    val mapView = findViewById<MapView>(R.id.mapView)
                    val location = GeoPoint(entry.latitude, entry.longitude)
                    mapView.controller.setZoom(15.0)
                    mapView.controller.setCenter(location)

                    // Add marker
                    val marker = Marker(mapView).apply {
                        position = location
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = entry.title
                    }
                    mapView.overlays.add(marker)
                    mapView.invalidate()

                    // Show photos if available
                    if (entry.photos.isNotEmpty()) {
                        findViewById<TextView>(R.id.tvPhotosLabel).visibility = View.VISIBLE
                        findViewById<RecyclerView>(R.id.rvPhotos).visibility = View.VISIBLE
                        photoAdapter.submitList(entry.photos)
                    }

                    // Show audio if available and automatically play it
                    if (entry.audioUri != null) {
                        findViewById<TextView>(R.id.tvAudioLabel).visibility = View.VISIBLE
                        findViewById<View>(R.id.layoutAudio).visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvAudioFile).text = "Audio File (Auto-playing...)"

                        // Store audioUri in a local variable to avoid smart cast issues
                        val audioUri = entry.audioUri
                        if (audioUri != null) {
                            // Automatically play the audio when entry is opened
                            com.mobdeve.s18.mco.utils.AudioPlayerUtils.playAudio(this@Activity_EntryDetail, audioUri)
                        }
                    }
                }

                state.errorMessage?.let { error ->
                    Toast.makeText(this@Activity_EntryDetail, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                if (state.isDeleted) {
                    Toast.makeText(this@Activity_EntryDetail, "Entry deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop audio when leaving the activity
        com.mobdeve.s18.mco.utils.AudioPlayerUtils.stopAudio()
    }
}
