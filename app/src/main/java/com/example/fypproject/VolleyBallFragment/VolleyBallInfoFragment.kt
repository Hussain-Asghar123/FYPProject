package com.example.fypproject.VolleyBallFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.databinding.InfoFragmentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class VolleyBallInfoFragment : Fragment(R.layout.info_fragment) {

    private var _binding: InfoFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private val SOCKET_KEY = "VolleyBallInfoFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = InfoFragmentBinding.bind(view)
        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }
        populateMatchInfo()
    }

    override fun onResume() { super.onResume(); registerSocketListeners() }
    override fun onPause() { super.onPause(); unregisterSocketListeners() }
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) registerSocketListeners() else unregisterSocketListeners()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    private fun registerSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected -> {}
                    is SocketState.Error -> {}
                    is SocketState.Disconnected -> {}
                }
            }
        }
        WebSocketManager.addMessageListener(SOCKET_KEY) {}
    }

    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }

    private fun populateMatchInfo() {
        matchResponse?.let { match ->
            binding.apply {
                val tossWinnerName = when (match.tossWinnerId) {
                    match.team1Id -> match.team1Name
                    match.team2Id -> match.team2Name
                    else -> "Unknown"
                }
                tvBallTypeLabel.text = "Sets"
                tvMatchTitle.text = "${match.team1Name} vs ${match.team2Name}"
                tvTournament.text = match.tournamentName
                tvMatchScorer.text = match.scorerId
                tvOvers.text = match.sets.toString()
                tvStatus.text = match.status
                tvVenue.text = match.venue
                tvDate.text = formatDateTime(match.date)
                tvTime.text = match.time
                tvTossWonBy.text = tossWinnerName
                tvChooseTo.text = match.decision
                tvMatchId.text = match.id.toString()
            }
        }
    }

    private fun formatDateTime(dateTime: String?): String {
        if (dateTime.isNullOrEmpty()) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            date?.let { outputFormat.format(it) } ?: dateTime
        } catch (e: Exception) { dateTime }
    }

    companion object {
        fun newInstance(match: MatchResponse) = VolleyBallInfoFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}