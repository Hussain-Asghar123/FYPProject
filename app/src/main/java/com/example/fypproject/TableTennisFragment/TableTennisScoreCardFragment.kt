package com.example.fypproject.TableTennisFragment


import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.TableTennisScoringActivity
import com.example.fypproject.Scoring.VolleyBallScoringActivity
import com.example.fypproject.ScoringDTO.TableTennisScoringDTO
import com.example.fypproject.Sockets.JsonConverter
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.TabletennisScorecardFragmentBinding
import com.google.gson.Gson
import org.json.JSONObject

class TableTennisScoreCardFragment :Fragment(R.layout.tabletennis_scorecard_fragment) {
    private var _binding: TabletennisScorecardFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private val SOCKET_KEY = "TableTennisScoreCardFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = TabletennisScorecardFragmentBinding.bind(view)
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

        (activity as? TableTennisScoringActivity)?.latestScore?.let { updateUI(it) }

    }

    private fun updateUI(score: TableTennisScoringDTO) {
        if (_binding == null) return
        binding.apply {
            tvTeam1Goals.text = (score.team1Games ?: 0).toString()
            tvTeam2Goals.text = (score.team2Games ?: 0).toString()

            tvTeam1Fouls.text = (score.team1Points ?: 0).toString()
            tvTeam2Fouls.text = (score.team2Points ?: 0).toString()
        }
    }
    private fun registerSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected -> { /* silent */ }
                    is SocketState.Error -> context?.toastShort("Socket Error")
                    is SocketState.Disconnected -> {}
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
    fun onScoreUpdated(score: TableTennisScoringDTO) {
        if (_binding == null) return
        updateUI(score)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            registerSocketListeners()
            (activity as? TableTennisScoringActivity)?.latestScore?.let { onScoreUpdated(it) }
        } else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): TableTennisScoreCardFragment {
            return TableTennisScoreCardFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }

}