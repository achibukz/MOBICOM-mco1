package com.mobdeve.s18.mco.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.mobdeve.s18.mco.R
import com.google.android.material.imageview.ShapeableImageView
import com.mobdeve.s18.mco.utils.SafUtils
import com.mobdeve.s18.mco.viewmodels.ProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class Activity_EditProfile : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnSave: Button
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMobile: EditText
    private lateinit var ivProfileImage: ShapeableImageView
    private lateinit var tvTitle: TextView
    private val viewModel: ProfileViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    // Activity-scoped coroutine scope (canceled in onDestroy)
    private val activityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + activityJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_page)

        // Initialize views
        initializeViews()
        setupListeners()
        observeViewModel()

        // Back button and image click handled in setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel()
    }

    // Image picker launcher
    private val imagePickerLauncher = SafUtils.createImagePickerLauncher(this) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUri = uris[0]
            // Display the selected image
            Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(ivProfileImage)
        }
    }

    private fun initializeViews() {
        // This function can be used to initialize views if needed
        btnBack = findViewById(R.id.btnProfileBack)
        btnSave = findViewById(R.id.btnProfileSave)
        etUsername = findViewById(R.id.etProfileUsername)
        etPassword = findViewById(R.id.etProfilePassword)
        etFirstName = findViewById(R.id.etProfileFirstName)
        etLastName = findViewById(R.id.etProfileLastName)
        etEmail = findViewById(R.id.etProfileEmail)
        etMobile = findViewById(R.id.etProfileMobile)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvTitle = findViewById(R.id.tvProfileTitle)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        // Launch system image picker when profile image is clicked
        ivProfileImage.setOnClickListener {
            imagePickerLauncher.launch(SafUtils.createImagePickerIntent())
        }

        // image click handled by setupListeners()
    }

    private fun saveProfile() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val mobile = etMobile.text.toString().trim()

        // Basic validation for profile fields
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate password if user provided one
        if (password.isNotEmpty() && password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Determine which fields to pass to unified updateProfile
        val usernameArg = if (username.isEmpty()) null else username
        val passwordArg = if (password.isEmpty()) null else password

        viewModel.updateProfile(
            username = usernameArg,
            password = passwordArg,
            firstName = firstName,
            lastName = lastName,
            email = email,
            mobile = mobile,
            profileImageUri = selectedImageUri?.toString()
        )
    }

    private fun observeViewModel() {
        uiScope.launch {
            viewModel.uiState.collect { state ->
                // Populate fields with current user data
                if (!state.isLoading && state.user != null) {
                    etUsername.setText(state.user.username)
                    // Note: Do not set password field (leave blank)
                    etFirstName.setText(state.user.firstName)
                    etLastName.setText(state.user.lastName)
                    etEmail.setText(state.user.email)
                    etMobile.setText(state.user.mobile)

                    // Load existing profile image if available
                    state.user.profileImageUri?.let { uriString ->
                        try {
                            val uri = Uri.parse(uriString)
                            Glide.with(this@Activity_EditProfile)
                                .load(uri)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(ivProfileImage)
                            selectedImageUri = uri
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Handle loading state
                btnSave.isEnabled = !state.isLoading

                // Handle errors
                state.errorMessage?.let { error ->
                    Toast.makeText(this@Activity_EditProfile, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                // Handle update success
                if (state.isUpdateSuccess) {
                    Toast.makeText(this@Activity_EditProfile, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.clearUpdateSuccess()
                    finish() // Go back to previous screen
                }
            }
        }
    }
}
