package com.example.fypproject.Activity

import android.content.res.ColorStateList
import android.graphics.Color.DKGRAY
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
import com.example.fypproject.databinding.ActivityHeavyStatsBinding
import com.example.fypproject.databinding.CardPlayerStatsHeaderBinding
import com.example.fypproject.databinding.ItemGridStatsBinding
import com.example.fypproject.databinding.ItemSummaryStatsBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Locale

class HeavyStatsActivity : AppCompatActivity() {

    companion object {
        private const val VIEW_OVERALL = "overall"
        private const val VIEW_TOURNAMENT = "tournament"

        private const val SPORT_CRICKET = "cricket"
        private const val SPORT_FUTSAL = "futsal"
        private const val SPORT_VOLLEYBALL = "volleyball"

        private const val SPORT_BADMINTON = "badminton"
    }

    private lateinit var binding: ActivityHeavyStatsBinding

    private var playerId: Long = -1L
    private val tournamentMap = linkedMapOf<String, Long>()

    private var overallStats: PlayerStatsDto? = null
    private var tournamentStats: PlayerStatsDto? = null
    private var selectedTournamentId: Long? = null
    private var selectedTournamentName: String? = null

    private var activeView: String = VIEW_OVERALL
    private var manualSport: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeavyStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerId = getSharedPreferences("MyPrefs", MODE_PRIVATE).getLong("playerId", -1L)
        if (playerId == -1L) {
            Toast.makeText(this, "Player not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }

        setupViewToggle()
        setupSportSelector()
        setupTournamentSelector()

        updateToggleState(isOverall = true)
        loadOverallStats(sport = null)
        fetchTournaments()
    }


    private fun setupViewToggle() {
        binding.btnOverallStats.setOnClickListener {
            activeView = VIEW_OVERALL
            updateToggleState(isOverall = true)
            if (overallStats == null) loadOverallStats(sport = manualSport)
            else renderCurrentStats()
        }

        binding.btnByTournament.setOnClickListener {
            activeView = VIEW_TOURNAMENT
            updateToggleState(isOverall = false)
            renderCurrentStats()
        }
    }


    private fun setupSportSelector() {
        applySelectedSportChip(SPORT_CRICKET)

        binding.chipGroupSports.setOnCheckedStateChangeListener { group, checkedIds ->

            for (i in 0 until group.childCount) {
                styleChip(group.getChildAt(i) as Chip, false)
            }

            if (checkedIds.isNotEmpty()) {
                val selectedChip = findViewById<Chip>(checkedIds.first())
                styleChip(selectedChip, true)


                val newSport = chipToSport(selectedChip.id)
                handleSportChange(newSport)
            }
        }
    }


    private fun handleSportChange(sport: String) {
        manualSport = sport
        if (activeView == VIEW_OVERALL) {
            loadOverallStats(sport = sport)
        }
    }

    // ✅ Naya sport add karna ho to sirf yahan ek jagah add karo
    private fun chipToSport(chipId: Int): String = when (chipId) {
        R.id.chipFutsal -> SPORT_FUTSAL
        R.id.chipVolleyball-> SPORT_VOLLEYBALL
        R.id.chipBadminton-> SPORT_BADMINTON
        else -> SPORT_CRICKET
    }

    // ✅ Naya sport add karna ho to sirf yahan ek jagah add karo
    private fun sportToChipId(sport: String): Int = when (sport) {
        SPORT_FUTSAL -> R.id.chipFutsal
        SPORT_VOLLEYBALL -> R.id.chipVolleyball
        SPORT_BADMINTON -> R.id.chipBadminton
        else -> R.id.chipCricket
    }


    private fun setupTournamentSelector() {
        binding.spinnerTournaments.setOnItemClickListener { parent, _, position, _ ->
            val name = parent.getItemAtPosition(position).toString()
            selectedTournamentName = name
            tournamentMap[name]?.let { id ->
                selectedTournamentId = id
                loadTournamentStats(id)
            }
        }
    }

    private fun loadOverallStats(sport: String?) {
        lifecycleScope.launch {
            showProgress(true)
            try {
                val stats = api.getPlayerStats(playerId, tournamentId = null, sport = sport)

                overallStats = stats

                if (manualSport == null) {
                    manualSport = detectSport(stats)
                    applySelectedSportChip(manualSport!!)
                }

                renderCurrentStats()
            } catch (e: Exception) {
                Toast.makeText(
                    this@HeavyStatsActivity,
                    "API Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showProgress(false)
            }
        }
    }

    private fun loadTournamentStats(tournamentId: Long) {
        lifecycleScope.launch {
            showProgress(true)
            try {
                val stats = api.getPlayerStats(playerId, tournamentId = tournamentId, sport = null)
                tournamentStats = stats
                renderCurrentStats()
            } catch (_: Exception) {
                tournamentStats = null
                Toast.makeText(this@HeavyStatsActivity, "No data found", Toast.LENGTH_SHORT).show()
                renderCurrentStats()
            } finally {
                showProgress(false)
            }
        }
    }

    private fun fetchTournaments() {
        lifecycleScope.launch {
            try {
                val response = api.getTournamentNamesAndIds()
                val names = mutableListOf<String>()
                response.forEach { map ->
                    map.forEach { (id, name) ->
                        tournamentMap[name] = id
                        names.add(name)
                    }
                }
                val adapter = ArrayAdapter(
                    this@HeavyStatsActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                binding.spinnerTournaments.setAdapter(adapter)
            } catch (_: Exception) {
            }
        }
    }

    // ─────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────

    private fun renderCurrentStats() {
        val stats = if (activeView == VIEW_OVERALL) overallStats else tournamentStats

        if (stats == null) {
            binding.statsContainer.visibility = View.GONE
            if (activeView == VIEW_TOURNAMENT) {
                binding.tvTournamentEmptyState.visibility =
                    if (selectedTournamentId == null) View.VISIBLE else View.GONE
                binding.tvTournamentSportHint.visibility = View.GONE
            }
            return
        }

        val detectedSport = detectSport(stats)
        val activeSport = if (activeView == VIEW_OVERALL && manualSport != null) {
            manualSport!!
        } else {
            detectedSport
        }

        binding.tvTournamentSportHint.visibility =
            if (activeView == VIEW_TOURNAMENT) View.VISIBLE else View.GONE
        binding.tvTournamentSportHint.text =
            "Sport: ${activeSport.replaceFirstChar { it.uppercase() }}"

        binding.tvTournamentEmptyState.visibility = View.GONE
        binding.statsContainer.visibility = View.VISIBLE

        populateUI(stats, activeSport)
    }

    private fun updateToggleState(isOverall: Boolean) {
        val primaryColor = ContextCompat.getColor(this, R.color.primaryColor)
        val grayColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val white = ContextCompat.getColor(this, android.R.color.white)
        val black = ContextCompat.getColor(this, android.R.color.black)

        if (isOverall) {
            binding.btnOverallStats.backgroundTintList = ColorStateList.valueOf(primaryColor)
            binding.btnByTournament.backgroundTintList = ColorStateList.valueOf(DKGRAY)
            binding.btnOverallStats.setTextColor(white)
            binding.btnByTournament.setTextColor(black)

            binding.sportSelectorContainer.visibility = View.VISIBLE
            binding.tournamentDropdownLayout.visibility = View.GONE
            binding.tvTournamentSportHint.visibility = View.GONE
            binding.tvTournamentEmptyState.visibility = View.GONE
            binding.statsContainer.visibility = if (overallStats != null) View.VISIBLE else View.GONE
        } else {
            binding.btnByTournament.backgroundTintList = ColorStateList.valueOf(primaryColor)
            binding.btnOverallStats.backgroundTintList = ColorStateList.valueOf(DKGRAY)
            binding.btnByTournament.setTextColor(white)
            binding.btnOverallStats.setTextColor(black)

            binding.sportSelectorContainer.visibility = View.GONE
            binding.tournamentDropdownLayout.visibility = View.VISIBLE
            binding.statsContainer.visibility = if (tournamentStats != null) View.VISIBLE else View.GONE
            binding.tvTournamentSportHint.visibility = if (tournamentStats != null) View.VISIBLE else View.GONE
            binding.tvTournamentEmptyState.visibility =
                if (selectedTournamentId == null) View.VISIBLE else View.GONE
        }
    }

    private fun styleChip(chip: Chip, isSelected: Boolean) {
        if (isSelected) {
            chip.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor))
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            chip.chipBackgroundColor =
                ColorStateList.valueOf  (DKGRAY)
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }
    }

    private fun applySelectedSportChip(sport: String) {
        val chipId = sportToChipId(sport)
        binding.chipGroupSports.check(chipId)
        styleChip(binding.chipCricket, sport == SPORT_CRICKET)
        styleChip(binding.chipFutsal, sport == SPORT_FUTSAL)
        styleChip(binding.chipVolleyball, sport == SPORT_VOLLEYBALL)
        styleChip(binding.chipBadminton, sport == SPORT_BADMINTON)
    }


    private fun detectSport(stats: PlayerStatsDto): String {
        val responseSport = stats.sport?.lowercase(Locale.US)
        return when {
            responseSport?.contains(SPORT_FUTSAL) == true -> SPORT_FUTSAL
            responseSport?.contains(SPORT_CRICKET) == true -> SPORT_CRICKET
            responseSport?.contains(SPORT_VOLLEYBALL) == true -> SPORT_VOLLEYBALL
            responseSport?.contains(SPORT_BADMINTON) == true -> SPORT_BADMINTON
            else -> SPORT_CRICKET // default cricket, JS jaisa
        }
    }


    private fun populateUI(stats: PlayerStatsDto, sport: String) {
        val headerBinding = CardPlayerStatsHeaderBinding.bind(binding.playerHeader.root)
        headerBinding.tvPlayerName.text = stats.playerName ?: "Player"

        // ✅ Naya sport add karna ho to sirf yahan ek jagah add karo
        when (sport) {
            SPORT_FUTSAL -> bindFutsalStats(stats)
            SPORT_VOLLEYBALL->bindVolleyballStats(stats)
            SPORT_BADMINTON->bindBadmintonStats(stats)
            else -> bindCricketStats(stats)
        }
    }

    private fun bindCricketStats(stats: PlayerStatsDto) {
        ItemSummaryStatsBinding.bind(binding.boxMatches.root).apply {
            tvBoxLabel.text = "Matches"
            tvBoxValue.text = (stats.cricketMatchesPlayed.takeIf { it > 0 } ?: stats.matchesPlayed).toString()
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

        setupGrid(
            binding.layoutBattingStats.root, "Batting Stats",
            listOf(
                "Runs" to stats.totalRuns.toString(),
                "Balls Faced" to stats.ballsFaced.toString(),
                "Strike Rate" to formatDouble(stats.strikeRate),
                "Highest" to stats.highest.toString(),
                "Fours" to stats.fours.toString(),
                "Sixes" to stats.sixes.toString(),
                "Not Outs" to stats.notOuts.toString(),
                "Average" to formatDouble(stats.battingAvg)
            )
        )

        setupGrid(
            binding.layoutBowlingStats.root, "Bowling Stats",
            listOf(
                "Wickets" to stats.wickets.toString(),
                "Balls" to stats.ballsBowled.toString(),
                "Runs Conceded" to stats.runsConceded.toString(),
                "Economy" to formatDouble(stats.economy),
                "Average" to formatDouble(stats.bowlingAverage)
            )
        )
    }

    private fun bindFutsalStats(stats: PlayerStatsDto) {
        ItemSummaryStatsBinding.bind(binding.boxMatches.root).apply {
            tvBoxLabel.text = "Matches"
            tvBoxValue.text = (stats.futsalMatchesPlayed.takeIf { it > 0 } ?: 0).toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxRuns.root).apply {
            tvBoxLabel.text = "Goals"
            tvBoxValue.text = stats.goals.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxWickets.root).apply {
            tvBoxLabel.text = "Assists"
            tvBoxValue.text = stats.assists.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxManOfMatch.root).apply {
            tvBoxLabel.text = "POMs"
            tvBoxValue.text = stats.pomCount.toString()
        }

        setupGrid(
            binding.layoutBattingStats.root, "Scoring",
            listOf(
                "Goals" to stats.goals.toString(),
                "Assists" to stats.assists.toString(),
                "G+A" to (stats.goals + stats.assists).toString()
            )
        )

        setupGrid(
            binding.layoutBowlingStats.root, "Discipline",
            listOf(
                "Fouls" to stats.futsalFouls.toString(),
                "Yellow Cards" to stats.yellowCards.toString(),
                "Red Cards" to stats.redCards.toString()
            )
        )
    }
    private fun bindVolleyballStats(stats: PlayerStatsDto) {
        val points  = stats.pointsScored.takeIf { it > 0 } ?: stats.goals
        val aces    = stats.aces.takeIf { it > 0 } ?: stats.assists
        val blocks  = stats.blocks.takeIf { it > 0 } ?: stats.futsalFouls
        val atkErr  = stats.attackErrors.takeIf { it > 0 } ?: stats.yellowCards
        val svcErr  = stats.serviceErrors.takeIf { it > 0 } ?: stats.redCards

        ItemSummaryStatsBinding.bind(binding.boxMatches.root).apply {
            tvBoxLabel.text = "Matches"
            tvBoxValue.text = (stats.volleyballMatchesPlayed.takeIf { it > 0 } ?: stats.matchesPlayed).toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxRuns.root).apply {
            tvBoxLabel.text = "Points"
            tvBoxValue.text = points.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxWickets.root).apply {
            tvBoxLabel.text = "Aces"
            tvBoxValue.text = aces.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxManOfMatch.root).apply {
            tvBoxLabel.text = "POMs"
            tvBoxValue.text = stats.pomCount.toString()
        }

        setupGrid(
            binding.layoutBattingStats.root, "Attacking & Serving",
            listOf(
                "Points Scored" to points.toString(),
                "Aces"          to aces.toString(),
                "Blocks"        to blocks.toString()
            )
        )
        setupGrid(
            binding.layoutBowlingStats.root, "Errors",
            listOf(
                "Attack Errors"  to atkErr.toString(),
                "Service Errors" to svcErr.toString()
            )
        )
    }

    private fun bindBadmintonStats(stats: PlayerStatsDto) {

        val matches   = stats.badmintonMatchesPlayed.takeIf { it > 0 } ?: stats.matchesPlayed
        val points    = stats.goals
        val smashesAces = stats.assists
        val faults    = stats.futsalFouls
        val outShots  = stats.yellowCards

        ItemSummaryStatsBinding.bind(binding.boxMatches.root).apply {
            tvBoxLabel.text = "Matches"
            tvBoxValue.text = matches.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxRuns.root).apply {
            tvBoxLabel.text = "Points"
            tvBoxValue.text = points.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxWickets.root).apply {
            tvBoxLabel.text = "Smashes"
            tvBoxValue.text = smashesAces.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxManOfMatch.root).apply {
            tvBoxLabel.text = "POMs"
            tvBoxValue.text = stats.pomCount.toString()
        }

        setupGrid(
            binding.layoutBattingStats.root, "Performance",
            listOf(
                "Points Scored" to points.toString(),
                "Smashes + Aces" to smashesAces.toString()
            )
        )

        setupGrid(
            binding.layoutBowlingStats.root, "Faults",
            listOf(
                "Faults (Net/Foot)" to faults.toString(),
                "Out Shots" to outShots.toString()
            )
        )
    }

    private fun setupGrid(root: View, title: String, dataList: List<Pair<String, String>>) {
        root.findViewById<TextView>(R.id.tvTableHeaderTitle)?.text = title
        val grid = root.findViewById<ViewGroup>(R.id.statsGrid)
        grid?.let {
            it.removeAllViews()
            dataList.forEach { (label, value) ->
                val itemBinding = ItemGridStatsBinding.inflate(layoutInflater, it, false)
                itemBinding.tvGridLabel.text = label
                itemBinding.tvGridValue.text = value
                it.addView(itemBinding.root)
            }
        }
    }

    private fun formatDouble(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.progressBar.indeterminateTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor))
    }
}