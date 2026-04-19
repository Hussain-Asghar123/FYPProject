package com.example.fypproject.VolleyBallFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Sockets.JsonConverter
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.InfoFragmentBinding
import com.example.fypproject.databinding.VolleyballInfoFragmentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class VolleyBallInfoFragment : Fragment(R.layout.info_fragment) {

    private var _binding: InfoFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

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
        setupSocketConnection()
        binding.tvBallTypeLabel.text="Sets"
        populateMatchInfo()
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

    private fun setupSocketConnection() {
        matchResponse?.id?.let { id ->
            WebSocketManager.socketStateListener = { state ->
                activity?.runOnUiThread {
                    when (state) {
                        is SocketState.Connected -> _binding?.root?.context?.toastShort("")
                        is SocketState.Error -> _binding?.root?.context?.toastShort("Socket Error: ${state.message}")
                        is SocketState.Disconnected -> {}
                    }
                }
            }
            WebSocketManager.messageListener = { jsonString ->
                val updatedScore = JsonConverter.fromJson(jsonString)
                updatedScore?.let {
                    activity?.runOnUiThread {
                    }
                }
            }
            WebSocketManager.connect(id)
        }
    }

    private fun populateMatchInfo() {
        matchResponse?.let { match ->
            binding.apply {
                val tossWinnerName = when (match.tossWinnerId) {
                    match.team1Id -> match.team1Name
                    match.team2Id -> match.team2Name
                    else -> "Unknown"
                }

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
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            date?.let { outputFormat.format(it) } ?: dateTime
        } catch (e: Exception) {
            dateTime
        }
    }

    companion object {
        fun newInstance(match: MatchResponse): VolleyBallInfoFragment {
            return VolleyBallInfoFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}