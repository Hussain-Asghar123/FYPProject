package com.example.fypproject.Fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.PlayerAdapter
import com.example.fypproject.Adapter.TeamPlayerAdapter
import com.example.fypproject.DTO.Player
import com.example.fypproject.DTO.TeamStatsDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.FragmentTeamDetailBinding
import kotlinx.coroutines.launch
import java.util.Locale

class TeamDetailFragment : Fragment(R.layout.fragment_team_detail) {

    private var _binding: FragmentTeamDetailBinding? = null
    private val binding get() = _binding!!

    private var teamId: Long = -1L
    private var teamName: String = ""

    // ── which view is active ──────────────────────────────────────────────────
    private var showingPlayers = true

    // ── onViewCreated mein retrieve karo ────────────────────────────────────────
    private var creatorPlayerId: Long = -1L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTeamDetailBinding.bind(view)

        teamId   = arguments?.getLong(ARG_TEAM_ID)    ?: -1L
        teamName = arguments?.getString(ARG_TEAM_NAME).orEmpty()

        binding.tvTeamDetailName.text = teamName
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnShowPlayers.setOnClickListener { switchToPlayers() }
        binding.btnShowStats.setOnClickListener   { switchToStats()   }
        switchToPlayers()
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Toggle helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun switchToPlayers() {
        showingPlayers = true
        updateButtonStyles()
        binding.layoutPlayers.visibility = View.VISIBLE
        binding.scrollStats.visibility   = View.GONE
        loadPlayers()
    }

    private fun switchToStats() {
        showingPlayers = false
        updateButtonStyles()
        binding.layoutPlayers.visibility = View.GONE
        binding.scrollStats.visibility   = View.VISIBLE
        loadStats()
    }

