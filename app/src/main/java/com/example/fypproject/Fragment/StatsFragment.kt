package com.example.fypproject.Fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)

        tournamentId = arguments?.getLong("tournamentId") ?: -1L

        if (tournamentId != -1L) {
            fetchTournamentStats(tournamentId)
        }
    }

    private fun fetchTournamentStats(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val stats = api.getTournamentStats(id)
                populateUI(stats)
            } catch (e: Exception) {
                Log.e("StatsFragment", "API Error: ${e::class.simpleName} - ${e.message}", e)
                Toast.makeText(context, "Error: ${e::class.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun populateUI(stats: TournamentStatsDto) {
        val detectedSport = detectSport(stats)
        val isFutsal = detectedSport == SPORT_FUTSAL

        if (isFutsal) {
            populateFutsalUI(stats)
        } else {
            populateCricketUI(stats)
        }
    }

    private fun populateCricketUI(stats: TournamentStatsDto) {
        val runScorers = stats.topRunScorers.orEmpty()
        val bowlers = stats.topBowlers.orEmpty()

        binding.headerBatsmen.apply {
            tvRuns.text = "Runs"; tvBalls.text = "Balls"
            tvFours.text = "4s"; tvSixes.text = "6s"; tvPom.text = "POM"
        }
        binding.headerBowlers.apply {
            tvWickets.text = "Wkts"; tvRuns.text = "Runs"
            tvBalls.text = "Balls"; tvEconomy.text = "Eco"; tvPom.text = "POM"
        }

        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"

        binding.cardBestBatsman.tvLabel.text = "Best Batsman"
        binding.cardBestBatsman.tvPlayerName.text = stats.bestBatsman?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text =
            "${runScorers.firstOrNull { it.playerId == stats.bestBatsman?.playerId }?.runs ?: 0} runs"

        binding.cardBestBowler.tvLabel.text = "Best Bowler"
        binding.cardBestBowler.tvPlayerName.text = stats.bestBowler?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text = stats.bestBowler?.reason ?: "No Data"

        binding.cardHighestScore.tvLabel.text = "Best Fielder"
        binding.cardHighestScore.tvPlayerName.text = stats.bestFielder?.playerName ?: "TBD"
        binding.cardHighestScore.tvValue.text = stats.bestFielder?.reason ?: "No Data"

        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
                battingItems = runScorers,
                isBatting = true,
                isFutsal = false
            )
        }

        binding.rvTopBowlers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
                bowlingItems = bowlers,
                isBatting = false,
                isFutsal = false
            )
        }
    }

    private fun populateFutsalUI(stats: TournamentStatsDto) {
        binding.rvTopBowlers.visibility = View.GONE
        binding.headerBowlers.root.visibility = View.GONE
        binding.tvTopBowlersTitle.visibility = View.GONE

        val goalScorers = stats.topGoalScorers.orEmpty()
        val assistants = stats.topAssistants.orEmpty()

        binding.tvTopBatsmenTitle.text = "Top Players"


        binding.tvManOfTournament.text = stats.manOfTournament?.playerName ?: "TBD"

        binding.cardBestBatsman.tvLabel.text = "Top Goal Scorer"
        binding.cardBestBatsman.tvPlayerName.text = goalScorers.maxByOrNull { it.goals }?.playerName ?: "TBD"
        binding.cardBestBatsman.tvValue.text = goalScorers.maxByOrNull { it.goals }?.let { "${it.goals} goals" } ?: "No Data"

        binding.cardBestBowler.tvLabel.text = "Top Assister"
        binding.cardBestBowler.tvPlayerName.text = assistants.maxByOrNull { it.assists }?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text = assistants.maxByOrNull { it.assists }?.let { "${it.assists} assists" } ?: "No Data"

        binding.cardHighestScore.tvLabel.text = "Man of Tournament"
        binding.cardHighestScore.tvPlayerName.text = stats.manOfTournament?.playerName ?: "TBD"
        binding.cardHighestScore.tvValue.text = stats.manOfTournament?.reason ?: "No Data"

        binding.headerBatsmen.apply {
            tvRuns.text = "Goals"; tvBalls.text = "Asst"
            tvFours.text = "G+A"; tvSixes.text = "YC"; tvPom.text = "RC"
        }

        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
                goalScorerItems = goalScorers,
                isBatting = true,
                isFutsal = true
            )
        }

    }

    private fun detectSport(stats: TournamentStatsDto): String {
        val responseSport = stats.sport?.lowercase(Locale.US)
        val goalScorers = stats.topGoalScorers.orEmpty()
        val assistants = stats.topAssistants.orEmpty()
        val runScorers = stats.topRunScorers.orEmpty()
        val bowlers = stats.topBowlers.orEmpty()

        val detected = when {
            responseSport?.contains(SPORT_FUTSAL) == true -> SPORT_FUTSAL
            responseSport?.contains(SPORT_CRICKET) == true -> SPORT_CRICKET
            goalScorers.isNotEmpty() || assistants.isNotEmpty() -> SPORT_FUTSAL
            runScorers.isNotEmpty() || bowlers.isNotEmpty() -> SPORT_CRICKET
            else -> SPORT_CRICKET
        }
       return detected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tournamentId: Long): StatsFragment {
            return StatsFragment().apply {
                arguments = Bundle().apply {
                    putLong("tournamentId", tournamentId)
                }
            }
        }

        private const val SPORT_CRICKET = "cricket"
        private const val SPORT_FUTSAL = "futsal"
    }
}