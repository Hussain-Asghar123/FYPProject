package com.example.fypproject.Fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TournamentStatsAdapter
import com.example.fypproject.DTO.TournamentStatsDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch
import java.util.Locale

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private var tournamentId: Long = -1L
    private var sportId: Long = -1L
    private var sportName: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)
        tournamentId = arguments?.getLong("tournamentId") ?: -1L
        sportId      = arguments?.getLong("sportId") ?: -1L
        sportName    = arguments?.getString("sportName").orEmpty()
        if (tournamentId != -1L) fetchTournamentStats(tournamentId)
    }

    private fun fetchTournamentStats(id: Long) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val stats = api.getTournamentStats(id)
                populateUI(stats)
            } catch (e: Exception) {
                Log.e("StatsFragment", "Error: ${e.message}", e)
                showError()
            } finally {
                setLoading(false)
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
        when (detectSport(stats)) {
            SPORT_FUTSAL     -> populateFutsalUI(stats)
            SPORT_VOLLEYBALL -> populateVolleyballUI(stats)
            SPORT_BADMINTON->populateBadmintonUI(stats)
            SPORT_TABLE_TENNIS->populateTableTennisUI(stats)
            SPORT_TUG_OF_WAR->populateTugOfWarUI(stats)
            SPORT_LUDO  -> populateLudoUI(stats)
            SPORT_CHESS -> populateChessUI(stats)
            else             -> populateCricketUI(stats)
        }
    }

    private fun populateCricketUI(stats: TournamentStatsDto) {
        showCardHighestScore()
        showBowlersSection()

        binding.tvTopBatsmenTitle.text = "Top Batsmen"
        binding.tvTopBowlersTitle.text = "Top Bowlers"

        binding.headerBatsmen.apply {
            tvRuns.text = "Runs"; tvBalls.text = "Balls"
            tvFours.text = "4s"; tvSixes.text = "6s"; tvPom.text = "POM"
        }
        binding.headerBowlers.apply {
            tvBalls.visibility   = View.VISIBLE
            tvEconomy.visibility = View.VISIBLE
            tvWickets.text = "Wkts"; tvRuns.text = "Runs"
            tvBalls.text = "Balls"; tvEconomy.text = "Eco"; tvPom.text = "POM"
        }

        binding.tvManOfTournament.text             = stats.manOfTournament?.playerName ?: "TBD"
        binding.cardBestBatsman.tvLabel.text       = "Best Batsman"
        binding.cardBestBatsman.tvPlayerName.text  = stats.bestBatsman?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text       = "${stats.topRunScorers.orEmpty().firstOrNull { it.playerId == stats.bestBatsman?.playerId }?.runs ?: 0} runs"
        binding.cardBestBowler.tvLabel.text        = "Best Bowler"
        binding.cardBestBowler.tvPlayerName.text   = stats.bestBowler?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text        = stats.bestBowler?.reason ?: "No Data"
        binding.cardHighestScore.tvLabel.text      = "Best Fielder"
        binding.cardHighestScore.tvPlayerName.text = stats.bestFielder?.playerName ?: "TBD"
        binding.cardHighestScore.tvValue.text      = stats.bestFielder?.reason ?: "No Data"

        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
                sportType    = TournamentStatsAdapter.SPORT_CRICKET,
                isBatting    = true,
                battingItems = stats.topRunScorers.orEmpty()
            )
        }
        binding.rvTopBowlers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
                sportType    = TournamentStatsAdapter.SPORT_CRICKET,
                isBatting    = false,
                bowlingItems = stats.topBowlers.orEmpty()
            )
        }
    }

    private fun populateFutsalUI(stats: TournamentStatsDto) {
        hideCardHighestScore()

        val goalScorers = stats.topGoalScorers.orEmpty()
        val assistants  = stats.topAssistants.orEmpty()

        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"

        binding.cardBestBatsman.tvLabel.text      = "Top Scorer"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName
                ?: goalScorers.maxByOrNull { it.goals }?.playerName
                        ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason
                ?: goalScorers.maxByOrNull { it.goals }?.let { "${it.goals} goals" }
                        ?: "No Data"

        binding.cardBestBowler.tvLabel.text       = "Top Assist"
        binding.cardBestBowler.tvPlayerName.text  =
            stats.topAssist?.playerName
                ?: assistants.maxByOrNull { it.assists }?.playerName
                        ?: "TBD"
        binding.cardBestBowler.tvValue.text       =
            stats.topAssist?.reason
                ?: assistants.maxByOrNull { it.assists }?.let { "${it.assists} assists" }
                        ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Scorers"
        binding.headerBatsmen.apply {
            tvRuns.text = "Goals"; tvBalls.text = "Asst"
            tvFours.text = "G+A"; tvSixes.text = "YC"; tvPom.text = "RC"
        }
        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
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
                tvWickets.text = "Asst"; tvRuns.text = "Goals"; tvPom.text = "G+A"
            }
            binding.rvTopBowlers.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = TournamentStatsAdapter(
                    sportType      = TournamentStatsAdapter.SPORT_FUTSAL,
                    isBatting      = false,
                    assistantItems = assistants
                )
            }
        } else {
            hideBowlersSection()
        }
    }

    private fun populateVolleyballUI(stats: TournamentStatsDto) {
        hideCardHighestScore()

        val scorers = stats.topGoalScorers.orEmpty()
        val servers = stats.topAssistants.orEmpty()

        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"


        binding.cardBestBatsman.tvLabel.text      = "Top Scorer"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName
                ?: scorers.maxByOrNull { it.goals }?.playerName
                        ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason
                ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} pts" }
                        ?: "No Data"

        binding.cardBestBowler.tvLabel.text       = "Best Server"
        binding.cardBestBowler.tvPlayerName.text  =
            stats.topAssist?.playerName
                ?: servers.maxByOrNull { it.assists }?.playerName
                        ?: "TBD"
        binding.cardBestBowler.tvValue.text       =
            stats.topAssist?.reason
                ?: servers.maxByOrNull { it.assists }?.let { "${it.assists} aces" }
                        ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Scorers (Attack Points)"
        binding.headerBatsmen.apply {
            tvRuns.text = "Pts"; tvBalls.text = "Aces"
            tvFours.text = "Blk"; tvSixes.text = "AErr"; tvPom.text = "Fant"
        }
        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
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
                tvWickets.text = "Aces"; tvRuns.text = "Pts"; tvPom.text = "Fant"
            }
            binding.rvTopBowlers.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = TournamentStatsAdapter(
                    sportType      = TournamentStatsAdapter.SPORT_VOLLEYBALL,
                    isBatting      = false,
                    assistantItems = servers
                )
            }
        } else {
            hideBowlersSection()
        }
    }

    private fun populateBadmintonUI(stats: TournamentStatsDto) {
        hideCardHighestScore()
        hideBowlersSection()

        val scorers = stats.topGoalScorers.orEmpty()

        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"

        binding.cardBestBatsman.tvLabel.text      = "Top Scorer"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName
                ?: scorers.maxByOrNull { it.goals }?.playerName
                        ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason
                ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} pts" }
                        ?: "No Data"

        binding.cardBestBowler.tvLabel.text       = "Top Attacker"
        binding.cardBestBowler.tvPlayerName.text  =
            stats.topAssist?.playerName
                ?: scorers.maxByOrNull { it.assists }?.playerName
                        ?: "TBD"
        binding.cardBestBowler.tvValue.text       =
            stats.topAssist?.reason
                ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} smashes" }
                        ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Scorers"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Pts"
            tvBalls.text = "Smash+Ace"
            tvFours.text = "Faults"
            tvSixes.visibility = View.GONE
            tvPom.visibility = View.GONE
        }
        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
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

        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"

        binding.cardBestBatsman.tvLabel.text      = "Top Scorer"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName
                ?: scorers.maxByOrNull { it.goals }?.playerName
                        ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason
                ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} pts" }
                        ?: "No Data"

        binding.cardBestBowler.tvLabel.text       = "Top Attacker"
        binding.cardBestBowler.tvPlayerName.text  =
            stats.topAssist?.playerName
                ?: scorers.maxByOrNull { it.assists }?.playerName
                        ?: "TBD"
        binding.cardBestBowler.tvValue.text       =
            stats.topAssist?.reason
                ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} smashes" }
                        ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Scorers"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Pts"
            tvBalls.text = "Smash+Ace"
            tvFours.text = "Faults"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
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

        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"

        binding.cardBestBatsman.tvLabel.text      = "Best Team"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName
                ?: scorers.maxByOrNull { it.goals }?.playerName
                        ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason
                ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} rounds won" }
                        ?: "No Data"

        binding.cardBestBowler.tvLabel.text       = "Most Dominant"
        binding.cardBestBowler.tvPlayerName.text  =
            stats.topAssist?.playerName
                ?: scorers.maxByOrNull { it.assists }?.playerName
                        ?: "TBD"
        binding.cardBestBowler.tvValue.text       =
            stats.topAssist?.reason
                ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} matches won" }
                        ?: "No Data"

        binding.tvTopBatsmenTitle.text = "Top Performers"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Rounds Won"
            tvBalls.text = "Matches Won"
            tvFours.text = "POM"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
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
        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"
        binding.cardBestBatsman.tvLabel.text      = "Top Home Runs"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName ?: scorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} home runs" } ?: "No Data"
        binding.cardBestBowler.tvLabel.text       = "Top Captures"
        binding.cardBestBowler.tvPlayerName.text  =
            stats.topAssist?.playerName ?: scorers.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text       =
            stats.topAssist?.reason ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} captures" } ?: "No Data"
        binding.tvTopBatsmenTitle.text = "Top Players"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Home Runs"
            tvBalls.text = "Captures"
            tvFours.text = "POM"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
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
        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"
        binding.cardBestBatsman.tvLabel.text      = "Most Wins"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.topScorer?.playerName ?: scorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text      =
            stats.topScorer?.reason ?: scorers.maxByOrNull { it.goals }?.let { "${it.goals} wins" } ?: "No Data"
        binding.cardBestBowler.tvLabel.text       = "Most Checks"
        binding.cardBestBowler.tvPlayerName.text  =
            stats.topAssist?.playerName ?: scorers.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text       =
            stats.topAssist?.reason ?: scorers.maxByOrNull { it.assists }?.let { "${it.assists} checks" } ?: "No Data"
        binding.tvTopBatsmenTitle.text = "Leaderboard"
        binding.headerBatsmen.apply {
            tvRuns.text  = "Wins"
            tvBalls.text = "Checks"
            tvFours.text = "POM"
            tvSixes.visibility = View.GONE
            tvPom.visibility   = View.GONE
        }
        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
                sportType       = TournamentStatsAdapter.SPORT_CHESS,
                isBatting       = true,
                goalScorerItems = scorers
            )
        }
    }



    // ── HELPERS ─────────────────────────────────────────────────────
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
        val p = binding.cardHighestScore.root.layoutParams as LinearLayout.LayoutParams
        p.weight = 0f; p.width = 0
        binding.cardHighestScore.root.layoutParams = p
        binding.cardHighestScore.root.visibility = View.GONE
    }

    private fun detectSport(stats: TournamentStatsDto): String {
        val fromId = when (sportId.takeIf { it > 0 } ?: stats.sportId) {
            1L -> SPORT_CRICKET
            2L -> SPORT_FUTSAL
            3L -> SPORT_VOLLEYBALL
            4L -> SPORT_BADMINTON
            5L-> SPORT_TABLE_TENNIS
            6L-> SPORT_TUG_OF_WAR
            7L -> SPORT_LUDO
            8L -> SPORT_CHESS
            else -> null
        }
        if (fromId != null) return fromId

        val name = (stats.sport ?: sportName).lowercase(Locale.US)
        return when {
            name.contains(SPORT_FUTSAL)     -> SPORT_FUTSAL
            name.contains(SPORT_VOLLEYBALL) -> SPORT_VOLLEYBALL
            name.contains(SPORT_CRICKET)    -> SPORT_CRICKET
            name.contains(SPORT_BADMINTON)   -> SPORT_BADMINTON
            name.contains(SPORT_TABLE_TENNIS)   -> SPORT_TABLE_TENNIS
            name.contains(SPORT_TUG_OF_WAR) -> SPORT_TUG_OF_WAR
            name.contains(SPORT_LUDO)  -> SPORT_LUDO
            name.contains(SPORT_CHESS) -> SPORT_CHESS
            stats.topGoalScorers.orEmpty().isNotEmpty()
                    || stats.topAssistants.orEmpty().isNotEmpty() -> SPORT_FUTSAL
            else -> SPORT_CRICKET
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tournamentId: Long, sportId: Long = -1L, sportName: String = ""): StatsFragment {
            return StatsFragment().apply {
                arguments = Bundle().apply {
                    putLong("tournamentId", tournamentId)
                    putLong("sportId", sportId)
                    putString("sportName", sportName)
                }
            }
        }

        private const val SPORT_CRICKET    = "cricket"
        private const val SPORT_FUTSAL     = "futsal"
        private const val SPORT_VOLLEYBALL = "volleyball"
        private const val SPORT_BADMINTON  = "badminton"
        private const val SPORT_TABLE_TENNIS  = "table_tennis"
        private const val SPORT_TUG_OF_WAR  = "tug_of_war"
        private const val SPORT_LUDO  = "ludo"
        private const val SPORT_CHESS = "chess"
    }
}