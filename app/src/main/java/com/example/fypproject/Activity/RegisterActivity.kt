package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.CreateAccountRequest
import com.example.fypproject.Network.ApiClient
import com.example.fypproject.R
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityRegisterBinding
import com.example.fypproject.databinding.ActivityTaskSportsBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityRegisterBinding.inflate(layoutInflater)
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
    private fun createAccount(name:String,username:String,password:String){
        val request= CreateAccountRequest(name,username,password)
        lifecycleScope.launch {
            showLoading(true)
            binding.btnAdd.isEnabled = false
            binding.btnBack.isEnabled = false
            try{
                val response= ApiClient.api.createAccount(request)
                toastShort("Account created successfully")
                val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                prefs.edit {
                    putLong("id",         response.id)
                }
                startActivity(Intent(this@RegisterActivity, TaskSportsActivity::class.java))
                finish()
            }catch (e: Exception){
            } finally {
                showLoading(false)
                binding.btnAdd.isEnabled = true
                binding.btnBack.isEnabled = true
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

}