package com.mobdeve.s18.mco.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // Added for correct Drawable handling
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.adapters.PhotoGridAdapter
import com.mobdeve.s18.mco.utils.AudioPlayerUtils
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
        // Disable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun setupMap() {
        val mapView = findViewById<MapView>(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
    }

    private fun setupPhotoGrid() {
        photoAdapter = PhotoGridAdapter { photo ->
            // Enable photo deletion in detail view
            viewModel.removePhoto(photo)
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        }

        findViewById<RecyclerView>(R.id.rvPhotos).apply {
            adapter = photoAdapter
            layoutManager = GridLayoutManager(this@Activity_EntryDetail, 3)
        }
    }

    private fun setupUI() {
        findViewById<MaterialButton>(R.id.btnEdit).setOnClickListener {
            val intent = Intent(this, Activity_EditEntry::class.java)
            intent.putExtra(Activity_EditEntry.EXTRA_ENTRY_ID, entryId)
            startActivityForResult(intent, EDIT_ENTRY_REQUEST_CODE)
        }

        // The Listener simply triggers the ViewModel command
        findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this entry? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // FIX: This is now a fire-and-forget call.
                // We do not check a return value here. We wait for the observer.
                viewModel.deleteEntry()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->

                // 1. Check for Delete Success FIRST
                // If deleted, close screen immediately to prevent crashes or UI flickers
                if (state.isDeleted) {
                    Toast.makeText(this@Activity_EntryDetail, "Entry deleted", Toast.LENGTH_SHORT).show()
                    finish()
                    return@collect // Stop processing this state
                }

                // 2. Check for Errors
                state.errorMessage?.let { error ->
                    Toast.makeText(this@Activity_EntryDetail, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                // 3. Update UI with Entry Data
                state.entry?.let { entry ->
                    findViewById<TextView>(R.id.tvTitle).text = entry.title
                    findViewById<TextView>(R.id.tvDateTime).text = DateUtils.formatDateTime(entry.timestamp)
                    findViewById<TextView>(R.id.tvNotes).text = entry.notes.ifEmpty { "No notes" }

                    // Setup map
                    val mapView = findViewById<MapView>(R.id.mapView)
                    val location = GeoPoint(entry.latitude, entry.longitude)
                    // Only center map if it's the first load or significant change to avoid jarring jumps
                    if (mapView.zoomLevelDouble < 15.0) {
                        mapView.controller.setZoom(15.0)
                        mapView.controller.setCenter(location)
                    }

                    // Map Marker
                    val marker = Marker(mapView).apply {
                        position = location
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = entry.title

                        val drawable = ContextCompat.getDrawable(this@Activity_EntryDetail, R.drawable.ic_pin)?.mutate()
                        drawable?.setTint(ContextCompat.getColor(this@Activity_EntryDetail, R.color.primary))
                        icon = drawable
                    }
                    mapView.overlays.clear() // Clear old markers to avoid duplicates
                    mapView.overlays.add(marker)
                    mapView.invalidate()

                    // Photos - Always update the photo grid
                    photoAdapter.submitList(entry.photos.toList())

                    if (entry.photos.isNotEmpty()) {
                        findViewById<TextView>(R.id.tvPhotosLabel).visibility = View.VISIBLE
                        findViewById<RecyclerView>(R.id.rvPhotos).visibility = View.VISIBLE
                    } else {
                        findViewById<TextView>(R.id.tvPhotosLabel).visibility = View.GONE
                        findViewById<RecyclerView>(R.id.rvPhotos).visibility = View.GONE
                    }

                    // Audio
                    if (entry.audioUri != null) {
                        findViewById<TextView>(R.id.tvAudioLabel).visibility = View.VISIBLE
                        findViewById<View>(R.id.layoutAudio).visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvAudioFile).text = "Audio File (Auto-playing...)"

                        // Only play if not already playing to prevent looping on state updates
                        if (!AudioPlayerUtils.isPlaying()) {
                            AudioPlayerUtils.playAudio(this@Activity_EntryDetail, entry.audioUri!!)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioPlayerUtils.stopAudio()
    }

    companion object {
        private const val EDIT_ENTRY_REQUEST_CODE = 1001
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Reload the entry after editing
            entryId?.let { viewModel.loadEntry(it) }
        }
    }
}