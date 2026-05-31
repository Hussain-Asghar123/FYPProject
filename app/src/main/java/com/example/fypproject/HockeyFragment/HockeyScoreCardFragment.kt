package com.example.fypproject.HockeyFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.HockeyScoringActivity
import com.example.fypproject.ScoringDTO.HockeyScoringDto
import com.example.fypproject.databinding.FragmentHockeyScorecardBinding

class HockeyScoreCardFragment : Fragment(R.layout.fragment_hockey_scorecard) {

    private var _binding: FragmentHockeyScorecardBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHockeyScorecardBinding.bind(view)

        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }

        binding.tvTeam1Name.text = matchResponse?.team1Name ?: "Team 1"
        binding.tvTeam2Name.text = matchResponse?.team2Name ?: "Team 2"

        (activity as? HockeyScoringActivity)?.latestScore?.let { updateUI(it) }
    }

    fun onScoreUpdated(score: HockeyScoringDto) {
        if (_binding == null) return
        updateUI(score)
    }

    private fun updateUI(score: HockeyScoringDto) {
        if (_binding == null) return
        binding.tvTeam1Name.text        = matchResponse?.team1Name ?: "Team 1"
        binding.tvTeam2Name.text        = matchResponse?.team2Name ?: "Team 2"
        binding.tvTeam1Goals.text       = score.team1Score.toString()
        binding.tvTeam2Goals.text       = score.team2Score.toString()
        binding.tvTeam1Fouls.text       = score.team1Fouls.toString()
        binding.tvTeam2Fouls.text       = score.team2Fouls.toString()
        binding.tvTeam1GreenCards.text  = score.team1GreenCards.toString()
        binding.tvTeam2GreenCards.text  = score.team2GreenCards.toString()
        binding.tvTeam1YellowCards.text = score.team1YellowCards.toString()
        binding.tvTeam2YellowCards.text = score.team2YellowCards.toString()
        binding.tvTeam1RedCards.text    = score.team1RedCards.toString()
        binding.tvTeam2RedCards.text    = score.team2RedCards.toString()
        binding.tvTeam1PenaltyCorners.text = score.team1PenaltyCorners.toString()
        binding.tvTeam2PenaltyCorners.text = score.team2PenaltyCorners.toString()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            (activity as? HockeyScoringActivity)?.latestScore?.let { updateUI(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): HockeyScoreCardFragment {
            return HockeyScoreCardFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}