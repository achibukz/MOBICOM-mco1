package com.mobdeve.s18.mco.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobdeve.s18.mco.databinding.ActivitySignInBinding
import com.mobdeve.s18.mco.viewmodels.SignInViewModel
import kotlinx.coroutines.launch

class Activity_SignIn : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private val viewModel: SignInViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnSignIn.setOnClickListener {
            signIn()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, Activity_SignUp::class.java))
        }
    }

    private fun signIn() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        viewModel.signIn(username, password)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.btnSignIn.isEnabled = !state.isLoading

                state.errorMessage?.let { error ->
                    Toast.makeText(this@Activity_SignIn, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                if (state.isSignedIn) {
                    val intent = Intent(this@Activity_SignIn, Activity_Home::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
