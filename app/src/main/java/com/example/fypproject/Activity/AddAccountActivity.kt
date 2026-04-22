package com.example.fypproject.Activity

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.CreateAccountRequest
import com.example.fypproject.Network.ApiClient
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityAddAccountBinding
import kotlinx.coroutines.launch

class AddAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddAccountBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityAddAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnAdd.setOnClickListener {
            val name=binding.editTextName.text.toString().trim()
            val username=binding.editTextAridNo.text.toString().trim()
            val password=binding.editTextPassword.text.toString().trim()
            val confirmPassword=binding.editTextConfirmPassword.text.toString().trim()
            if(name.isEmpty()||username.isEmpty()||password.isEmpty()||confirmPassword.isEmpty()){
                toastShort("All fields are required")
                return@setOnClickListener
            }
            if(password!=confirmPassword){
                toastShort("Passwords do not match")
                return@setOnClickListener
            }
            createAccount(name,username,password)
        }
    }
    private fun createAccount(name: String, username: String, password: String){
        val request= CreateAccountRequest(name, username, password)
        lifecycleScope.launch {
            showLoading(true)
            binding.btnAdd.isEnabled = false
            binding.btnBack.isEnabled = false
            try{
                val response= ApiClient.api.createAccount(request)
                toastShort("Account created successfully")
                finish()
            }catch (e: Exception){
                toastLong(NetworkUi.userMessage(e))
            } finally {
                showLoading(false)
                binding.btnAdd.isEnabled = true
                binding.btnBack.isEnabled = true
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAdd.isEnabled = !isLoading
        binding.btnBack.isEnabled = !isLoading
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        // Add empty state logic here if needed
        // Example: if (someDataList.isEmpty()) { showEmptyStateView() }
    }
}