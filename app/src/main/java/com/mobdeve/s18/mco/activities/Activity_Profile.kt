package com.mobdeve.s18.mco.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

class Activity_Profile : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setupToolbar()
        setupUI()
        observeViewModel()
    }

    private fun setupToolbar() {
        title = "Profile"
    }

    private fun setupUI() {
        findViewById<MaterialButton>(R.id.btnUpdateProfile).setOnClickListener {
            updateProfile()
        }

        findViewById<MaterialButton>(R.id.btnSignOut).setOnClickListener {
            viewModel.signOut()
        }
    }

    private fun updateProfile() {
        val username = findViewById<TextInputEditText>(R.id.etUsername).text.toString().trim()
        val password = findViewById<TextInputEditText>(R.id.etPassword).text.toString().trim()

        viewModel.updateProfile(username, password)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Populate fields with current user data
                if (!state.isLoading && state.user != null) {
                    findViewById<TextInputEditText>(R.id.etUsername).setText(state.username)
                }

                // Handle loading state
                findViewById<MaterialButton>(R.id.btnUpdateProfile).isEnabled = !state.isLoading

                // Handle errors
                state.errorMessage?.let { error ->
                    Toast.makeText(this@Activity_Profile, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                // Handle update success
                if (state.isUpdateSuccess) {
                    Toast.makeText(this@Activity_Profile, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.clearUpdateSuccess()
                }

                // Handle sign out
                if (state.isSignedOut) {
                    val intent = Intent(this@Activity_Profile, Activity_SignIn::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
