package com.example.fypproject.FutsalFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.FutsalScoreDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.FutsalScorecardFragmentBinding
import com.google.gson.Gson

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

        setupSocketConnection()
    }

    private fun updateUI(score: FutsalScoreDTO) {
        binding.tvTeam1Name.text  = matchResponse?.team1Name ?: "Team 1"
        binding.tvTeam2Name.text  = matchResponse?.team2Name ?: "Team 2"
        binding.tvTeam1Goals.text       = score.team1Score.toString()
        binding.tvTeam2Goals.text       = score.team2Score.toString()
        binding.tvTeam1Fouls.text       = score.team1Fouls.toString()
        binding.tvTeam2Fouls.text       = score.team2Fouls.toString()
        binding.tvTeam1YellowCards.text = score.team1YellowCards.toString()
        binding.tvTeam2YellowCards.text = score.team2YellowCards.toString()
        binding.tvTeam1RedCards.text    = score.team1RedCards.toString()
        binding.tvTeam2RedCards.text    = score.team2RedCards.toString()
    }

    private fun setupSocketConnection() {
        matchResponse?.id?.let {
            WebSocketManager.socketStateListener = { state ->
                activity?.runOnUiThread {
                    when (state) {
                        is SocketState.Connected    -> {}
                        is SocketState.Error        -> requireContext().toastShort("Socket Error: ${state.message}")
                        is SocketState.Disconnected -> {}
                    }
                }
            }

            WebSocketManager.messageListener = { jsonString ->
                val updatedScore = runCatching {
                    Gson().fromJson(jsonString, FutsalScoreDTO::class.java)
                }.getOrNull()
                updatedScore?.let { score ->
                    activity?.runOnUiThread {
                        updateUI(score)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        matchResponse?.id?.let { WebSocketManager.connect(it) }
    }

    override fun onPause() {
        super.onPause()
        WebSocketManager.disconnect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        WebSocketManager.socketStateListener = null
        WebSocketManager.messageListener = null
        WebSocketManager.disconnect()
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