package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.PreferenceDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityTaskSportsBinding
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskSportsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskSportsBinding
    private var accountId: Long = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityTaskSportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        val prefs=getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)
        accountId=prefs.getLong("id",-1L)

        binding.btnAdd.setOnClickListener {
            val selectedPreference = getselectedPreference()

            if (selectedPreference.isEmpty()) {
                showToast("Please select at least one preference")
                return@setOnClickListener
            }
            setupCardClickListeners()
            disableButton()
            sendpreferences()
        }
    }
    private  fun sendpreferences(){
        val preference = PreferenceDto(
            id=accountId,
            options = getselectedPreference()
        )
        lifecycleScope.launch {
            try{
                val response = api.createPreference(preference)
                if (response.isSuccessful) {
                    toastShort("Preferences Sent")
                    startActivity(Intent(this@TaskSportsActivity, LoginActivity::class.java))
                    finish()
                } else {
                   toastShort("Sent Failed")
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            }

        }
    }
    private fun getselectedPreference(): List<Int>{
        val list = mutableListOf<Int>()
        if (binding.cbplayerOut.isChecked) list.add(1)
        if (binding.cbplayerOutScore.isChecked) list.add(2)
        if(binding.cbplayerSummary.isChecked) list.add(3)
        return list
    }
    private fun setupCardClickListeners() {
        binding.CardPreference.setOnClickListener { binding.cbplayerOut.toggle() }
        binding.CardPreference.setOnClickListener { binding.cbplayerOutScore.toggle() }
        binding.CardPreference.setOnClickListener { binding.cbplayerSummary.toggle() }
    }
    private fun disableButton() {
        binding.btnAdd.isEnabled = false

    }

    private fun enableButton() {
        binding.btnAdd.isEnabled = true
    }

    private fun showToast(msg: String) {
        toastShort(msg)
    }
}