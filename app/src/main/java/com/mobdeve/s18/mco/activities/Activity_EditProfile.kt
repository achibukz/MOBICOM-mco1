package com.mobdeve.s18.mco.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.mco.R
import com.google.android.material.imageview.ShapeableImageView

class Activity_EditProfile : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnSave: Button
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMobile: EditText
    private lateinit var ivProfileImage: ShapeableImageView
    private lateinit var tvTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_page)

        // Initialize views
        btnBack = findViewById(R.id.btnProfileBack)
        btnSave = findViewById(R.id.btnProfileSave)
        etFirstName = findViewById(R.id.etProfileFirstName)
        etLastName = findViewById(R.id.etProfileLastName)
        etEmail = findViewById(R.id.etProfileEmail)
        etMobile = findViewById(R.id.etProfileMobile)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvTitle = findViewById(R.id.tvProfileTitle)

        // Example: Load current user info (replace with ViewModel or DB later)
        etFirstName.setText("John")
        etLastName.setText("Doe")
        etEmail.setText("john.doe@example.com")
        etMobile.setText("1234567890")

        // Back button closes the activity
        btnBack.setOnClickListener {
            finish()
        }

        // Save button
        btnSave.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val mobile = etMobile.text.toString().trim()

            // Basic validation
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || mobile.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Save to ViewModel, database, or SharedPreferences
            Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
        }

        // Optional: Click profile image to change (later)
        ivProfileImage.setOnClickListener {
            Toast.makeText(this, "Change profile picture feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
