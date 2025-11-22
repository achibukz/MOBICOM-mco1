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
import android.widget.Spinner
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
import com.mobdeve.s18.mco.viewmodels.EditEntryViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Calendar

class Activity_EditEntry : AppCompatActivity() {

    private val viewModel: EditEntryViewModel by viewModels()
    private lateinit var photoGridAdapter: PhotoGridAdapter
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var audioPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationSearchLauncher: ActivityResultLauncher<Intent>
    private var locationMarker: Marker? = null
    private var entryId: String? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val EXTRA_ENTRY_ID = "entryId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_add_entry)

        entryId = intent.getStringExtra(EXTRA_ENTRY_ID)

        if (entryId == null) {
            Toast.makeText(this, "Error: No entry ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

        // Load the entry data
        viewModel.loadEntry(entryId!!)
    }

    private fun setupToolbar() {
        title = "Edit Entry"
    }

    private fun setupSpinner() {
        val moods = arrayOf("Happy", "Sad", "Excited", "Calm", "Anxious", "Grateful", "Nostalgic", "Adventurous")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, moods)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        findViewById<Spinner>(R.id.spinnerMood).adapter = adapter
    }

    private fun initEntryDateButton() {
        val btnDate = findViewById<Button>(R.id.btnEntryDate)

        btnDate.setOnClickListener {
            val currentTimestamp = viewModel.uiState.value.timestamp
            val cal = Calendar.getInstance()
            cal.timeInMillis = currentTimestamp

            val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_PinJournal)
            val dialog = DatePickerDialog(themedContext, { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth, 0, 0, 0)
                newCal.set(Calendar.MILLISECOND, 0)
                val ts = newCal.timeInMillis
                btnDate.text = DateUtils.formatDate(ts)
                viewModel.updateTimestamp(ts)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

            dialog.show()
        }
    }

    private fun setupMap() {
        val mapView = findViewById<MapView>(R.id.mapView)

        Configuration.getInstance().apply {
            userAgentValue = "PinJournal/1.0"
            osmdroidBasePath = cacheDir
            osmdroidTileCache = File(cacheDir, "osmdroid")
            load(this@Activity_EditEntry, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }

        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            setUseDataConnection(true)
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
        }

        val defaultLocation = LocationUtils.getDefaultLocation()
        val startPoint = GeoPoint(defaultLocation.first, defaultLocation.second)

        mapView.controller.apply {
            setZoom(15.0)
            setCenter(startPoint)
        }

        createLocationMarker(mapView, startPoint)

        mapView.postDelayed({
            mapView.invalidate()
        }, 100)
    }

