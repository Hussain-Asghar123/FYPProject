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
import com.example.fypproject.DTO.PlayerDto
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
import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class HeavyStatsActivity : AppCompatActivity() {

    companion object {
        private const val VIEW_OVERALL = "overall"
        private const val VIEW_TOURNAMENT = "tournament"
        private const val SPORT_CRICKET = "cricket"
        private const val SPORT_FUTSAL = "futsal"
        private const val SPORT_VOLLEYBALL = "volleyball"
        private const val SPORT_BADMINTON = "badminton"
        private const val SPORT_TABLETENNIS = "table tennis"
        private const val SPORT_LUDO = "ludo"
        private const val SPORT_CHESS = "chess"
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

    // Admin
    private var isAdmin: Boolean = false
    private var allPlayers: List<PlayerDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeavyStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val role = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("role", "")
        isAdmin = role == "ADMIN"

        if (isAdmin) {
            setupAdminFlow()
            return
        }

        // Normal player flow
        playerId = getSharedPreferences("MyPrefs", MODE_PRIVATE).getLong("playerId", -1L)
        if (playerId == -1L) {
            Toast.makeText(this, "Player not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewToggle()
        setupSportSelector()
        setupTournamentSelector()
        updateToggleState(isOverall = true)
        loadOverallStats(sport = null)
        fetchTournaments()
    }

    // ─────────────────────────────────────────────
    // ADMIN FLOW
    // ─────────────────────────────────────────────

    private fun setupAdminFlow() {
        // Show player selector, hide everything else initially
        binding.playerSelectorLayout.visibility = View.VISIBLE
        binding.sportSelectorContainer.visibility = View.GONE
        binding.statsContainer.visibility = View.GONE
        binding.tournamentDropdownLayout.visibility = View.GONE
        binding.tvTournamentSportHint.visibility = View.GONE
        binding.tvTournamentEmptyState.text = "Select a player to view stats"
        binding.tvTournamentEmptyState.visibility = View.VISIBLE
        binding.btnOverallStats.visibility = View.GONE
        binding.btnByTournament.visibility = View.GONE

        lifecycleScope.launch {
            showLoading(true)
            try {
                val response = api.getAllPlayers()
                allPlayers = response.body() ?: emptyList()

                val names = allPlayers.map { it.name }
                val adapter = ArrayAdapter(
                    this@HeavyStatsActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                binding.spinnerPlayers.setAdapter(adapter)

                binding.spinnerPlayers.setOnItemClickListener { _, _, position, _ ->
                    val selectedPlayer = allPlayers[position]
                    playerId = selectedPlayer.id


                    // Reset state for new player
                    manualSport = null
                    selectedTournamentId = null
                    selectedTournamentName = null
                    overallStats = null
                    tournamentStats = null

                    // Show UI
                    binding.tvTournamentEmptyState.visibility = View.GONE
                    binding.btnOverallStats.visibility = View.VISIBLE
                    binding.btnByTournament.visibility = View.VISIBLE

                    setupViewToggle()
                    setupSportSelector()
                    setupTournamentSelector()
                    updateToggleState(isOverall = true)
                    loadOverallStats(sport = null)
                    fetchTournaments()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@HeavyStatsActivity,
                    "Failed to load players: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    // ─────────────────────────────────────────────
    // NORMAL FLOW
    // ─────────────────────────────────────────────

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

    private fun chipToSport(chipId: Int): String = when (chipId) {
        R.id.chipFutsal -> SPORT_FUTSAL
        R.id.chipVolleyball -> SPORT_VOLLEYBALL
        R.id.chipBadminton -> SPORT_BADMINTON
        R.id.chipTableTennis -> SPORT_TABLETENNIS
        R.id.chipLudo -> SPORT_LUDO
        R.id.chipChess -> SPORT_CHESS
        else -> SPORT_CRICKET
    }

    private fun sportToChipId(sport: String): Int = when (normalizeSportKey(sport)) {
        SPORT_FUTSAL -> R.id.chipFutsal
        SPORT_VOLLEYBALL -> R.id.chipVolleyball
        SPORT_BADMINTON -> R.id.chipBadminton
        SPORT_TABLETENNIS -> R.id.chipTableTennis
        SPORT_LUDO -> R.id.chipLudo
        SPORT_CHESS -> R.id.chipChess
        else -> R.id.chipCricket
    }

    private fun normalizeSportKey(rawSport: String?): String {
        val compact = rawSport
            ?.lowercase(Locale.US)
            ?.replace("_", " ")
            ?.replace("-", " ")
            ?.replace("\\s+".toRegex(), " ")
            ?.trim()
            ?: return SPORT_CRICKET

        return when (compact.replace(" ", "")) {
            "futsal" -> SPORT_FUTSAL
            "cricket" -> SPORT_CRICKET
            "volleyball" -> SPORT_VOLLEYBALL
            "badminton" -> SPORT_BADMINTON
            "tabletennis" -> SPORT_TABLETENNIS
            "ludo" -> SPORT_LUDO
            "chess" -> SPORT_CHESS
            else -> SPORT_CRICKET
        }
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
            showLoading(true)
            try {
                val stats = api.getPlayerStats(playerId, tournamentId = null, sport = sport)
                overallStats = stats

                if (manualSport == null) {
                    manualSport = detectSport(stats)
                    applySelectedSportChip(manualSport!!)
                }

                renderCurrentStats()
                checkEmptyState()
            } catch (e: Exception) {
                Toast.makeText(
                    this@HeavyStatsActivity,
                    "API Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                checkEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadTournamentStats(tournamentId: Long) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val stats = api.getPlayerStats(playerId, tournamentId = tournamentId, sport = null)
                tournamentStats = stats
                renderCurrentStats()
                checkEmptyState()
            } catch (_: Exception) {
                tournamentStats = null
                Toast.makeText(this@HeavyStatsActivity, "No data found", Toast.LENGTH_SHORT).show()
                renderCurrentStats()
                checkEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun fetchTournaments() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                tournamentMap.clear()
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
                checkEmptyState()
            } catch (_: Exception) {
                checkEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

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
            chip.chipBackgroundColor = ColorStateList.valueOf(DKGRAY)
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
        styleChip(binding.chipTableTennis, sport == SPORT_TABLETENNIS)
        styleChip(binding.chipLudo, sport == SPORT_LUDO)
        styleChip(binding.chipChess, sport == SPORT_CHESS)
    }

    private fun detectSport(stats: PlayerStatsDto): String {
        return normalizeSportKey(stats.sport)
    }

    private fun populateUI(stats: PlayerStatsDto, sport: String) {
        hideAllPlayerChartCards()
        val headerBinding = CardPlayerStatsHeaderBinding.bind(binding.playerHeader.root)
        headerBinding.tvPlayerName.text = stats.playerName ?: "Player"

        binding.layoutBattingStats.root.visibility = View.VISIBLE
        binding.layoutBowlingStats.root.visibility = View.VISIBLE

        when (sport) {
            SPORT_FUTSAL -> bindFutsalStats(stats)
            SPORT_VOLLEYBALL -> bindVolleyballStats(stats)
            SPORT_BADMINTON -> bindBadmintonStats(stats)
            SPORT_TABLETENNIS -> bindTableTennisStats(stats)
            SPORT_LUDO -> bindLudoStats(stats)
            SPORT_CHESS -> bindChessStats(stats)
            else -> bindCricketStats(stats)
        }
    }
    private fun hideAllPlayerChartCards() {
        binding.cardCricketPlayerCharts.visibility     = View.GONE
    }

    private fun bindCricketStats(stats: PlayerStatsDto) {
        ItemSummaryStatsBinding.bind(binding.boxMatches.root).apply {
            tvBoxLabel.text = "Matches"
            tvBoxValue.text = (stats.cricketMatchesPlayed.takeIf { it > 0 } ?: stats.matchesPlayed).toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxRuns.root).apply {
            tvBoxLabel.text = "Runs"
            tvBoxValue.text = stats.runsScored.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxWickets.root).apply {
            tvBoxLabel.text = "Wickets"
            tvBoxValue.text = stats.wicketsTaken.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxManOfMatch.root).apply {
            tvBoxLabel.text = "POMs"
            tvBoxValue.text = stats.pomCount.toString()
        }

        setupGrid(
            binding.layoutBattingStats.root, "Batting Stats",
            listOf(
                "Runs Scored" to stats.runsScored.toString(),
                "Balls Faced" to stats.ballsFaced.toString(),
                "Strike Rate" to formatDouble(stats.strikeRate),
                "Highest" to stats.highestScore.toString(),
                "Fours" to stats.fours.toString(),
                "Sixes" to stats.sixes.toString(),
                "Not Outs" to stats.notOuts.toString(),
                "Average" to formatDouble(stats.average)
            )
        )

        setupGrid(
            binding.layoutBowlingStats.root, "Bowling Stats",
            listOf(
                "Wickets" to stats.wicketsTaken.toString(),
                "Balls" to stats.ballsBowled.toString(),
                "Runs Conceded" to stats.runsConceded.toString(),
                "Economy" to formatDouble(stats.economy),
                "Average" to formatDouble(stats.bowlingAverage),
                "Best Bowling" to stats.bestBowling,
                "Catches" to stats.catches.toString()
            )
        )
        setupCricketPlayerCharts(stats)
    }

    fun HeavyStatsActivity.setupCricketPlayerCharts(stats: com.example.fypproject.DTO.PlayerStatsDto) {
        binding.cardCricketPlayerCharts.visibility = android.view.View.VISIBLE

        // ── Radar Chart — 5 batting skills ──────────────────────────
        // Normalize each value to 0-100 scale for clean radar
        val maxRuns = 500f; val maxSR = 200f; val maxAvg = 60f
        val maxFours = 40f;  val maxSixes = 20f

        val radarEntries = listOf(
            RadarEntry((stats.runsScored.toFloat() / maxRuns * 100f).coerceAtMost(100f)),
            RadarEntry((stats.strikeRate.toFloat() / maxSR * 100f).coerceAtMost(100f)),
            RadarEntry((stats.average.toFloat() / maxAvg * 100f).coerceAtMost(100f)),
            RadarEntry((stats.fours.toFloat() / maxFours * 100f).coerceAtMost(100f)),
            RadarEntry((stats.sixes.toFloat() / maxSixes * 100f).coerceAtMost(100f))
        )
        val radarDataSet = RadarDataSet(radarEntries, "Batting").apply {
            color             = Color.parseColor("#E31212")
            fillColor         = Color.parseColor("#E31212")
            setDrawFilled(true)
            fillAlpha         = 80
            lineWidth         = 2f
            valueTextSize     = 9f
            valueTextColor    = Color.parseColor("#E31212")
        }
        binding.radarChartCricket.apply {
            data = RadarData(radarDataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(
                listOf("Runs", "Strike\nRate", "Average", "Fours", "Sixes")
            )
            xAxis.textSize = 11f
            yAxis.apply { axisMinimum = 0f; axisMaximum = 100f; setDrawLabels(false) }
            webColor         = Color.LTGRAY
            webColorInner    = Color.LTGRAY
            webAlpha         = 100
            description.isEnabled = false
            legend.isEnabled      = false
            animateXY(1000, 1000)
            invalidate()
        }

        // ── Bar Chart — Batting vs Bowling contribution ──────────────
        // Runs (batting) vs Wickets*10 (bowling) - simple split view
        val contribEntries = listOf(
            BarEntry(0f, stats.runsScored.toFloat()),
            BarEntry(1f, (stats.wicketsTaken * 15).toFloat()),
            BarEntry(2f, stats.catches.toFloat() * 10),
            BarEntry(3f, stats.pomCount.toFloat() * 20)
        )
        val contribLabels = listOf("Runs", "Wkts×15", "Catch×10", "POM×20")
        val contribDataSet = BarDataSet(contribEntries, "Contribution").apply {
            colors = listOf(
                Color.parseColor("#E31212"), Color.parseColor("#1A1A2E"),
                Color.parseColor("#16A34A"), Color.parseColor("#F59E0B")
            )
            valueTextSize  = 10f
            valueTextColor = Color.BLACK
        }
        binding.barChartCricketContrib.apply {
            data = BarData(contribDataSet)
            xAxis.apply {
                position       = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(contribLabels)
                granularity    = 1f
                setDrawGridLines(false)
            }
            axisRight.isEnabled   = false
            axisLeft.axisMinimum  = 0f
            description.isEnabled = false
            legend.isEnabled      = false
            animateY(800)
            invalidate()
        }
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
        val points = stats.pointsScored.takeIf { it > 0 } ?: stats.goals
        val aces = stats.aces.takeIf { it > 0 } ?: stats.assists
        val blocks = stats.blocks.takeIf { it > 0 } ?: stats.futsalFouls
        val atkErr = stats.attackErrors.takeIf { it > 0 } ?: stats.yellowCards
        val svcErr = stats.serviceErrors.takeIf { it > 0 } ?: stats.redCards

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
                "Aces" to aces.toString(),
                "Blocks" to blocks.toString()
            )
        )
        setupGrid(
            binding.layoutBowlingStats.root, "Errors",
            listOf(
                "Attack Errors" to atkErr.toString(),
                "Service Errors" to svcErr.toString()
            )
        )
    }

    private fun bindBadmintonStats(stats: PlayerStatsDto) {
        val matches = stats.badmintonMatchesPlayed.takeIf { it > 0 } ?: stats.matchesPlayed
        val points = stats.goals
        val smashesAces = stats.assists
        val faults = stats.futsalFouls
        val outShots = stats.yellowCards

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

    private fun bindTableTennisStats(stats: PlayerStatsDto) {
        val matches = stats.tableTennisMatchesPlayed.takeIf { it > 0 } ?: stats.matchesPlayed
        val points = stats.goals
        val smashesAces = stats.assists
        val faults = stats.futsalFouls
        val outShots = stats.yellowCards

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

        val attackRatio = if (stats.goals > 0)
            "${((stats.assists.toDouble() / stats.goals) * 100).toInt()}%"
        else "—"

        setupGrid(
            binding.layoutBattingStats.root, "Performance",
            listOf(
                "Points Scored" to points.toString(),
                "Smashes + Aces" to smashesAces.toString(),
                "Attack Ratio" to attackRatio
            )
        )
        setupGrid(
            binding.layoutBowlingStats.root, "Faults",
            listOf(
                "Net/Service Faults" to faults.toString(),
                "Out Shots" to outShots.toString()
            )
        )
    }

    private fun bindLudoStats(stats: PlayerStatsDto) {
        val matches = stats.ludoMatchesPlayed.takeIf { it > 0 } ?: stats.matchesPlayed
        val runs = stats.goals
        val captures = stats.assists

        ItemSummaryStatsBinding.bind(binding.boxMatches.root).apply {
            tvBoxLabel.text = "Matches"
            tvBoxValue.text = matches.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxRuns.root).apply {
            tvBoxLabel.text = "Home Runs"
            tvBoxValue.text = runs.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxWickets.root).apply {
            tvBoxLabel.text = "Captures"
            tvBoxValue.text = captures.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxManOfMatch.root).apply {
            tvBoxLabel.text = "POMs"
            tvBoxValue.text = stats.pomCount.toString()
        }

        setupGrid(
            binding.layoutBattingStats.root, "Performance",
            listOf(
                "Home Runs" to runs.toString(),
                "Captures" to captures.toString()
            )
        )
        binding.layoutBowlingStats.root.visibility = View.GONE
    }

    private fun bindChessStats(stats: PlayerStatsDto) {
        val matches = stats.chessMatchesPlayed.takeIf { it > 0 } ?: stats.matchesPlayed
        val wins = stats.goals
        val check = stats.assists
        val winRate = if (matches > 0)
            "${((stats.goals.toDouble() / matches) * 100).toInt()}%"
        else "—"

        ItemSummaryStatsBinding.bind(binding.boxMatches.root).apply {
            tvBoxLabel.text = "Matches"
            tvBoxValue.text = matches.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxRuns.root).apply {
            tvBoxLabel.text = "Wins"
            tvBoxValue.text = wins.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxWickets.root).apply {
            tvBoxLabel.text = "Checks"
            tvBoxValue.text = check.toString()
        }
        ItemSummaryStatsBinding.bind(binding.boxManOfMatch.root).apply {
            tvBoxLabel.text = "POMs"
            tvBoxValue.text = stats.pomCount.toString()
        }

        setupGrid(
            binding.layoutBattingStats.root, "Performance",
            listOf(
                "Wins" to wins.toString(),
                "Checks" to check.toString(),
                "Win Rate" to winRate
            )
        )
        binding.layoutBowlingStats.root.visibility = View.GONE
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

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.progressBar.indeterminateTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor))
    }

    private fun checkEmptyState() {
        val stats = if (activeView == VIEW_OVERALL) overallStats else tournamentStats
        if (activeView == VIEW_TOURNAMENT && stats == null) {
            binding.tvTournamentEmptyState.visibility =
                if (selectedTournamentId == null) View.VISIBLE else View.GONE
        }
    }
}