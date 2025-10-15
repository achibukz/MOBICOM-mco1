package com.mobdeve.s18.mco.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobdeve.s18.mco.databinding.ActivitySignUpBinding
import com.mobdeve.s18.mco.viewmodels.SignUpViewModel
import kotlinx.coroutines.launch

class Activity_SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnSignUp.setOnClickListener {
            signUp()
        }

        binding.tvSignIn.setOnClickListener {
            finish() // Go back to sign in
        }
    }

    private fun signUp() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        viewModel.signUp(username, password, confirmPassword)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.btnSignUp.isEnabled = !state.isLoading

                state.errorMessage?.let { error ->
                    Toast.makeText(this@Activity_SignUp, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                if (state.isSignUpSuccess) {
                    Toast.makeText(this@Activity_SignUp, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to sign in
                }
            }
        }
    }
}
