package com.example.fypproject.VolleyBallFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.VollayBallScoreDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.VolleyballScorecardFragmentBinding
import com.google.gson.Gson

class VolleyBallScoreCardFragment : Fragment(R.layout.volleyball_scorecard_fragment) {

    private var _binding: VolleyballScorecardFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = VolleyballScorecardFragmentBinding.bind(view)

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }

        binding.tvTeam1Name.text = matchResponse?.team1Name ?: "Team 1"
        binding.tvTeam2Name.text = matchResponse?.team2Name ?: "Team 2"

        setupSocketConnection()
    }

    private fun updateUI(score: VollayBallScoreDTO) {
        if (_binding == null) return
        binding.apply {
            // Sets Won row
            tvTeam1Goals.text = (score.team1Sets ?: 0).toString()
            tvTeam2Goals.text = (score.team2Sets ?: 0).toString()

            // Current Points row
            tvTeam1Fouls.text = (score.team1Score ?: 0).toString()
            tvTeam2Fouls.text = (score.team2Score ?: 0).toString()

            // Timeouts Used row
            tvTeam1YellowCards.text = (score.team1Timeouts ?: 0).toString()
            tvTeam2YellowCards.text = (score.team2Timeouts ?: 0).toString()
        }
    }

    private fun setupSocketConnection() {
        matchResponse?.id?.let { id ->
            WebSocketManager.socketStateListener = { state ->
                activity?.runOnUiThread {
                    when (state) {
                        is SocketState.Error -> context?.toastShort("Socket Error")
                        else -> {}
                    }
                }
            }

            WebSocketManager.messageListener = { jsonString ->
                activity?.runOnUiThread {
                    try {
                        val score = Gson().fromJson(jsonString, VollayBallScoreDTO::class.java)
                        score?.let { updateUI(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            WebSocketManager.connect(id)
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
        fun newInstance(match: MatchResponse) = VolleyBallScoreCardFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}