package com.example.fypproject.FutsalFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.FutsalScoringActivity
import com.example.fypproject.ScoringDTO.FutsalScoreDTO
import com.example.fypproject.databinding.FutsalScorecardFragmentBinding

class FutsalScoreCardFragment : Fragment(R.layout.futsal_scorecard_fragment) {

    private var _binding: FutsalScorecardFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FutsalScorecardFragmentBinding.bind(view)

        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }

        // Team names set karo
        binding.tvTeam1Name.text = matchResponse?.team1Name ?: "Team 1"
        binding.tvTeam2Name.text = matchResponse?.team2Name ?: "Team 2"

        // ── JS ki tarah: jo data already aa chuka hai wo dikhao ──
        // Jab fragment pehli baar bane ya tab switch karo — cached data dikhao
        (activity as? FutsalScoringActivity)?.latestScore?.let { updateUI(it) }
    }

    // Activity is function ko call karegi jab naya data aaye
    fun onScoreUpdated(score: FutsalScoreDTO) {
        if (_binding == null) return
        updateUI(score)
    }

    private fun updateUI(score: FutsalScoreDTO) {
        if (_binding == null) return
        binding.tvTeam1Name.text        = matchResponse?.team1Name ?: "Team 1"
        binding.tvTeam2Name.text        = matchResponse?.team2Name ?: "Team 2"
        binding.tvTeam1Goals.text       = score.team1Score.toString()
        binding.tvTeam2Goals.text       = score.team2Score.toString()
        binding.tvTeam1Fouls.text       = score.team1Fouls.toString()
        binding.tvTeam2Fouls.text       = score.team2Fouls.toString()
        binding.tvTeam1YellowCards.text = score.team1YellowCards.toString()
        binding.tvTeam2YellowCards.text = score.team2YellowCards.toString()
        binding.tvTeam1RedCards.text    = score.team1RedCards.toString()
        binding.tvTeam2RedCards.text    = score.team2RedCards.toString()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // Tab switch pe cached data refresh karo
        if (!hidden) {
            (activity as? FutsalScoringActivity)?.latestScore?.let { updateUI(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): FutsalScoreCardFragment {
            return FutsalScoreCardFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}