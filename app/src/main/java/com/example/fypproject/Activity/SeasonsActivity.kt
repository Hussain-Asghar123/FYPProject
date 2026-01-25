package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.SeasonAdapter
import com.example.fypproject.DTO.CreateSeasonRequest
import com.example.fypproject.DTO.SeasonResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivitySeasonsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SeasonsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeasonsBinding
    private lateinit var adapter: SeasonAdapter

    private val fullList = mutableListOf<SeasonResponse>()
    private val filterList = mutableListOf<SeasonResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeasonsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecycler()
        checkAdminButton()
        fetchSeasons()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun checkAdminButton() {
        val role = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("role", "") ?: ""

        binding.btnAdd.visibility =
            if (role.equals("ADMIN", true)) View.VISIBLE else View.GONE

        binding.btnAdd.setOnClickListener { showAddSeasonDialog() }
    }

    private fun showAddSeasonDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_season, null)
        val etSeasonName = dialogView.findViewById<EditText>(R.id.etSeasonName)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveSeason)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val name = etSeasonName.text.toString().trim()
            if (name.isEmpty()) {
                etSeasonName.error = "Name is required"
                return@setOnClickListener
            }
            createSeason(name, dialog)
        }

        dialog.show()
    }

    private fun createSeason(name: String, dialog: androidx.appcompat.app.AlertDialog) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                val username = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    .getString("username", "") ?: ""

                val request = CreateSeasonRequest(
                    name = name,
                    username = username
                )

                val response = api.createSeason(request)
                toastShort("Season created successfully")
                dialog.dismiss()
                fetchSeasons()
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setupRecycler() {
        adapter = SeasonAdapter(filterList) { season ->
            val intent = Intent(this, TournamentDetailActivity::class.java)
            intent.putExtra("seasonId", season.id)
            intent.putExtra("seasonName", season.name)
            startActivity(intent)
        }

        binding.seasonRecycler.layoutManager = LinearLayoutManager(this)
        binding.seasonRecycler.adapter = adapter
    }

    private fun fetchSeasons() {
        lifecycleScope.launch {
            setLoading(true)
            try {
                val response = api.getonlynames()
                if (response.isSuccessful && response.body() != null) {

                    fullList.clear()
                    fullList.addAll(
                        response.body()!!.map {
                            SeasonResponse(it.id, it.name)
                        }
                    )

                    filterList.clear()
                    filterList.addAll(fullList)
                    adapter.notifyDataSetChanged()

                } else {
                    Log.e("SeasonsActivity", "Error: ${response.code()}")
                    toastLong(NetworkUi.userMessage(response, "Failed to load seasons"))
                }
            } catch (e: Exception) {
                Log.e("SeasonsActivity", "Fetch failed", e)
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAdd.isEnabled = !isLoading
    }

    override fun onResume() {
        super.onResume()
        fetchSeasons()
    }
}
