package com.example.fypproject.Fragment

import android.os.Bundle
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
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateUI(stats: TournamentStatsDto) {

        // 🏆 Man of Tournament
        binding.tvManOfTournament.text =
            stats.manOfTournament?.playerName ?: "TBD"

        binding.cardBestBatsman.tvLabel.text = "Best Batsman"
        binding.cardBestBatsman.tvPlayerName.text =
            stats.bestBatsman?.playerName ?: "TBD"

        binding.cardBestBatsman.tvValue.text =
            "${stats.topRunScorers.firstOrNull { it.playerId == stats.bestBatsman?.playerId }?.runs ?: 0} runs"


        binding.cardBestBowler.tvLabel.text = "Best Bowler"
        binding.cardBestBowler.tvPlayerName.text =
            stats.bestBowler?.playerName ?: "TBD"
        binding.cardBestBowler.tvValue.text =
            stats.bestBowler?.reason ?: "No Data"

        binding.cardHighestScore.tvLabel.text = "Best Fielder"
        binding.cardHighestScore.tvPlayerName.text =
            stats.bestFielder?.playerName ?: "TBD"
        binding.cardHighestScore.tvValue.text =
            stats.bestFielder?.reason ?: "No Data"

        // 🏏 Top Batsmen
        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
                battingItems = stats.topRunScorers,
                isBatting = true
            )
        }

        // 🎯 Top Bowlers (FIXED)
        binding.rvTopBowlers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(
                bowlingItems = stats.topBowlers,
                isBatting = false
            )
        }
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
    }
}