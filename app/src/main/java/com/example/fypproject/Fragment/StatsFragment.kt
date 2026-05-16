package com.example.fypproject.Fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.PomAwardAdapter
import com.example.fypproject.Adapter.TournamentStatsAdapter
import com.example.fypproject.DTO.TournamentStatsDto
import com.example.fypproject.DTO.TopVotedPlayerDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.FragmentStatsBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate


class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private var tournamentId: Long = -1L
    private var sportId: Long      = -1L
    private var sportName: String  = ""

    private var pendingRefresh = false

    private val isAdmin: Boolean by lazy {
        val prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        prefs.getString("role", "").equals("ADMIN", ignoreCase = true)
    }

    private var hasLoadedOnce = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)

        tournamentId = arguments?.getLong("tournamentId") ?: -1L
        sportId      = arguments?.getLong("sportId")      ?: -1L
        sportName    = arguments?.getString("sportName").orEmpty()

        if (tournamentId != -1L)
            loadStats()
        hasLoadedOnce=true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadStats(retryCount: Int = 0) {
        setLoading(true)
        binding.tvEmptyState.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val stats = api.getTournamentStats(tournamentId)
                setLoading(false)
                populateUI(stats)
            } catch (e: Exception) {
                if (e is java.net.SocketTimeoutException && retryCount < 5) {
                    delay(2000)
                    loadStats(retryCount + 1)
                } else {
                    setLoading(false)
                    Log.e("StatsFragment", "loadStats error: ${e.message}", e)
                    showError()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError() {
        if (_binding == null) return
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = "No data available"
    }

    private fun populateUI(stats: TournamentStatsDto) {
        hideAllChartCards()
        setupManOfTournament(stats)
        setupFavouritePlayer(stats)

        when (detectSport(stats)) {
            SPORT_FUTSAL       -> populateFutsalUI(stats)
            SPORT_VOLLEYBALL   -> populateVolleyballUI(stats)
            SPORT_BADMINTON    -> populateBadmintonUI(stats)
            SPORT_TABLE_TENNIS -> populateTableTennisUI(stats)
            SPORT_TUG_OF_WAR   -> populateTugOfWarUI(stats)
            SPORT_LUDO         -> populateLudoUI(stats)
            SPORT_CHESS        -> populateChessUI(stats)
            else               -> populateCricketUI(stats)
        }

        populatePomAwards(stats)
    }
    private fun hideAllChartCards() {
        binding.cardCricketCharts.visibility    = View.GONE
        binding.cardFutsalCharts.visibility=    View.GONE
    }

    private fun detectSport(stats: TournamentStatsDto): String {
        val effectiveSportId = sportId.takeIf { it > 0 } ?: stats.sportId
        if (effectiveSportId != null && effectiveSportId > 0) {
            val fromId = when (effectiveSportId) {
                1L -> SPORT_CRICKET
                2L -> SPORT_FUTSAL
                3L -> SPORT_VOLLEYBALL
                4L -> SPORT_BADMINTON
                5L -> SPORT_TABLE_TENNIS
                6L -> SPORT_TUG_OF_WAR
                7L -> SPORT_LUDO
                8L -> SPORT_CHESS
                else -> null
            }
            if (fromId != null) return fromId
        }

        val name = (stats.sport ?: sportName).lowercase(Locale.US).trim()
        return when {
            name == SPORT_FUTSAL                                              -> SPORT_FUTSAL
            name == SPORT_VOLLEYBALL                                          -> SPORT_VOLLEYBALL
            name == SPORT_CRICKET                                             -> SPORT_CRICKET
            name == SPORT_BADMINTON                                           -> SPORT_BADMINTON
            name == "table tennis" || name == "tabletennis"
                    || name == SPORT_TABLE_TENNIS                             -> SPORT_TABLE_TENNIS
            name == "tug of war" || name == SPORT_TUG_OF_WAR                 -> SPORT_TUG_OF_WAR
            name == SPORT_LUDO                                                -> SPORT_LUDO
            name == SPORT_CHESS                                               -> SPORT_CHESS
            name.contains("futsal")                                           -> SPORT_FUTSAL
            name.contains("volleyball")                                       -> SPORT_VOLLEYBALL
            name.contains("badminton")                                        -> SPORT_BADMINTON
            name.contains("table")                                            -> SPORT_TABLE_TENNIS
            name.contains("tug")                                              -> SPORT_TUG_OF_WAR
            name.contains("ludo")                                             -> SPORT_LUDO
            name.contains("chess")                                            -> SPORT_CHESS
            stats.topGoalScorers.orEmpty().isNotEmpty()
                    || stats.topAssistants.orEmpty().isNotEmpty()             -> SPORT_FUTSAL
            else                                                              -> SPORT_CRICKET
        }
    }

    private fun setupManOfTournament(stats: TournamentStatsDto) {
        val slots = stats.manOfTournament.orEmpty()

        binding.tvMotRank1.text = slots.getOrNull(0)?.playerName ?: "TBD"

        binding.tvMotRank2.visibility = View.GONE
        binding.tvMotRank3.visibility = View.GONE

        binding.btnEditMot.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.btnEditMot.setOnClickListener { openMotEditDialog() }
    }
    private fun openMotEditDialog() {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val candidates = api.getTopStatPlayers(tournamentId)
                setLoading(false)
                if (candidates.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "No stat players found for this tournament", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                showMotSelectionDialog(candidates)
            } catch (e: Exception) {
                setLoading(false)
                Log.e("StatsFragment", "getTopStatPlayers error: ${e.message}", e)
                Toast.makeText(requireContext(),
                    "Failed to load players, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMotSelectionDialog(candidates: List<TopVotedPlayerDto>) {
        val ctx    = requireContext()
        val medals = listOf("🥇", "🥈", "🥉")

        val rankGroup = RadioGroup(ctx).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(48, 16, 48, 4)
        }
        listOf("Rank 1 🥇", "Rank 2 🥈", "Rank 3 🥉").forEachIndexed { i, label ->
            RadioButton(ctx).apply {
                id   = 100 + i
                text = label
                rankGroup.addView(this)
            }
        }
        (rankGroup.getChildAt(0) as? RadioButton)?.isChecked = true

        val playerGroup = RadioGroup(ctx).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(48, 8, 48, 8)
        }
        candidates.forEachIndexed { index, candidate ->
            RadioButton(ctx).apply {
                id       = index
                text     = "${medals.getOrElse(index) { "  "}}  ${candidate.playerName}  (${candidate.votes} stat pts)"
                textSize = 14f
                setPadding(16, 12, 16, 12)
                playerGroup.addView(this)
            }
        }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(ctx).apply {
                text = "Select rank slot"
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray))
                setPadding(48, 16, 48, 4)
            })
            addView(rankGroup)
            addView(TextView(ctx).apply {
                text = "Select player"
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray))
                setPadding(48, 8, 48, 4)
            })
            addView(playerGroup)
        }

        AlertDialog.Builder(ctx)
            .setTitle("Select Man of the Tournament")
            .setView(container)
            .setPositiveButton("Confirm & Save") { dialog, _ ->
                val checkedPlayer = playerGroup.checkedRadioButtonId
                if (checkedPlayer == -1) {
                    Toast.makeText(ctx, "Please select a player", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val rank = rankGroup.checkedRadioButtonId - 99
                saveManOfTournamentRanked(candidates[checkedPlayer].playerId, rank)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveManOfTournamentRanked(playerId: Long, rank: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                api.setManOfTournamentRanked(tournamentId, playerId, rank)
                Toast.makeText(requireContext(), "Man of the Tournament updated!", Toast.LENGTH_SHORT).show()
                pendingRefresh = false
                loadStats()
            } catch (e: Exception) {
                Log.e("StatsFragment", "setManOfTournamentRanked error: ${e.message}", e)
                Toast.makeText(requireContext(), "Save failed, please try again", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setupFavouritePlayer(stats: TournamentStatsDto) {
        binding.tvFavouritePlayer.text = stats.favouritePlayer?.playerName ?: "TBD"

        if (stats.favouritePlayer?.reason.isNullOrBlank()) {
            binding.tvFavouritePlayerReason.visibility = View.GONE
        } else {
            binding.tvFavouritePlayerReason.visibility = View.VISIBLE
            binding.tvFavouritePlayerReason.text = stats.favouritePlayer?.reason
        }

        binding.btnEditFavPlayer.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.btnEditFavPlayer.setOnClickListener { openFavouritePlayerEditDialog() }
    }

    private fun openFavouritePlayerEditDialog() {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val candidates = api.getTopVotedPlayers(tournamentId)
                setLoading(false)
                if (candidates.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "No voted players found", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                showFavouritePlayerDialog(candidates)
            } catch (e: Exception) {
                setLoading(false)
                Log.e("StatsFragment", "getTopVotedPlayers error: ${e.message}", e)
                Toast.makeText(requireContext(),
                    "Failed to load players", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFavouritePlayerDialog(candidates: List<TopVotedPlayerDto>) {
        val ctx = requireContext()

        val radioGroup = RadioGroup(ctx).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val hint = TextView(ctx).apply {
            text = "Top fan-voted players"
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray))
            setPadding(48, 16, 48, 8)
        }
        candidates.forEachIndexed { index, candidate ->
            RadioButton(ctx).apply {
                id       = index
                text     = "#${index + 1}  ${candidate.playerName}  (${candidate.votes} votes)"
                textSize = 14f
                setPadding(16, 16, 16, 16)
                radioGroup.addView(this)
            }
        }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(hint)
            addView(radioGroup)
        }

        AlertDialog.Builder(ctx)
            .setTitle("Select Favourite Player")
            .setView(container)
            .setPositiveButton("Confirm & Save") { dialog, _ ->
                val checked = radioGroup.checkedRadioButtonId
                if (checked == -1) {
                    Toast.makeText(ctx, "Please select a player", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveFavouritePlayer(candidates[checked].playerId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveFavouritePlayer(playerId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                api.setManOfTournament(tournamentId, playerId)
                Toast.makeText(requireContext(), "Favourite Player updated!", Toast.LENGTH_SHORT).show()
                pendingRefresh = false   // reload right now
                loadStats()
            } catch (e: Exception) {
                Log.e("StatsFragment", "saveFavouritePlayer error: ${e.message}", e)
                Toast.makeText(requireContext(), "Save failed, please try again", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun populatePomAwards(stats: TournamentStatsDto) {
        val awards = stats.allAwards.orEmpty()
        if (awards.isEmpty()) {
            binding.cardPomAwards.visibility = View.GONE
            return
        }
        binding.cardPomAwards.visibility = View.VISIBLE
        binding.rvPomAwards.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = PomAwardAdapter(awards)
            if (itemDecorationCount == 0) {
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    }

    private fun populateCricketUI(stats: TournamentStatsDto) {
        showCardHighestScore()
        showBowlersSection()

        binding.tvTopBatsmenTitle.text = "Top Batsmen"
        binding.tvTopBowlersTitle.text = "Top Bowlers"

        binding.headerBatsmen.apply {
            tvRuns.text  = "Runs"
            tvBalls.text = "Balls"
            tvFours.text = "4s"
            tvSixes.text = "6s"
            tvPom.text   = "POM"
            tvSixes.visibility = View.VISIBLE
            tvPom.visibility   = View.VISIBLE
        }
        binding.headerBowlers.apply {
            tvBalls.visibility   = View.VISIBLE
            tvEconomy.visibility = View.VISIBLE
            tvWickets.text = "Wkts"
            tvRuns.text    = "Runs"
            tvBalls.text   = "Balls"
            tvEconomy.text = "Eco"
            tvPom.text     = "POM"
        }

        binding.cardBestBatsman.tvLabel.text      = "Best Batsman"
        binding.cardBestBatsman.tvPlayerName.text = stats.bestBatsman?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topRunScorers.orEmpty().firstOrNull { it.playerId == stats.bestBatsman?.playerId }
                ?.let { "${it.runs} runs" } ?: stats.bestBatsman?.reason ?: "No Data"

        binding.cardBestBowler.tvLabel.text      = "Best Bowler"
        binding.cardBestBowler.tvPlayerName.text = stats.bestBowler?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text      = stats.bestBowler?.reason ?: "No Data"

        binding.cardHighestScore.tvLabel.text      = "Best Fielder"
        binding.cardHighestScore.tvPlayerName.text = stats.bestFielder?.playerName ?: "TBD"
        binding.cardHighestScore.tvValue.text      = stats.bestFielder?.reason ?: "No Data"

        val batsmen = stats.topRunScorers.orEmpty()
        if (batsmen.isNotEmpty()) {
            binding.rvTopBatsmen.layoutManager = LinearLayoutManager(context)
            binding.rvTopBatsmen.adapter = TournamentStatsAdapter(
                sportType    = TournamentStatsAdapter.SPORT_CRICKET,
                isBatting    = true,
                battingItems = batsmen
            )
        }

        val bowlers = stats.topBowlers.orEmpty()
        if (bowlers.isNotEmpty()) {
            binding.rvTopBowlers.layoutManager = LinearLayoutManager(context)
            binding.rvTopBowlers.adapter = TournamentStatsAdapter(
                sportType    = TournamentStatsAdapter.SPORT_CRICKET,
                isBatting    = false,
                bowlingItems = bowlers
            )
        } else {
            hideBowlersSection()
        }
        setupCricketCharts(stats)
    }

    private fun setupCricketCharts(stats: TournamentStatsDto) {
        binding.cardCricketCharts.visibility = android.view.View.VISIBLE

        // ── Bar Chart — Top 5 batsmen runs ──────────────────────────
        val batsmen = stats.topRunScorers.orEmpty().take(5)
        if (batsmen.isNotEmpty()) {
            val entries = batsmen.mapIndexed { i, p -> BarEntry(i.toFloat(), p.runs.toFloat()) }
            val dataSet = BarDataSet(entries, "Runs").apply {
                colors = listOf(
                    Color.parseColor("#E31212"), Color.parseColor("#FF4444"),
                    Color.parseColor("#FF6B6B"), Color.parseColor("#FF9999"),
                    Color.parseColor("#FFBBBB")
                )
                valueTextSize = 10f
                valueTextColor = Color.BLACK
            }
            binding.barChartBatsmen.applyCommonBar(BarData(dataSet), batsmen.map { it.playerName.take(9) })
        } else {
            binding.barChartBatsmen.clear()
            binding.barChartBatsmen.invalidate()
        }

        // ── Horizontal Bar Chart — Top 5 bowlers wickets ────────────
        val bowlers = stats.topBowlers.orEmpty().take(5)
        if (bowlers.isNotEmpty()) {
            val entries = bowlers.mapIndexed { i, p -> BarEntry(i.toFloat(), p.wickets.toFloat()) }
            val dataSet = BarDataSet(entries, "Wickets").apply {
                colors = listOf(
                    Color.parseColor("#1A1A2E"), Color.parseColor("#2D2D5E"),
                    Color.parseColor("#3F3F8F"), Color.parseColor("#5252B0"),
                    Color.parseColor("#6565D0")
                )
                valueTextSize = 10f
                valueTextColor = Color.BLACK
            }
            binding.barChartBowlers.applyCommonHorizontalBar(BarData(dataSet), bowlers.map { it.playerName.take(9) })
        } else {
            binding.barChartBowlers.clear()
            binding.barChartBowlers.invalidate()
        }
    }

    private fun BarChart.applyCommonBar(data: BarData, labels: List<String>) {
        data.barWidth = 0.6f
        description.isEnabled = false
        legend.isEnabled = false
        setDrawGridBackground(false)
        setFitBars(true)
        axisRight.isEnabled = false
        axisLeft.axisMinimum = 0f
        axisLeft.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        this.data = data
        animateY(600)
        invalidate()
    }

    private fun HorizontalBarChart.applyCommonHorizontalBar(data: BarData, labels: List<String>) {
        data.barWidth = 0.6f
        description.isEnabled = false
        legend.isEnabled = false
        setDrawGridBackground(false)
        axisRight.isEnabled = false
        axisLeft.axisMinimum = 0f
        axisLeft.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        this.data = data
        animateY(600)
        invalidate()
    }

    private fun populateFutsalUI(stats: TournamentStatsDto) {
        hideCardHighestScore()

        val goalScorers = stats.topGoalScorers.orEmpty()
        val assistants  = stats.topAssistants.orEmpty()

        binding.cardBestBatsman.tvLabel.text      = "Top Scorer"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName
                ?: goalScorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason
                ?: goalScorers.maxByOrNull { it.goals }?.let { "${it.goals} goals" } ?: "No Data"

        binding.cardBestBowler.tvLabel.text      = "Top Assist"
        binding.cardBestBowler.tvPlayerName.text =
            stats.topAssist?.playerName
                ?: assistants.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text      =
            stats.topAssist?.reason
                ?: assistants.maxByOrNull { it.assists }?.let { "${it.assists} assists" } ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Scorers"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Goals"
            tvBalls.text = "Asst"
            tvFours.text = "G+A"
            tvSixes.text = "🟨"
            tvPom.text   = "🟥"
            tvSixes.visibility = View.VISIBLE
            tvPom.visibility   = View.VISIBLE
        }
        if (goalScorers.isNotEmpty()) {
            binding.rvTopBatsmen.layoutManager = LinearLayoutManager(context)
            binding.rvTopBatsmen.adapter = TournamentStatsAdapter(
                sportType       = TournamentStatsAdapter.SPORT_FUTSAL,
                isBatting       = true,
                goalScorerItems = goalScorers
            )
        }

        if (assistants.isNotEmpty()) {
            showBowlersSection()
            binding.tvTopBowlersTitle.text = "Top Assisters"
            binding.headerBowlers.apply {
                tvBalls.visibility   = View.GONE
                tvEconomy.visibility = View.GONE
                tvWickets.text = "Asst"
                tvRuns.text    = "Goals"
                tvPom.text     = "G+A"
            }
            binding.rvTopBowlers.layoutManager = LinearLayoutManager(context)
            binding.rvTopBowlers.adapter = TournamentStatsAdapter(
                sportType      = TournamentStatsAdapter.SPORT_FUTSAL,
                isBatting      = false,
                assistantItems = assistants
            )
        } else {
            hideBowlersSection()
        }
        setupFutsalCharts(stats)
    }

    private fun setupFutsalCharts(stats:TournamentStatsDto) {
        binding.cardFutsalCharts.visibility = android.view.View.VISIBLE

        val scorers = stats.topGoalScorers.orEmpty().take(5)

        // ── Grouped Bar Chart — Goals (red) + Assists (green) ───────
        if (scorers.isNotEmpty()) {
            val goalEntries   = scorers.mapIndexed { i, p -> BarEntry(i.toFloat(), p.goals.toFloat()) }
            val assistEntries = scorers.mapIndexed { i, p -> BarEntry(i.toFloat(), p.assists.toFloat()) }

            val dsGoals   = BarDataSet(goalEntries, "Goals").apply { color = Color.parseColor("#E31212"); valueTextSize = 9f }
            val dsAssists = BarDataSet(assistEntries, "Assists").apply { color = Color.parseColor("#16A34A"); valueTextSize = 9f }

            val barData = BarData(dsGoals, dsAssists).apply {
                barWidth = 0.35f
                groupBars(-0.5f, 0.1f, 0.05f)
            }
            binding.barChartFutsalGoals.apply {
                data = barData
                xAxis.apply {
                    position         = XAxis.XAxisPosition.BOTTOM
                    valueFormatter   = IndexAxisValueFormatter(scorers.map { it.playerName.take(9) })
                    granularity      = 1f
                    setCenterAxisLabels(true)
                    setDrawGridLines(false)
                }
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                description.isEnabled = false
                setFitBars(true)
                animateY(800)
                invalidate()
            }
        }

        // ── Pie Chart — Goals share ──────────────────────────────────
        if (scorers.isNotEmpty()) {
            val pieEntries = scorers.map { PieEntry(it.goals.toFloat(), it.playerName.take(9)) }
            val pieDataSet = PieDataSet(pieEntries, "").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize   = 11f
                valueTextColor  = Color.WHITE
            }
            binding.pieChartFutsalShare.apply {
                data            = PieData(pieDataSet)
                isDrawHoleEnabled = true
                holeRadius      = 38f
                transparentCircleRadius = 43f
                description.isEnabled = false
                legend.isEnabled = true
                setEntryLabelColor(Color.WHITE)
                animateY(900)
                invalidate()
            }
        }
    }



    private fun populateVolleyballUI(stats: TournamentStatsDto) {
        hideCardHighestScore()

        val scorers = stats.topGoalScorers.orEmpty()
        val servers = stats.topAssistants.orEmpty()

        binding.cardBestBatsman.tvLabel.text      = "Top Scorer"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName ?: scorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} pts" } ?: "No Data"

        binding.cardBestBowler.tvLabel.text      = "Best Server"
        binding.cardBestBowler.tvPlayerName.text =
            stats.topAssist?.playerName ?: servers.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text      =
            stats.topAssist?.reason ?: servers.maxByOrNull { it.assists }?.let { "${it.assists} aces" } ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Point Scorers"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Points"
            tvBalls.text = "Aces"
            tvFours.text = "Blocks"
            tvSixes.text = "AErr"
            tvPom.text   = "Fant"
            tvSixes.visibility = View.VISIBLE
            tvPom.visibility   = View.VISIBLE
        }
        if (scorers.isNotEmpty()) {
            binding.rvTopBatsmen.layoutManager = LinearLayoutManager(context)
            binding.rvTopBatsmen.adapter = TournamentStatsAdapter(
                sportType       = TournamentStatsAdapter.SPORT_VOLLEYBALL,
                isBatting       = true,
                goalScorerItems = scorers
            )
        }

        if (servers.isNotEmpty()) {
            showBowlersSection()
            binding.tvTopBowlersTitle.text = "Best Servers (Aces)"
            binding.headerBowlers.apply {
                tvBalls.visibility   = View.GONE
                tvEconomy.visibility = View.GONE
                tvWickets.text = "Aces"
                tvRuns.text    = "Points"
                tvPom.text     = "Fant"
            }
            binding.rvTopBowlers.layoutManager = LinearLayoutManager(context)
            binding.rvTopBowlers.adapter = TournamentStatsAdapter(
                sportType      = TournamentStatsAdapter.SPORT_VOLLEYBALL,
                isBatting      = false,
                assistantItems = servers
            )
        } else {
            hideBowlersSection()
        }
    }

    private fun populateBadmintonUI(stats: TournamentStatsDto) {
        hideCardHighestScore()
        hideBowlersSection()

        val scorers = stats.topGoalScorers.orEmpty()

        binding.cardBestBatsman.tvLabel.text      = "Top Scorer"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName ?: scorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} pts" } ?: "No Data"

        binding.cardBestBowler.tvLabel.text      = "Top Attacker"
        binding.cardBestBowler.tvPlayerName.text =
            stats.topAssist?.playerName ?: scorers.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text      =
            stats.topAssist?.reason ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} smashes" } ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Scorers"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Points"
            tvBalls.text = "Smash+Ace"
            tvFours.text = "Faults"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        if (scorers.isNotEmpty()) {
            binding.rvTopBatsmen.layoutManager = LinearLayoutManager(context)
            binding.rvTopBatsmen.adapter = TournamentStatsAdapter(
                sportType       = TournamentStatsAdapter.SPORT_BADMINTON,
                isBatting       = true,
                goalScorerItems = scorers
            )
        }
    }

    private fun populateTableTennisUI(stats: TournamentStatsDto) {
        hideCardHighestScore()
        hideBowlersSection()

        val scorers = stats.topGoalScorers.orEmpty()

        binding.cardBestBatsman.tvLabel.text      = "Top Scorer"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName ?: scorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} pts" } ?: "No Data"

        binding.cardBestBowler.tvLabel.text      = "Top Attacker"
        binding.cardBestBowler.tvPlayerName.text =
            stats.topAssist?.playerName ?: scorers.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text      =
            stats.topAssist?.reason ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} smashes" } ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Scorers"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Points"
            tvBalls.text = "Smash+Ace"
            tvFours.text = "Faults"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        if (scorers.isNotEmpty()) {
            binding.rvTopBatsmen.layoutManager = LinearLayoutManager(context)
            binding.rvTopBatsmen.adapter = TournamentStatsAdapter(
                sportType       = TournamentStatsAdapter.SPORT_TABLETENNIS,
                isBatting       = true,
                goalScorerItems = scorers
            )
        }
    }

    private fun populateTugOfWarUI(stats: TournamentStatsDto) {
        hideCardHighestScore()
        hideBowlersSection()

        val scorers = stats.topGoalScorers.orEmpty()

        binding.cardBestBatsman.tvLabel.text      = "Top Team"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName ?: scorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} wins" } ?: "No Data"

        binding.cardBestBowler.tvLabel.text      = "Top Strength"
        binding.cardBestBowler.tvPlayerName.text =
            stats.topAssist?.playerName ?: scorers.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text      =
            stats.topAssist?.reason ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Leaderboard"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Wins"
            tvBalls.text = "Strength"
            tvFours.text = "POM"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        if (scorers.isNotEmpty()) {
            binding.rvTopBatsmen.layoutManager = LinearLayoutManager(context)
            binding.rvTopBatsmen.adapter = TournamentStatsAdapter(
                sportType       = TournamentStatsAdapter.SPORT_TUG_OF_WAR,
                isBatting       = true,
                goalScorerItems = scorers
            )
        }
    }

    private fun populateLudoUI(stats: TournamentStatsDto) {
        hideCardHighestScore()
        hideBowlersSection()

        val scorers = stats.topGoalScorers.orEmpty()

        binding.cardBestBatsman.tvLabel.text      = "Top Home Runs"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName ?: scorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} home runs" } ?: "No Data"

        binding.cardBestBowler.tvLabel.text      = "Top Captures"
        binding.cardBestBowler.tvPlayerName.text =
            stats.topAssist?.playerName ?: scorers.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text      =
            stats.topAssist?.reason ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} captures" } ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Players"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Home Runs"
            tvBalls.text = "Captures"
            tvFours.text = "POM"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        if (scorers.isNotEmpty()) {
            binding.rvTopBatsmen.layoutManager = LinearLayoutManager(context)
            binding.rvTopBatsmen.adapter = TournamentStatsAdapter(
                sportType       = TournamentStatsAdapter.SPORT_LUDO,
                isBatting       = true,
                goalScorerItems = scorers
            )
        }
    }

    private fun populateChessUI(stats: TournamentStatsDto) {
        hideCardHighestScore()
        hideBowlersSection()

        val scorers = stats.topGoalScorers.orEmpty()

        binding.cardBestBatsman.tvLabel.text      = "Most Wins"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName ?: scorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} wins" } ?: "No Data"

        binding.cardBestBowler.tvLabel.text      = "Most Checks"
        binding.cardBestBowler.tvPlayerName.text =
            stats.topAssist?.playerName ?: scorers.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text      =
            stats.topAssist?.reason ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} checks" } ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Leaderboard"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Wins"
            tvBalls.text = "Checks"
            tvFours.text = "POM"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        if (scorers.isNotEmpty()) {
            binding.rvTopBatsmen.layoutManager = LinearLayoutManager(context)
            binding.rvTopBatsmen.adapter = TournamentStatsAdapter(
                sportType       = TournamentStatsAdapter.SPORT_CHESS,
                isBatting       = true,
                goalScorerItems = scorers
            )
        }
    }

    private fun showBowlersSection() {
        binding.tvTopBowlersTitle.visibility  = View.VISIBLE
        binding.headerBowlers.root.visibility = View.VISIBLE
        binding.rvTopBowlers.visibility       = View.VISIBLE
    }

    private fun hideBowlersSection() {
        binding.tvTopBowlersTitle.visibility  = View.GONE
        binding.headerBowlers.root.visibility = View.GONE
        binding.rvTopBowlers.visibility       = View.GONE
    }

    private fun showCardHighestScore() {
        binding.cardHighestScore.root.visibility = View.VISIBLE
        val p = binding.cardHighestScore.root.layoutParams as LinearLayout.LayoutParams
        p.weight = 1f; p.width = 0
        binding.cardHighestScore.root.layoutParams = p
    }

    private fun hideCardHighestScore() {
        binding.cardHighestScore.root.visibility = View.GONE
        val p = binding.cardHighestScore.root.layoutParams as LinearLayout.LayoutParams
        p.weight = 0f; p.width = 0
        binding.cardHighestScore.root.layoutParams = p
    }

    companion object {
        fun newInstance(
            tournamentId: Long,
            sportId: Long = -1L,
            sportName: String = ""
        ): StatsFragment = StatsFragment().apply {
            arguments = Bundle().apply {
                putLong("tournamentId", tournamentId)
                putLong("sportId", sportId)
                putString("sportName", sportName)
            }
        }

        private const val SPORT_CRICKET      = "cricket"
        private const val SPORT_FUTSAL       = "futsal"
        private const val SPORT_VOLLEYBALL   = "volleyball"
        private const val SPORT_BADMINTON    = "badminton"
        private const val SPORT_TABLE_TENNIS = "table_tennis"
        private const val SPORT_TUG_OF_WAR   = "tug_of_war"
        private const val SPORT_LUDO         = "ludo"
        private const val SPORT_CHESS        = "chess"
    }

    override fun onResume() {
        super.onResume()
        if (tournamentId != -1L && hasLoadedOnce) loadStats()
    }
}