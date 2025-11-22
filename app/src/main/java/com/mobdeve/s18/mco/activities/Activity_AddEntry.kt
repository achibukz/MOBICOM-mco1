package com.mobdeve.s18.mco.activities

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.adapters.PhotoGridAdapter
import com.mobdeve.s18.mco.utils.LocationUtils
import com.mobdeve.s18.mco.utils.SafUtils
import com.mobdeve.s18.mco.utils.DateUtils
import com.mobdeve.s18.mco.viewmodels.AddEntryViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Calendar

class Activity_AddEntry : AppCompatActivity() {

    private val viewModel: AddEntryViewModel by viewModels()
    private lateinit var photoGridAdapter: PhotoGridAdapter
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var audioPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationSearchLauncher: ActivityResultLauncher<Intent>
    private var locationMarker: Marker? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_add_entry)

        setupToolbar()
        setupSpinner()
        setupMap()
        setupPhotoGrid()
        setupMediaPickers()
        setupLocationSearchLauncher()
        setupUI()
        initEntryDateButton()
        setupBottomNavigation()
        observeViewModel()

        requestLocationPermission()
    }

    private fun setupToolbar() {
        // Simple toolbar setup - we'll use a basic approach
        title = "Add Entry"
    }

    private fun setupSpinner() {
        val moods = arrayOf("Happy", "Sad", "Excited", "Calm", "Anxious", "Grateful", "Nostalgic", "Adventurous")
        // Use themed item layouts so colors follow the app theme
        val adapter = ArrayAdapter(this, R.layout.spinner_item, moods)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        findViewById<Spinner>(R.id.spinnerMood).adapter = adapter
    }

    // Helper to format and set the entry date button text
    private fun initEntryDateButton() {
        val btnDate = findViewById<Button>(R.id.btnEntryDate)
        // initialize with today's date
        btnDate.text = DateUtils.formatDate(DateUtils.getCurrentTimestamp())

        btnDate.setOnClickListener {
            val now = Calendar.getInstance()
            // Use a themed context so the DatePicker matches the app theme (dark/light)
            val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_PinJournal)
            val dialog = DatePickerDialog(themedContext, { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val ts = cal.timeInMillis
                // update button text and viewmodel
                btnDate.text = DateUtils.formatDate(ts)
                viewModel.updateTimestamp(ts)
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))

            dialog.show()
        }
    }

    private fun setupMap() {
        val mapView = findViewById<MapView>(R.id.mapView)

        // Configure OSMDroid properly with better settings
        Configuration.getInstance().apply {
            userAgentValue = "PinJournal/1.0"
            osmdroidBasePath = cacheDir
            osmdroidTileCache = File(cacheDir, "osmdroid")
            load(this@Activity_AddEntry, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }

        // Enhanced map setup
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true) // Enable built-in zoom controls
            setUseDataConnection(true)

            // Set zoom limits
            minZoomLevel = 3.0
            maxZoomLevel = 20.0

            // Enable better performance
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            // Enable map rotation
            setMultiTouchControls(true)

            // Set tiles scaling
            isTilesScaledToDpi = true
        }

        // Set default location (Manila)
        val defaultLocation = LocationUtils.getDefaultLocation()
        val startPoint = GeoPoint(defaultLocation.first, defaultLocation.second)

        // Set initial view parameters with better zoom handling
        mapView.controller.apply {
            setZoom(15.0)
            setCenter(startPoint)
        }

        // Create and configure marker with better settings
        createLocationMarker(mapView, startPoint)

        // Force initial refresh
        mapView.postDelayed({
            mapView.invalidate()
        }, 100)

        // Update ViewModel with default location
        viewModel.updateLocation(startPoint.latitude, startPoint.longitude)
    }

    private fun createLocationMarker(mapView: MapView, position: GeoPoint) {
        locationMarker = Marker(mapView).apply {
            this.position = position
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Entry Location"
            isDraggable = true

            // Set marker icon to make it more visible
            val drawable = ContextCompat.getDrawable(this@Activity_AddEntry, R.drawable.ic_pin)?.mutate()
            drawable?.setTint(ContextCompat.getColor(this@Activity_AddEntry, R.color.primary))

            // --- THIS IS THE FIX ---
            // Set the drawable directly. No bitmap conversion is needed.
            icon = drawable
            // Add drag listener to update location when marker is moved
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker?) {
                    // Optional: Show real-time coordinates while dragging
                }

                override fun onMarkerDragEnd(marker: Marker?) {
                    marker?.position?.let { newPosition ->
                        viewModel.updateLocation(newPosition.latitude, newPosition.longitude)
                        Toast.makeText(this@Activity_AddEntry,
                            "Location updated to: ${String.format("%.4f", newPosition.latitude)}, ${String.format("%.4f", newPosition.longitude)}",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onMarkerDragStart(marker: Marker?) {
                    Toast.makeText(this@Activity_AddEntry, "Drag to set location", Toast.LENGTH_SHORT).show()
                }
            })

            // Add click listener for marker
            setOnMarkerClickListener { marker, mapView ->
                Toast.makeText(this@Activity_AddEntry, "Tap and drag to move pin", Toast.LENGTH_SHORT).show()
                true
            }
        }

        // Clear existing overlays and add the marker
        mapView.overlays.clear()
        mapView.overlays.add(locationMarker)
        mapView.invalidate()
    }

    private fun setupPhotoGrid() {
        photoGridAdapter = PhotoGridAdapter { photo ->
            viewModel.removePhoto(photo)
        }

        findViewById<RecyclerView>(R.id.rvPhotos).apply {
            adapter = photoGridAdapter
            layoutManager = GridLayoutManager(this@Activity_AddEntry, 3)
        }
    }

    private fun setupMediaPickers() {
        // 1. IMAGE PICKER
        imagePickerLauncher = SafUtils.createImagePickerLauncher(this) { uris ->
            if (uris.isNotEmpty()) {
                // --- NEW CODE START: Take Persistable Permissions ---
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION

                for (uri in uris) {
                    try {
                        contentResolver.takePersistableUriPermission(uri, takeFlags)
                    } catch (e: Exception) {
                        // Some providers might not support this, catch to prevent crash
                        e.printStackTrace()
                    }
                }
                // --- NEW CODE END ---

                viewModel.addPhotos(uris)
            }
        }

        // 2. AUDIO PICKER
        audioPickerLauncher = SafUtils.createAudioPickerLauncher(this) { uri ->
            uri?.let {
                // --- NEW CODE START: Take Permission for Audio too ---
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // --- NEW CODE END ---

                val filename = getFileNameFromUri(it)
                viewModel.setAudioFile(it, filename)
                // Show feedback that audio was added
                Toast.makeText(this, getString(R.string.audio_added_fmt, filename), Toast.LENGTH_SHORT).show()
                updateAudioButtonState(true, filename)
            }
        }
    }

    private fun setupLocationSearchLauncher() {
        locationSearchLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val name = data?.getStringExtra(Activity_LocationSearch.EXTRA_LOCATION_NAME) ?: "Unknown"
                val lat = data?.getDoubleExtra(Activity_LocationSearch.EXTRA_LOCATION_LAT, 0.0) ?: 0.0
                val lon = data?.getDoubleExtra(Activity_LocationSearch.EXTRA_LOCATION_LON, 0.0) ?: 0.0
                val displayName = data?.getStringExtra(Activity_LocationSearch.EXTRA_LOCATION_DISPLAY) ?: name

                moveMapToLocation(lat, lon, name)
                findViewById<TextInputEditText>(R.id.etLocationSearch).setText(displayName)
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_add

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
                R.id.nav_journal -> {
                    startActivity(Intent(this, Activity_Journal::class.java))
                    true
                }
                R.id.nav_add -> true
                else -> false
            }
        }
    }

    private fun setupUI() {
        findViewById<Button>(R.id.btnCurrentLocation).setOnClickListener {
            useCurrentLocation()
        }

        // Launch location search activity instead of inline search
        findViewById<TextInputEditText>(R.id.etLocationSearch).setOnClickListener {
            launchLocationSearch()
        }

        findViewById<TextInputEditText>(R.id.etLocationSearch).setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                launchLocationSearch()
            }
        }

        // Add search icon click listener
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilLocationSearch).setEndIconOnClickListener {
            launchLocationSearch()
        }

        findViewById<Button>(R.id.btnAddPhotos).setOnClickListener {
            imagePickerLauncher.launch(SafUtils.createImagePickerIntent())
        }

        findViewById<Button>(R.id.btnAddMusic).setOnClickListener {
            audioPickerLauncher.launch(SafUtils.createAudioPickerIntent())
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveEntry()
        }
    }

    private fun launchLocationSearch() {
        val intent = Intent(this, Activity_LocationSearch::class.java)
        locationSearchLauncher.launch(intent)

        // Clear focus from the text field to prevent keyboard from showing
        findViewById<TextInputEditText>(R.id.etLocationSearch).clearFocus()
    }

    private fun updateAudioButtonState(hasAudio: Boolean, filename: String? = null) {
        val btnAddMusic = findViewById<Button>(R.id.btnAddMusic)
        val layoutAudio = findViewById<View>(R.id.layoutAudio)
        val tvAudioFile = findViewById<TextView>(R.id.tvAudioFile)
        val btnRemoveAudio = findViewById<ImageButton>(R.id.btnRemoveAudio)

        if (hasAudio && filename != null) {
            // Show the audio layout with file info
            layoutAudio.visibility = View.VISIBLE

            // Clean up the filename display - remove extensions and long paths
            val cleanFileName = filename
                .substringAfterLast("/") // Remove path
                .substringBeforeLast(".") // Remove extension
                .let { name ->
                    if (name.length > 25) {
                        "${name.take(22)}..." // Truncate long names
                    } else {
                        name
                    }
                }

            tvAudioFile.text = cleanFileName
            btnAddMusic.text = getString(R.string.change_music)

            // Set up remove audio button
            btnRemoveAudio.setOnClickListener {
                viewModel.setAudioFile(null, null)
                Toast.makeText(this, getString(R.string.audio_removed), Toast.LENGTH_SHORT).show()
            }
        } else {
            // Hide the audio layout
            layoutAudio.visibility = View.GONE
            btnAddMusic.text = getString(R.string.add_music)
        }
    }

    private fun saveEntry() {
        // Update ViewModel with current form values
        val title = findViewById<TextInputEditText>(R.id.etTitle).text.toString()
        val notes = findViewById<TextInputEditText>(R.id.etNotes).text.toString()
        val mood = findViewById<Spinner>(R.id.spinnerMood).selectedItem.toString()

        viewModel.updateTitle(title)
        viewModel.updateNotes(notes)
        viewModel.updateMood(mood)

        if (viewModel.saveEntry()) {
            Toast.makeText(this, "Entry saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun useCurrentLocation() {
        if (LocationUtils.hasLocationPermission(this)) {
            val location = LocationUtils.getCurrentLocation(this)
            location?.let {
                val mapView = findViewById<MapView>(R.id.mapView)
                val geoPoint = GeoPoint(it.latitude, it.longitude)
                mapView.controller.setCenter(geoPoint)
                locationMarker?.position = geoPoint
                viewModel.updateLocation(it.latitude, it.longitude)
                mapView.invalidate()
            } ?: run {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        // Try to get the actual filename from the URI
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                val fileName = cursor.getString(nameIndex)
                if (!fileName.isNullOrEmpty()) {
                    return fileName
                }
            }
        }

        // Fallback to extracting filename from URI path
        val path = uri.path
        return path?.substringAfterLast("/") ?: "Audio File"
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update photo grid
                photoGridAdapter.submitList(state.photos)
                findViewById<RecyclerView>(R.id.rvPhotos).visibility = if (state.photos.isNotEmpty()) View.VISIBLE else View.GONE

                // Update audio button state
                updateAudioButtonState(state.audioUri != null, state.audioFilename)

                // Handle errors
                state.errorMessage?.let { error ->
                    Toast.makeText(this@Activity_AddEntry, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                // Handle save success
                if (state.isSaved) {
                    finish()
                }
            }
        }
    }

    private fun moveMapToLocation(latitude: Double, longitude: Double, locationName: String) {
        val mapView = findViewById<MapView>(R.id.mapView)
        val geoPoint = GeoPoint(latitude, longitude)

        // Clear existing overlays before adding new ones
        mapView.overlays.clear()

        // Update the marker position
        locationMarker?.position = geoPoint
        locationMarker?.title = locationName

        // Re-add the marker to overlays
        locationMarker?.let { marker ->
            mapView.overlays.add(marker)
        }

        // Animate to new location with proper timing
        mapView.controller.animateTo(geoPoint)
        mapView.controller.setZoom(15.0)

        // Force refresh the map view
        mapView.invalidate()

        // Post a delayed refresh to ensure proper rendering
        mapView.post {
            mapView.invalidate()
        }

        // Update ViewModel
        viewModel.updateLocation(latitude, longitude, locationName)

        Toast.makeText(this, "Location set to: $locationName", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    useCurrentLocation()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val mapView = findViewById<MapView>(R.id.mapView)
        mapView.onResume()

        // Refresh map view when resuming
        mapView.post {
            mapView.invalidate()
        }

        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_add
    }

    override fun onPause() {
        super.onPause()
        findViewById<MapView>(R.id.mapView).onPause()
    }
}
