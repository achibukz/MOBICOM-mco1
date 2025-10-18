package com.mobdeve.s18.mco.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.models.JournalEntry
import com.mobdeve.s18.mco.utils.DateUtils
import com.mobdeve.s18.mco.viewmodels.MapViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

class Activity_Map : AppCompatActivity() {

    private val viewModel: MapViewModel by viewModels()
    private val entryMarkers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enhanced OSMDroid configuration
        Configuration.getInstance().apply {
            load(this@Activity_Map, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "PinJournal/1.0"
            osmdroidBasePath = cacheDir
            osmdroidTileCache = File(cacheDir, "osmdroid")
        }

        setContentView(R.layout.activity_map)

        setupToolbar()
        setupMap()
        setupBottomNavigation()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        val mapView = findViewById<MapView>(R.id.mapView)
        mapView.onResume()

        // Refresh map view when resuming
        mapView.post {
            mapView.invalidate()
        }

        viewModel.refreshEntries()
        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_map
    }

    override fun onPause() {
        super.onPause()
        findViewById<MapView>(R.id.mapView).onPause()
    }

    private fun setupToolbar() {
        title = "Map"
    }

    private fun setupMap() {
        val mapView = findViewById<MapView>(R.id.mapView)

        // Enhanced map setup with zoom controls and better configuration
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false) // Disable built-in controls since we have custom ones
            setUseDataConnection(true)

            // Set zoom limits
            minZoomLevel = 3.0
            maxZoomLevel = 20.0

            // Enable better performance
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

            // Enable map rotation and better touch controls
            setMultiTouchControls(true)

            // Set tiles scaling for better display
            isTilesScaledToDpi = true
        }

        // Set default view to Manila with better zoom level
        val startPoint = GeoPoint(14.5995, 120.9842)
        mapView.controller.apply {
            setZoom(12.0)
            setCenter(startPoint)
        }

        // Setup custom zoom controls
        setupZoomControls(mapView)

        // Force initial refresh with delay
        mapView.postDelayed({
            mapView.invalidate()
        }, 100)
    }

    private fun setupZoomControls(mapView: MapView) {
        // Zoom In Button
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabZoomIn).setOnClickListener {
            val currentZoom = mapView.zoomLevelDouble
            if (currentZoom < mapView.maxZoomLevel) {
                mapView.controller.setZoom(currentZoom + 1)
            }
        }

        // Zoom Out Button
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabZoomOut).setOnClickListener {
            val currentZoom = mapView.zoomLevelDouble
            if (currentZoom > mapView.minZoomLevel) {
                mapView.controller.setZoom(currentZoom - 1)
            }
        }

        // My Location Button
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabMyLocation).setOnClickListener {
            // Center map on Manila as default location
            val defaultLocation = GeoPoint(14.5995, 120.9842)
            mapView.controller.animateTo(defaultLocation)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_map

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Activity_Home::class.java))
                    true
                }
                R.id.nav_map -> true
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
                if (!state.isLoading) {
                    updateMapMarkers(state.entries)
                }
            }
        }
    }

    private fun updateMapMarkers(entries: List<JournalEntry>) {
        val mapView = findViewById<MapView>(R.id.mapView)

        // Clear existing markers properly
        entryMarkers.forEach { marker ->
            mapView.overlays.remove(marker)
        }
        entryMarkers.clear()

        // Add new markers with enhanced configuration
        entries.forEach { entry ->
            val marker = createEntryMarker(mapView, entry)
            entryMarkers.add(marker)
            mapView.overlays.add(marker)
        }

        // Force refresh the map view
        mapView.invalidate()

        // Post delayed refresh to ensure proper rendering
        mapView.post {
            mapView.invalidate()
        }

        // Center map on entries if available
        if (entries.isNotEmpty()) {
            try {
                val bounds = calculateBounds(entries)
                // Add some padding and animate to bounds
                mapView.post {
                    mapView.zoomToBoundingBox(bounds, true, 50)
                }
            } catch (e: Exception) {
                // Fallback to showing all entries at a reasonable zoom
                val centerLat = entries.map { it.latitude }.average()
                val centerLon = entries.map { it.longitude }.average()
                mapView.controller.animateTo(GeoPoint(centerLat, centerLon))
                mapView.controller.setZoom(10.0)
            }
        }
    }

    private fun createEntryMarker(mapView: MapView, entry: JournalEntry): Marker {
        return Marker(mapView).apply {
            position = GeoPoint(entry.latitude, entry.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = entry.title
            snippet = DateUtils.formatDateTime(entry.timestamp)

            // Get drawable, apply tint, and set it directly
            val drawable = ContextCompat.getDrawable(this@Activity_Map, R.drawable.ic_pin)?.mutate()
            drawable?.setTint(ContextCompat.getColor(this@Activity_Map, R.color.primary))

            // --- THIS IS THE FIX ---
            // Set the drawable directly. No bitmap conversion is needed.
            icon = drawable
            // --- END OF FIX ---

            setOnMarkerClickListener { _, _ ->
                showEntryPreview(entry)
                true
            }
        }
    }


    private fun calculateBounds(entries: List<JournalEntry>): BoundingBox {
        val latitudes = entries.map { it.latitude }
        val longitudes = entries.map { it.longitude }

        val minLat = latitudes.minOrNull() ?: 0.0
        val maxLat = latitudes.maxOrNull() ?: 0.0
        val minLon = longitudes.minOrNull() ?: 0.0
        val maxLon = longitudes.maxOrNull() ?: 0.0

        // Add some padding to the bounds
        val latPadding = (maxLat - minLat) * 0.1
        val lonPadding = (maxLon - minLon) * 0.1

        return BoundingBox(
            maxLat + latPadding,
            maxLon + lonPadding,
            minLat - latPadding,
            minLon - lonPadding
        )
    }

    private fun showEntryPreview(entry: JournalEntry) {
        // Store audioUri in a local variable to avoid smart cast issues
        val audioUri = entry.audioUri
        if (audioUri != null) {
            // Automatically play audio if available when entry is opened from map
            com.mobdeve.s18.mco.utils.AudioPlayerUtils.playAudio(this, audioUri)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(entry.title)
            .setMessage("📍 ${entry.address ?: "Unknown location"}\n🕒 ${DateUtils.formatDateTime(entry.timestamp)}\n😊 ${entry.mood}\n\n${entry.notes}")
            .setPositiveButton("Open Entry") { _, _ ->
                val intent = Intent(this, Activity_EntryDetail::class.java)
                intent.putExtra("entryId", entry.id)
                startActivity(intent)
            }
            .setNegativeButton("Close") { _, _ ->
                // Stop audio when dialog is closed
                com.mobdeve.s18.mco.utils.AudioPlayerUtils.stopAudio()
            }
            .show()
    }
}
