package com.example.fypproject.BadmintionFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.BadmintionScoreDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.BadmintionScorecardFragmentBinding
import com.google.gson.Gson
import org.json.JSONObject

class BadmintionScoreCardFragment : Fragment(R.layout.badmintion_scorecard_fragment) {

    private var _binding: BadmintionScorecardFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

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

        setupSocketConnection()
    }

    private fun updateUI(obj: JSONObject) {
        if (_binding == null) return
        binding.apply {
            val team1Games = obj.optInt("team1Games",
                obj.optInt("gamesTeam1", 0))
            val team2Games = obj.optInt("team2Games",
                obj.optInt("gamesTeam2", 0))

            val team1Points = obj.optInt("team1Points",
                obj.optInt("team1Score",
                    obj.optInt("scoreTeam1", 0)))
            val team2Points = obj.optInt("team2Points",
                obj.optInt("team2Score",
                    obj.optInt("scoreTeam2", 0)))

            tvTeam1Goals.text = team1Games.toString()
            tvTeam2Goals.text = team2Games.toString()

            tvTeam1Fouls.text = team1Points.toString()
            tvTeam2Fouls.text = team2Points.toString()

            android.util.Log.d("BADMINTON_SCORECARD", "Games: $team1Games-$team2Games, Points: $team1Points-$team2Points")
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
                        val obj = JSONObject(jsonString)
                        updateUI(obj)
                        android.util.Log.d("BADMINTON_SCORECARD", "Received: $jsonString")
                    } catch (e: Exception) {
                        android.util.Log.e("BADMINTON_SCORECARD", "Parse error: ${e.message}")
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
        WebSocketManager.messageListener     = null
        WebSocketManager.disconnect()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = BadmintionScoreCardFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}