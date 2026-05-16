package com.example.fypproject.Activity

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.fypproject.Adapter.StatsAdapter
import com.example.fypproject.DTO.PlayerDto1
import com.example.fypproject.DTO.PlayeraStatsDTO1
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.SportStatsConfig
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.databinding.ActivityCompareBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.collections.map

class CompareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompareBinding
    private val statsAdapter = StatsAdapter()

    private val playerList = mutableListOf<PlayerDto1>()

    private var activeSport = "cricket"
    private var stats1: PlayeraStatsDTO1? = null
    private var stats2: PlayeraStatsDTO1? = null
    private var stats1Job: Job? = null
    private var stats2Job: Job? = null

    private var selectedPlayer1: PlayerDto1? = null
    private var selectedPlayer2: PlayerDto1? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        buildSportPills()
        fetchPlayers()

        binding.btnBack.setOnClickListener { finish() }
    }

    // ─────────────────────────────────────────────────────────
    // SPORT PILLS
    // ─────────────────────────────────────────────────────────

    private fun buildSportPills() {
        binding.layoutSportPills.removeAllViews()

        SportStatsConfig.SPORTS.forEach { (key, label) ->
            val pill = android.widget.TextView(this).apply {
                text = label
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))

                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, dpToPx(8), 0) }
                layoutParams = params

                applyPillStyle(this, key == activeSport)

                setOnClickListener {
                    if (activeSport == key) return@setOnClickListener
                    activeSport = key
                    buildSportPills()          // Redraw pills
                    refreshStats()             // Re-fetch stats for new sport
                }
            }
            binding.layoutSportPills.addView(pill)
        }
    }

    private fun applyPillStyle(tv: android.widget.TextView, isSelected: Boolean) {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(20).toFloat()
            if (isSelected) {
                setColor(Color.parseColor("#E31212"))
                setStroke(dpToPx(2), Color.parseColor("#E31212"))
            } else {
                setColor(Color.WHITE)
                setStroke(dpToPx(2), Color.parseColor("#E5E7EB"))
            }
        }
        tv.background = bg
        tv.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#4B5563"))
    }

    // ─────────────────────────────────────────────────────────
    // FETCH PLAYERS
    // ─────────────────────────────────────────────────────────

    private fun fetchPlayers() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.getPlayers()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    playerList.clear()
                    playerList.addAll(response.body()!!)
                    setupPlayerSpinners()
                } else {
                    // Response code dekho
                    android.util.Log.e("CompareActivity", "Error code: ${response.code()} | body: ${response.errorBody()?.string()}")
                    toastLong("Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                // Exact exception dekho Logcat mein
                android.util.Log.e("CompareActivity", "Exception: ${e::class.simpleName} - ${e.message}", e)
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // PLAYER SPINNERS
    // ─────────────────────────────────────────────────────────

    private fun setupPlayerSpinners() {
        val names = mutableListOf("Select Player") + playerList.map{it.name}

        val adapter1 = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlayer1.adapter = adapter1

        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlayer2.adapter = adapter2

        binding.spinnerPlayer1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPlayer1 = if (position == 0) null else playerList[position - 1]
                bindPlayerCard(1, selectedPlayer1)
                fetchStats(1)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerPlayer2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPlayer2 = if (position == 0) null else playerList[position - 1]
                bindPlayerCard(2, selectedPlayer2)
                fetchStats(2)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // ─────────────────────────────────────────────────────────
    // PLAYER CARD
    // ─────────────────────────────────────────────────────────

    private fun bindPlayerCard(playerNum: Int, player: PlayerDto1?) {
        val cardView      = if (playerNum == 1) binding.cardPlayer1    else binding.cardPlayer2
        val ivPhoto       = if (playerNum == 1) binding.ivPlayer1Photo  else binding.ivPlayer2Photo
        val tvInitials    = if (playerNum == 1) binding.tvPlayer1Initials else binding.tvPlayer2Initials
        val tvName        = if (playerNum == 1) binding.tvPlayer1Name    else binding.tvPlayer2Name
        val tvJersey      = if (playerNum == 1) binding.tvPlayer1Jersey  else binding.tvPlayer2Jersey

        if (player == null) {
            cardView.visibility = View.GONE
            return
        }

        cardView.visibility = View.VISIBLE
        tvName.text = player.name

        // Jersey badge
        if (!player.jerseyNumber.isNullOrEmpty()) {
            tvJersey.visibility = View.VISIBLE
            tvJersey.text = "#${player.jerseyNumber}"
        } else {
            tvJersey.visibility = View.GONE
        }

        // Photo or initials
        if (!player.profilePhotoUrl.isNullOrEmpty()) {
            tvInitials.visibility = View.INVISIBLE
            Glide.with(this)
                .load(player.profilePhotoUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivPhoto)
        } else {
            tvInitials.visibility = View.VISIBLE
            tvInitials.text = player.name
                .split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
            ivPhoto.setImageDrawable(null)
        }

        // Update table header names
        updateTableHeaders()
    }

    private fun updateTableHeaders() {
        binding.tvTableP1Header.text = selectedPlayer1?.name?.split(" ")?.firstOrNull() ?: "P1"
        binding.tvTableP2Header.text = selectedPlayer2?.name?.split(" ")?.firstOrNull() ?: "P2"
    }

    // ─────────────────────────────────────────────────────────
    // FETCH STATS
    // ─────────────────────────────────────────────────────────

    private fun refreshStats() {
        // Called when sport changes — re-fetch both
        stats1 = null
        stats2 = null
        fetchStats(1)
        fetchStats(2)
    }

    private fun fetchStats(playerNum: Int) {
        val player = if (playerNum == 1) selectedPlayer1 else selectedPlayer2
        if (player == null) {
            if (playerNum == 1) stats1 = null else stats2 = null
            renderStatsTable()
            return
        }

        if (playerNum == 1) stats1Job?.cancel() else stats2Job?.cancel()

        val job = lifecycleScope.launch {
            showStatsLoading(true)
            try {
                val response = api.getPlayerStatsByIdAndSport(player.id, activeSport)

                if (response.isSuccessful) {
                    if (playerNum == 1) stats1 = response.body()
                    else                stats2 = response.body()
                } else {
                    if (playerNum == 1) stats1 = null else stats2 = null
                }
            } catch (e: Exception) {
                android.util.Log.e("StatsDebug", "P$playerNum Exception: ${e::class.simpleName} - ${e.message}", e)
                if (playerNum == 1) stats1 = null else stats2 = null
            } finally {
                showStatsLoading(false)
                renderStatsTable()
            }
        }

        if (playerNum == 1) stats1Job = job else stats2Job = job
    }

    // ─────────────────────────────────────────────────────────
    // RENDER STATS TABLE
    // ─────────────────────────────────────────────────────────

    private fun renderStatsTable() {
        val bothSelected = selectedPlayer1 != null && selectedPlayer2 != null

        binding.cardEmptyState.visibility = if (!bothSelected) View.VISIBLE else View.GONE
        binding.cardStats.visibility      = if (bothSelected) View.VISIBLE else View.GONE

        if (!bothSelected) return

        val rows = SportStatsConfig.buildRows(stats1, stats2, activeSport)
        statsAdapter.submitList(rows)

        val hasData = stats1 != null || stats2 != null
        binding.rvStats.visibility   = if (hasData) View.VISIBLE else View.GONE
        binding.tvNoStats.visibility = if (!hasData) View.VISIBLE else View.GONE
    }

    // ─────────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────────

    private fun setupRecycler() {
        binding.rvStats.apply {
            layoutManager = LinearLayoutManager(this@CompareActivity)
            adapter = statsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showStatsLoading(show: Boolean) {
        binding.layoutStatsLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvStats.visibility            = if (show) View.GONE   else View.VISIBLE
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}