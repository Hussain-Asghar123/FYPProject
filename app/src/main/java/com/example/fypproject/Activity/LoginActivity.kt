package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.LoginRequest
import com.example.fypproject.DTO.LoginResponse
import com.example.fypproject.R
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding=ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLogin.setOnClickListener {
            val username=binding.etUserName.text.toString()
            val password=binding.etPassword.text.toString()

            if(username.isEmpty()||password.isEmpty()){
                toastShort("Username and password are required")
                return@setOnClickListener
            }

            setLoading(true)
            currentFocus?.let { view ->
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
            val request= LoginRequest(username, password)
            lifecycleScope.launch(){
                try{
                    val response: LoginResponse = RetrofitInstance.api.login(request)
                    toastShort("Login successful")
                    val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    sharedPreferences.edit{
                        putString("name", response.name)
                        putLong("id", response.id)
                        putLong("playerId", response.playerId)
                        putString("role", response.role)
                        putString("username", response.username)
                    }
                    checkEmptyState()
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                }catch(e: Exception){
                    toastLong(NetworkUi.userMessage(e))
                    Log.e("Login", "Login Failed", e)
                    checkEmptyState()
                } finally {
                    setLoading(false)
                }
            }



        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun checkEmptyState() {
        // Add empty state logic here if needed
        // Example: if (username.isEmpty() || password.isEmpty()) { showErrorMessage() }
    }
}