    private fun updateButtonStyles() {
        val primary = ContextCompat.getColor(requireContext(), R.color.primaryColor)
        val grey    = android.graphics.Color.DKGRAY
        val white   = ContextCompat.getColor(requireContext(), android.R.color.white)
        val black   = ContextCompat.getColor(requireContext(), android.R.color.black)

        if (showingPlayers) {
            binding.btnShowPlayers.backgroundTintList = ColorStateList.valueOf(primary)
            binding.btnShowStats.backgroundTintList   = ColorStateList.valueOf(grey)
            binding.btnShowPlayers.setTextColor(white)
            binding.btnShowStats.setTextColor(black)
        } else {
            binding.btnShowStats.backgroundTintList   = ColorStateList.valueOf(primary)
            binding.btnShowPlayers.backgroundTintList = ColorStateList.valueOf(grey)
            binding.btnShowStats.setTextColor(white)
            binding.btnShowPlayers.setTextColor(black)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Players
    // ─────────────────────────────────────────────────────────────────────────

    // ── loadPlayers — PlayerAdapter use karo ─────────────────────────────────────
    private fun loadPlayers() {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val players = api.getTeamPlayers(teamId)   // returns List<TeamPlayerDto>
                setLoading(false)
                if (players.isEmpty()) {
                    binding.tvPlayersEmpty.visibility = View.VISIBLE
                    binding.rvTeamPlayers.visibility  = View.GONE
                } else {
                    binding.tvPlayersEmpty.visibility = View.GONE
                    binding.rvTeamPlayers.visibility  = View.VISIBLE

                    // ✅ TeamPlayerAdapter use karo — PlayerAdapter NAHI
                    binding.rvTeamPlayers.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvTeamPlayers.adapter = TeamPlayerAdapter(players)

                    if (binding.rvTeamPlayers.itemDecorationCount == 0)
                        binding.rvTeamPlayers.addItemDecoration(
                            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                        )
                }
            } catch (e: Exception) {
                setLoading(false)
                Log.e("TeamDetail", "loadPlayers: ${e.message}", e)
                binding.tvPlayersEmpty.visibility = View.VISIBLE
                binding.tvPlayersEmpty.text = "Failed to load players"
                binding.rvTeamPlayers.visibility = View.GONE
            }
        }
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Team Stats
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadStats() {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val stats = api.getTeamStats(teamId)
                setLoading(false)
                populateStats(stats)
            } catch (e: Exception) {
                setLoading(false)
                Log.e("TeamDetail", "loadStats: ${e.message}", e)
                binding.tvStatsEmpty.visibility = View.VISIBLE
                binding.statsContainer.visibility = View.GONE
            }
        }
    }

    private fun populateStats(stats: TeamStatsDto) {
        binding.tvStatsEmpty.visibility   = View.GONE
        binding.statsContainer.visibility = View.VISIBLE

        // Sport label
        binding.tvStatsSportLabel.text =
            (stats.sport ?: "Unknown").replaceFirstChar { it.uppercase(Locale.US) }

        // Normalize sport once for use in conditional logic (e.g., top performer visibility)
        val normalized = normalizeSport(stats.sport, stats.sportId)
        Log.d("TeamDetail", "Sport: ${stats.sport}, SportId: ${stats.sportId}, Normalized: $normalized")

        // Match record — common for all sports
        setStatRow(binding.tvMatchesPlayed,  "Matches Played", stats.matchesPlayed.toString())
        setStatRow(binding.tvWins,           "Wins",           stats.wins.toString())
        setStatRow(binding.tvLosses,         "Losses",         stats.losses.toString())
        setStatRow(binding.tvDraws,          "Draws",          stats.draws.toString())


        if (normalized != SPORT_CHESS  && normalized != SPORT_TUG_OF_WAR && stats.topScorerName != null) {
            binding.cardTopPerformer.visibility = View.VISIBLE
            binding.tvTopPerformerName.text = stats.topScorerName
            binding.tvTopPerformerStat.text = stats.topScorerStat ?: ""
        } else {
            binding.cardTopPerformer.visibility = View.GONE
        }


        // Hide all sport sections first (use included bindings' root views)
        binding.sectionCricket.root.visibility    = View.GONE
        binding.sectionFutsal.root.visibility     = View.GONE
        binding.sectionVolleyball.root.visibility = View.GONE
        binding.sectionBadminton.root.visibility  = View.GONE
        binding.sectionLudo.root.visibility       = View.GONE
        binding.sectionChess.root.visibility      = View.GONE
        binding.sectionTugOfWar.root.visibility   = View.GONE

        when (normalizeSport(stats.sport, stats.sportId)) {
            SPORT_CRICKET      -> populateCricketStats(stats)
            SPORT_FUTSAL       -> populateFutsalStats(stats)
            SPORT_VOLLEYBALL   -> populateVolleyballStats(stats)
            SPORT_BADMINTON    -> populateBadmintonStats(stats)
            SPORT_TABLE_TENNIS -> populateBadmintonStats(stats)
            SPORT_LUDO         -> populateLudoStats(stats)
            SPORT_CHESS        -> populateChessStats(stats)
            SPORT_TUG_OF_WAR   -> populateTugOfWarStats(stats)
            else               -> populateCricketStats(stats)
        }
    }

    // ── sport sections ────────────────────────────────────────────────────────

    private fun populateCricketStats(stats: TeamStatsDto) {
        binding.sectionCricket.root.visibility = View.VISIBLE
        binding.sectionCricket.tvCricketRuns.text = "Total Runs: ${stats.totalRunsScored}"
        binding.sectionCricket.tvCricketWickets.text = "Wickets: ${stats.totalWicketsTaken}"
        binding.sectionCricket.tvCricketFours.text = "Fours: ${stats.totalFours}"
        binding.sectionCricket.tvCricketSixes.text = "Sixes: ${stats.totalSixes}"
        binding.sectionCricket.tvCricketHighest.text = "Highest Score: ${stats.highestTeamScore}"
        binding.sectionCricket.tvCricketCatches.text = "Catches: ${stats.totalCatches}"
    }

    private fun populateFutsalStats(stats: TeamStatsDto) {
        binding.sectionFutsal.root.visibility = View.VISIBLE
        binding.sectionFutsal.tvFutsalGoals.text   = "Goals: ${stats.totalGoals}"
        binding.sectionFutsal.tvFutsalAssists.text = "Assists: ${stats.totalGoals -3}"
        binding.sectionFutsal.tvFutsalGplusA.text  = "G+A: ${stats.totalGoals + stats.totalAssists}"
        binding.sectionFutsal.tvFutsalFouls.text   = "Fouls: ${stats.totalFouls}"
        binding.sectionFutsal.tvFutsalYellow.text  = "Yellow Cards: ${stats.totalYellowCards}"
        binding.sectionFutsal.tvFutsalRed.text     = "Red Cards: ${stats.totalRedCards}"
    }

    private fun populateVolleyballStats(stats: TeamStatsDto) {
        binding.sectionVolleyball.root.visibility = View.VISIBLE
        binding.sectionVolleyball.tvVbPoints.text  = "Points: ${stats.totalPoints .takeIf { it > 0 } ?: stats.totalGoals}"
        binding.sectionVolleyball.tvVbAces.text    = "Aces: ${stats.totalAces .takeIf { it > 0 } ?: stats.totalAssists}"
        binding.sectionVolleyball.tvVbBlocks.text  = "Blocks: ${stats.totalBlocks .takeIf { it > 0 } ?: stats.totalFouls}"
        binding.sectionVolleyball.tvVbAtkErr.text  = "Attack Errors: ${stats.totalAttackErrors .takeIf { it > 0 } ?: stats.totalYellowCards}"
        binding.sectionVolleyball.tvVbSvcErr.text  = "Service Errors: ${stats.totalServiceErrors .takeIf { it > 0 } ?: stats.totalRedCards}"
    }

    private fun populateBadmintonStats(stats: TeamStatsDto) {
        binding.sectionBadminton.root.visibility = View.VISIBLE
        // reuse totalPoints / totalSmashes / totalFaults
        binding.sectionBadminton.tvBdPoints.text  = "Points: ${stats.totalPoints.takeIf { it > 0 } ?: stats.totalGoals}"
        binding.sectionBadminton.tvBdSmashes.text = "Smashes: ${stats.totalSmashes.takeIf { it > 0 } ?: stats.totalAssists}"
        binding.sectionBadminton.tvBdFaults.text  = "Faults: ${stats.totalFaults.takeIf { it > 0 } ?: stats.totalFouls}"
        binding.sectionBadminton.tvBdOutShots.text = "Out Shots: ${stats.totalOutShots.takeIf { it > 0 } ?: stats.totalYellowCards}"
    }

    private fun populateLudoStats(stats: TeamStatsDto) {
        binding.sectionLudo.root.visibility = View.VISIBLE
        binding.sectionLudo.tvLudoHomeRuns.text = "Home Runs: ${stats.totalHomeRuns.takeIf { it > 0 } ?: stats.totalGoals}"
    }

    private fun populateChessStats(stats: TeamStatsDto) {
        binding.sectionChess.root.visibility = View.VISIBLE
        binding.sectionChess.tvChessWins.text   = "Wins: ${stats.wins}"
        binding.sectionChess.tvChessChecks.text = "Checks: ${stats.totalChecks.takeIf { it > 0 } ?: stats.totalAssists}"
        val winRate = if (stats.matchesPlayed > 0)
            "${((stats.wins.toDouble() / stats.matchesPlayed) * 100).toInt()}%"
        else "—"
        binding.sectionChess.tvChessWinRate.text = "Win Rate: $winRate"
    }

    private fun populateTugOfWarStats(stats: TeamStatsDto) {
        binding.sectionTugOfWar.root.visibility = View.VISIBLE
        binding.sectionTugOfWar.tvTowWins.text       = "Wins: ${stats.wins}"
        binding.sectionTugOfWar.tvTowLosses.text     = "Loses: ${stats.losses}"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun setStatRow(tv: TextView, label: String, value: String) {
        tv.text = "$label: $value"
    }

    private fun setLoading(show: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun normalizeSport(sport: String?, sportId: Long?): String {
        // First, normalize by sport name as it's the most reliable source
        val name = sport?.lowercase(Locale.US)?.trim() ?: ""
        val byName = when {
            name.contains("futsal")      -> SPORT_FUTSAL
            name.contains("volleyball")  -> SPORT_VOLLEYBALL
            name.contains("badminton")   -> SPORT_BADMINTON
            name.contains("table")       -> SPORT_TABLE_TENNIS
            (name.contains("tug") && name.contains("war")) -> SPORT_TUG_OF_WAR
            name.contains("ludo")        -> SPORT_LUDO
            name.contains("chess")       -> SPORT_CHESS
            else                         -> null
        }

        // If we found a match by name, prefer that
        if (byName != null) {
            return byName
        }

        // Fall back to sportId if name matching didn't work
        sportId?.let {
            return when (it) {
                1L -> SPORT_CRICKET
                2L -> SPORT_FUTSAL
                3L -> SPORT_VOLLEYBALL
                4L -> SPORT_BADMINTON
                5L -> SPORT_TABLE_TENNIS
                6L -> SPORT_TUG_OF_WAR
                7L -> SPORT_LUDO
                8L -> SPORT_CHESS
                else -> SPORT_CRICKET
            }
        }

        return SPORT_CRICKET
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        private const val ARG_TEAM_ID   = "teamId"
        private const val ARG_TEAM_NAME = "teamName"

        private const val SPORT_CRICKET      = "cricket"
        private const val SPORT_FUTSAL       = "futsal"
        private const val SPORT_VOLLEYBALL   = "volleyball"
        private const val SPORT_BADMINTON    = "badminton"
        private const val SPORT_TABLE_TENNIS = "table_tennis"
        private const val SPORT_TUG_OF_WAR   = "tug_of_war"
        private const val SPORT_LUDO         = "ludo"
        private const val SPORT_CHESS        = "chess"

        fun newInstance(teamId: Long, teamName: String): TeamDetailFragment =
            TeamDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TEAM_ID, teamId)
                    putString(ARG_TEAM_NAME, teamName)
                }
            }
    }
}