package com.example.fypproject.Activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.PlayerStatsDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.*
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class HeavyStatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHeavyStatsBinding
    private var playerId: Long = -1L
    private var tournamentMap = mutableMapOf<String, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeavyStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerId = getSharedPreferences("MyPrefs", MODE_PRIVATE).getLong("playerId", -1L)

        loadOverallStats()
        fetchTournaments()
        styleChip(binding.chipCricket, true)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnOverallStats.setOnClickListener {
            updateToggleState(isOverall = true)
            loadOverallStats()
        }

        binding.btnByTournament.setOnClickListener {
            updateToggleState(isOverall = false)
            binding.statsContainer.visibility = View.GONE
            binding.spinnerTournaments.setText("", false)
        }

        binding.chipGroupSports.setOnCheckedStateChangeListener { group, checkedIds ->
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as Chip
                styleChip(chip, false)
            }
            if (checkedIds.isNotEmpty()) {
                val selectedChip = findViewById<Chip>(checkedIds[0])
                styleChip(selectedChip, true)
                loadOverallStats()
            }
        }

        binding.spinnerTournaments.setOnItemClickListener { parent, _, position, _ ->
            val name = parent.getItemAtPosition(position).toString()
            tournamentMap[name]?.let { id ->
                loadTournamentStats(id)
            }
        }
    }

    private fun updateToggleState(isOverall: Boolean) {
        val primaryColor = ContextCompat.getColor(this, R.color.primaryColor)
        val grayColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val white = ContextCompat.getColor(this, android.R.color.white)
        val black = ContextCompat.getColor(this, android.R.color.black)

        if (isOverall) {
            binding.btnOverallStats.backgroundTintList = ColorStateList.valueOf(primaryColor)
            binding.btnByTournament.backgroundTintList = ColorStateList.valueOf(grayColor)
            binding.btnOverallStats.setTextColor(white)
            binding.btnByTournament.setTextColor(black)

            binding.sportSelectorContainer.visibility = View.VISIBLE
            binding.tournamentDropdownLayout.visibility = View.GONE
            binding.statsContainer.visibility = View.VISIBLE
        } else {
            binding.btnByTournament.backgroundTintList = ColorStateList.valueOf(primaryColor)
            binding.btnOverallStats.backgroundTintList = ColorStateList.valueOf(grayColor)
            binding.btnByTournament.setTextColor(white)
            binding.btnOverallStats.setTextColor(black)

            binding.sportSelectorContainer.visibility = View.GONE
            binding.tournamentDropdownLayout.visibility = View.VISIBLE

            binding.statsContainer.visibility = View.GONE
        }
    }

    private fun loadTournamentStats(tId: Long) {
        lifecycleScope.launch {
            showProgress(true)
            try {
                val stats = api.getPlayerTournamentStats(playerId, tId)
                binding.statsContainer.visibility = View.VISIBLE
                populateUI(stats)
            } catch (e: Exception) {
                binding.statsContainer.visibility = View.GONE
                Toast.makeText(this@HeavyStatsActivity, "No data found", Toast.LENGTH_SHORT).show()
            } finally {
                showProgress(false)
            }
        }
    }

    private fun loadOverallStats() {
        lifecycleScope.launch {
            showProgress(true)
            try {
                val stats = api.getPlayerStats(playerId)
                binding.statsContainer.visibility = View.VISIBLE
                populateUI(stats)
            } catch (e: Exception) {
                Toast.makeText(this@HeavyStatsActivity, "API Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showProgress(false)
            }
        }
    }

    private fun styleChip(chip: Chip, isSelected: Boolean) {
        if (isSelected) {
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor))
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray))
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }
    }

    private fun populateUI(stats: PlayerStatsDto) {
        val headerBinding = CardPlayerStatsHeaderBinding.bind(binding.playerHeader.root)
        headerBinding.tvPlayerName.text = stats.playerName

        ItemSummaryStatsBinding.bind(binding.boxMatches.root).apply {
            tvBoxLabel.text = "Matches"
            tvBoxValue.text = stats.matchesPlayed.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxRuns.root).apply {
            tvBoxLabel.text = "Runs"
            tvBoxValue.text = stats.totalRuns.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxWickets.root).apply {
            tvBoxLabel.text = "Wickets"
            tvBoxValue.text = stats.wickets.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxManOfMatch.root).apply {
            tvBoxLabel.text = "POMs"
            tvBoxValue.text = stats.pomCount.toString()
        }

        setupGrid(binding.layoutBattingStats.root, "Batting Stats", listOf(
            "Runs" to stats.totalRuns.toString(),
            "Balls Faced" to stats.ballsFaced.toString(),
            "Avg" to String.format("%.2f", stats.battingAvg),
            "S/R" to String.format("%.2f", stats.strikeRate),
            "High" to stats.highest.toString(),
            "4s" to stats.fours.toString(),
            "6s" to stats.sixes.toString(),
            "Not Outs" to stats.notOuts.toString()

        ))

        setupGrid(binding.layoutBowlingStats.root, "Bowling Stats", listOf(
            "Wkts" to stats.wickets.toString(),
            "Econ" to String.format("%.2f", stats.economy),
            "Avg" to String.format("%.2f", stats.bowlingAverage),
            "Balls" to stats.ballsBowled.toString(),
            "Runs Conceded" to stats.runsConceded.toString(),
        ))
    }

    private fun setupGrid(root: View, title: String, dataList: List<Pair<String, String>>) {
        root.findViewById<TextView>(R.id.tvTableHeaderTitle)?.text = title
        val grid = root.findViewById<ViewGroup>(R.id.statsGrid)
        if (grid != null) {
            grid.removeAllViews()
            dataList.forEach { (label, value) ->
                val itemBinding = ItemGridStatsBinding.inflate(layoutInflater, grid, false)
                itemBinding.tvGridLabel.text = label
                itemBinding.tvGridValue.text = value
                grid.addView(itemBinding.root)
            }
        }
    }

    private fun fetchTournaments() {
        lifecycleScope.launch {
            showProgress(true)
            try {
                val response = api.getTournamentNamesAndIds()
                val names = mutableListOf<String>()
                response.forEach { map ->
                    map.forEach { (id, name) ->
                        tournamentMap[name] = id
                        names.add(name)
                    }
                }
                val adapter = ArrayAdapter(this@HeavyStatsActivity, android.R.layout.simple_dropdown_item_1line, names)
                binding.spinnerTournaments.setAdapter(adapter)
            } catch (e: Exception) { } finally {
                showProgress(false)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.progressBar.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor))
    }
}