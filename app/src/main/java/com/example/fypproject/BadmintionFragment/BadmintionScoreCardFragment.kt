package com.example.fypproject.BadmintionFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.BadmintionScoringActivity
import com.example.fypproject.Scoring.TableTennisScoringActivity
import com.example.fypproject.ScoringDTO.BadmintionScoreDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.BadmintionScorecardFragmentBinding

class BadmintionScoreCardFragment : Fragment(R.layout.badmintion_scorecard_fragment) {

    private var _binding: BadmintionScorecardFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private val SOCKET_KEY = "BadmintionScoreCardFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = BadmintionScorecardFragmentBinding.bind(view)

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }

        binding.tvTeam1Name.text = matchResponse?.team1Name ?: "Team 1"
        binding.tvTeam2Name.text = matchResponse?.team2Name ?: "Team 2"

        (activity as? BadmintionScoringActivity)?.latestScore?.let { updateUI(it) }
    }

    private fun updateUI(score: BadmintionScoreDTO) {
        if (_binding == null) return

        binding.tvEmptyState.visibility = View.GONE

        binding.apply {
            tvTeam1Goals.text = (score.team1Games ?: 0).toString()
            tvTeam2Goals.text = (score.team2Games ?: 0).toString()

            tvTeam1Fouls.text = (score.team1Points ?: 0).toString()
            tvTeam2Fouls.text = (score.team2Points ?: 0).toString()
        }
    }

    fun onScoreUpdated(score: BadmintionScoreDTO) {
        if (_binding == null) return
        setLoading(false)
        updateUI(score)
    }

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showEmptyState() {
        if (_binding == null) return
        binding.tvEmptyState.visibility = View.VISIBLE
    }

    private fun registerSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Error -> context?.toastShort("Socket Error")
                    else -> {}
                }
            }
        }
        WebSocketManager.addMessageListener(SOCKET_KEY) { /* no-op */ }
    }

    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }


    override fun onResume() {
        super.onResume()
        registerSocketListeners()
    }

    override fun onPause() {
        super.onPause()
        unregisterSocketListeners()

    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            registerSocketListeners()
            (activity as? BadmintionScoringActivity)?.latestScore?.let { onScoreUpdated(it) }
        } else {
            unregisterSocketListeners()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = BadmintionScoreCardFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}