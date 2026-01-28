package com.example.fypproject.Activity

import android.R.attr.id
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.UpdateAccountRequest
import com.example.fypproject.Network.ApiClient
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityUpdateAccountBinding
import kotlinx.coroutines.launch
import kotlin.toString

class UpdateAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateAccountBinding
    private var accountId: Long=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getLongExtra("id", -1L)
        val name = intent.getStringExtra("name")
        val username = intent.getStringExtra("username")
        val role = intent.getStringExtra("role")


        accountId = id
        binding.editTextName.setText(name)
        binding.editTextAridNo.setText(username)
        binding.editTextRole.setText(role)
        if (accountId == -1L) {
            toastShort("Account not found")
            finish()
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.buttonUpdate.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val username = binding.editTextAridNo.text.toString().trim()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()
            val role = binding.editTextRole.text.toString()
            if (name.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || role.isEmpty()) {
                toastShort("All fields are required")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                toastShort("Passwords do not match")
                return@setOnClickListener
            }
            updateAccount(accountId, name, username, password, role)
        }
    }

    private fun updateAccount(id: Long, name: String, username: String, password: String?, role: String?) {
        val request = UpdateAccountRequest(
            name = name,
            username = username,
            password = password,
            role = role
        )
        lifecycleScope.launch {
            setLoading(true)
            try {
                ApiClient.api.updateAccount(id, request)
                toastShort("Account updated successfully")
                finish()
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonUpdate.isEnabled = !isLoading
        binding.btnBack.isEnabled = !isLoading
    }
}