    private fun createLocationMarker(mapView: MapView, position: GeoPoint) {
        locationMarker = Marker(mapView).apply {
            this.position = position
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Entry Location"
            isDraggable = true

            val drawable = ContextCompat.getDrawable(this@Activity_EditEntry, R.drawable.ic_pin)?.mutate()
            drawable?.setTint(ContextCompat.getColor(this@Activity_EditEntry, R.color.primary))
            icon = drawable

            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker?) {}

                override fun onMarkerDragEnd(marker: Marker?) {
                    marker?.position?.let { newPosition ->
                        // Update location with address "Custom Location" when manually dragged
                        viewModel.updateLocation(newPosition.latitude, newPosition.longitude, "Custom Location")
                        Toast.makeText(this@Activity_EditEntry,
                            "Location updated to: ${String.format("%.4f", newPosition.latitude)}, ${String.format("%.4f", newPosition.longitude)}",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onMarkerDragStart(marker: Marker?) {
                    Toast.makeText(this@Activity_EditEntry, "Drag to set location", Toast.LENGTH_SHORT).show()
                }
            })

            setOnMarkerClickListener { marker, mapView ->
                Toast.makeText(this@Activity_EditEntry, "Tap and drag to move pin", Toast.LENGTH_SHORT).show()
                true
            }
        }

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
            layoutManager = GridLayoutManager(this@Activity_EditEntry, 3)
        }
    }

    private fun setupMediaPickers() {
        imagePickerLauncher = SafUtils.createImagePickerLauncher(this) { uris ->
            if (uris.isNotEmpty()) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                for (uri in uris) {
                    try {
                        contentResolver.takePersistableUriPermission(uri, takeFlags)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                viewModel.addPhotos(uris)
            }
        }

        audioPickerLauncher = SafUtils.createAudioPickerLauncher(this) { uri ->
            uri?.let {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val filename = getFileNameFromUri(it)
                viewModel.setAudioFile(it, filename)
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

        findViewById<TextInputEditText>(R.id.etLocationSearch).setOnClickListener {
            launchLocationSearch()
        }

        findViewById<TextInputEditText>(R.id.etLocationSearch).setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                launchLocationSearch()
            }
        }

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
        findViewById<TextInputEditText>(R.id.etLocationSearch).clearFocus()
    }

    private fun updateAudioButtonState(hasAudio: Boolean, filename: String? = null) {
        val btnAddMusic = findViewById<Button>(R.id.btnAddMusic)
        if (hasAudio && filename != null) {
            btnAddMusic.text = "🎵 $filename"
        } else {
            btnAddMusic.setText(R.string.add_music)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "audio_file.mp3"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun useCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }

        val location = LocationUtils.getCurrentLocation(this)
        if (location != null) {
            moveMapToLocation(location.latitude, location.longitude, "Current Location")
            Toast.makeText(this, "Location set to current position", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveMapToLocation(latitude: Double, longitude: Double, name: String) {
        val mapView = findViewById<MapView>(R.id.mapView)
        val newPosition = GeoPoint(latitude, longitude)

        mapView.controller.animateTo(newPosition)
        mapView.controller.setZoom(15.0)

        locationMarker?.position = newPosition
        locationMarker?.title = name
        mapView.invalidate()

        // Update ViewModel with address
        viewModel.updateLocation(latitude, longitude, name)
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEntry() {
        val title = findViewById<TextInputEditText>(R.id.etTitle).text.toString()
        val notes = findViewById<TextInputEditText>(R.id.etNotes).text.toString()
        val mood = findViewById<Spinner>(R.id.spinnerMood).selectedItem.toString()

        viewModel.updateTitle(title)
        viewModel.updateNotes(notes)
        viewModel.updateMood(mood)

        if (viewModel.updateEntry()) {
            Toast.makeText(this, "Entry updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.isSaved) {
                    Toast.makeText(this@Activity_EditEntry, "Entry updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                    return@collect
                }

                state.errorMessage?.let { error ->
                    Toast.makeText(this@Activity_EditEntry, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                // Pre-populate fields when entry is loaded
                if (!state.isLoading && state.title.isNotEmpty()) {
                    findViewById<TextInputEditText>(R.id.etTitle).setText(state.title)
                    findViewById<TextInputEditText>(R.id.etNotes).setText(state.notes)

                    val moods = arrayOf("Happy", "Sad", "Excited", "Calm", "Anxious", "Grateful", "Nostalgic", "Adventurous")
                    val moodIndex = moods.indexOf(state.mood)
                    if (moodIndex >= 0) {
                        findViewById<Spinner>(R.id.spinnerMood).setSelection(moodIndex)
                    }

                    findViewById<Button>(R.id.btnEntryDate).text = DateUtils.formatDate(state.timestamp)

                    // Update map location
                    if (state.latitude != 0.0 && state.longitude != 0.0) {
                        val mapView = findViewById<MapView>(R.id.mapView)
                        val location = GeoPoint(state.latitude, state.longitude)
                        mapView.controller.setCenter(location)
                        mapView.controller.setZoom(15.0)
                        locationMarker?.position = location
                        mapView.invalidate()
                    }

                    // Update photos
                    photoGridAdapter.submitList(state.photos)

                    // Update audio
                    state.audioFilename?.let { filename ->
                        updateAudioButtonState(true, filename)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<MapView>(R.id.mapView).onResume()
    }

    override fun onPause() {
        super.onPause()
        findViewById<MapView>(R.id.mapView).onPause()
    }
}
