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


class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private var tournamentId: Long = -1L
    private var sportId: Long      = -1L
    private var sportName: String  = ""

    private val isAdmin: Boolean by lazy {
        val prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) // "user_prefs" → "MyPrefs"
        prefs.getString("role", "").equals("ADMIN", ignoreCase = true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)

        tournamentId = arguments?.getLong("tournamentId") ?: -1L
        sportId      = arguments?.getLong("sportId")      ?: -1L
        sportName    = arguments?.getString("sportName").orEmpty()

        // Admin edit button — same as JS isAdmin check
        binding.btnEditMot.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.btnEditMot.setOnClickListener { openMotEditDialog() }

        if (tournamentId != -1L) loadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Data Loading — mirrors JS loadStats() ─────────────────────────

    private fun loadStats(retryCount: Int = 0) {
        setLoading(true)
        binding.tvEmptyState.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val stats = api.getTournamentStats(tournamentId)
                populateUI(stats)
            } catch (e: Exception) {
                if (e is java.net.SocketTimeoutException && retryCount < 2) {
                    delay(2000)
                    loadStats(retryCount + 1)  // 2 baar retry
                } else {
                    Log.e("StatsFragment", "loadStats error: ${e.message}", e)
                    showError()
                }
            } finally {
                setLoading(false)
            }
        }
    }

    // ── UI State ──────────────────────────────────────────────────────

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError() {
        if (_binding == null) return
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = "No data available"
    }

    // ── Main populate — mirrors JS switch(stats.sport) ────────────────

    private fun populateUI(stats: TournamentStatsDto) {
        // 1. Man of Tournament (always)
        setupManOfTournament(stats)

        // 2. Sport switch — same as JS switch(stats.sport)
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

        // 3. POM awards (always)
        populatePomAwards(stats)
    }

    // ── detectSport — handles ALL variants (space, underscore, id) ────

    private fun detectSport(stats: TournamentStatsDto): String {
        // Priority 1: sportId (from fragment args or API response)
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

        // Priority 2: sport name string — same as JS switch(stats.sport)
        // JS: case "table tennis": case "tabletennis": → so handle all variants
        val name = (stats.sport ?: sportName).lowercase(Locale.US).trim()
        return when {
            name == SPORT_FUTSAL                              -> SPORT_FUTSAL
            name == SPORT_VOLLEYBALL                          -> SPORT_VOLLEYBALL
            name == SPORT_CRICKET                             -> SPORT_CRICKET
            name == SPORT_BADMINTON                           -> SPORT_BADMINTON
            // JS handles "table tennis" AND "tabletennis" — mirror that
            name == "table tennis" || name == "tabletennis"
                    || name == SPORT_TABLE_TENNIS             -> SPORT_TABLE_TENNIS
            name == "tug of war" || name == SPORT_TUG_OF_WAR -> SPORT_TUG_OF_WAR
            name == SPORT_LUDO                                -> SPORT_LUDO
            name == SPORT_CHESS                               -> SPORT_CHESS
            // Partial match fallback
            name.contains("futsal")                           -> SPORT_FUTSAL
            name.contains("volleyball")                       -> SPORT_VOLLEYBALL
            name.contains("badminton")                        -> SPORT_BADMINTON
            name.contains("table")                            -> SPORT_TABLE_TENNIS
            name.contains("tug")                              -> SPORT_TUG_OF_WAR
            name.contains("ludo")                             -> SPORT_LUDO
            name.contains("chess")                            -> SPORT_CHESS
            // Priority 3: infer from data shape (same as JS default → cricket)
            stats.topGoalScorers.orEmpty().isNotEmpty()
                    || stats.topAssistants.orEmpty().isNotEmpty() -> SPORT_FUTSAL
            else -> SPORT_CRICKET
        }
    }

    // ── Man of Tournament — mirrors JS ManOfTournament component ──────

    private fun setupManOfTournament(stats: TournamentStatsDto) {
        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"
    }

    private fun openMotEditDialog() {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val candidates: List<TopVotedPlayerDto> = api.getTopVotedPlayers(tournamentId)
                setLoading(false)

                if (candidates.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "No voted players found for this tournament", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                showMotSelectionDialog(candidates)

            } catch (e: Exception) {
                setLoading(false)
                Log.e("StatsFragment", "getTopVotedPlayers error: ${e.message}", e)
                Toast.makeText(requireContext(),
                    "Failed to load players, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMotSelectionDialog(candidates: List<TopVotedPlayerDto>) {
        val ctx = requireContext()

        val radioGroup = RadioGroup(ctx).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val hint = TextView(ctx).apply {
            text = "Top 3 favourite players by fan votes"
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray))
            setPadding(48, 16, 48, 8)
        }

        candidates.forEachIndexed { index, candidate ->
            RadioButton(ctx).apply {
                id = index
                text = "#${index + 1}  ${candidate.playerName}  (${candidate.votes} votes)"
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
            .setTitle("Select Man of the Tournament")
            .setView(container)
            .setPositiveButton("Confirm & Save") { dialog, _ ->
                val checkedIndex = radioGroup.checkedRadioButtonId
                if (checkedIndex == -1) {
                    Toast.makeText(ctx, "Please select a player", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveManOfTournament(candidates[checkedIndex].playerId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveManOfTournament(playerId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                api.setManOfTournament(tournamentId, playerId)  // ab crash nahi hoga
                Toast.makeText(requireContext(), "Man of the Tournament updated!", Toast.LENGTH_SHORT).show()
                loadStats()
            } catch (e: Exception) {
                Log.e("StatsFragment", "setManOfTournament error: ${e.message}", e)
                Toast.makeText(requireContext(), "Save failed, please try again", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    // ── POM Awards — mirrors JS PomList component ─────────────────────

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

    // ── Cricket — mirrors JS CricketStats ────────────────────────────
    // Columns: Batsmen → Runs | Balls | 4s | 6s | POM
    //          Bowlers → Wkts | Runs(runsConceded) | Balls | Eco | POM

    private fun populateCricketUI(stats: TournamentStatsDto) {
        showCardHighestScore()
        showBowlersSection()

        binding.tvTopBatsmenTitle.text = "Top Batsmen"
        binding.tvTopBowlersTitle.text = "Top Bowlers"

        // Header labels — same as JS columns
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

        // Award cards
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

        // RecyclerViews
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
    }

    // ── Futsal — mirrors JS FutsalStats ──────────────────────────────
    // Scorers:  Goals | Assists | G+A | YC | RC
    // Assisters: Assists | Goals | G+A

    private fun populateFutsalUI(stats: TournamentStatsDto) {
        hideCardHighestScore()

        val goalScorers = stats.topGoalScorers.orEmpty()
        val assistants  = stats.topAssistants.orEmpty()   // @SerializedName handles topAssisters too

        // Award cards — same as JS AwardCard
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

        // Scorers leaderboard
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

        // Assisters leaderboard — same as JS topAssisters?.length > 0
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
    }

    // ── Volleyball — mirrors JS VolleyballStats ───────────────────────
    // Scorers: Points | Aces | Blocks
    // Servers: Aces | Points | Fantasy

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

    // ── Badminton — mirrors JS BadmintonStats ─────────────────────────
    // Columns: Points | Smashes+Aces | Faults

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

    // ── Table Tennis — mirrors JS TableTennisStats ────────────────────
    // Columns: Points | Smashes+Aces | Faults

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

    // ── Tug of War ────────────────────────────────────────────────────

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

    // ── Ludo — mirrors JS LudoStats ───────────────────────────────────
    // Columns: Home Runs | Captures

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

    // ── Chess — mirrors JS ChessStats ─────────────────────────────────
    // Columns: Wins | Checks | POM

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

    // ── Section visibility helpers ────────────────────────────────────

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

    // ── Companion ─────────────────────────────────────────────────────

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
        private const val SPORT_TUG_OF_WAR  = "tug_of_war"
        private const val SPORT_LUDO         = "ludo"
        private const val SPORT_CHESS        = "chess"
    }

    override fun onResume() {
        super.onResume()
        if (tournamentId != -1L) loadStats()
    }
}