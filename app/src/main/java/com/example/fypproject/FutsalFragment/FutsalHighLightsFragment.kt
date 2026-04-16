package com.example.fypproject.FutsalFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.FutsalEventsAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.FutsalEventDTO
import com.example.fypproject.ScoringDTO.FutsalScoreDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.FutsalHighlightsFragmentBinding
import com.google.gson.Gson

class FutsalHighLightsFragment : Fragment(R.layout.futsal_highlights_fragment) {

    private var _binding: FutsalHighlightsFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    private val eventsList     = mutableListOf<FutsalEventDTO>()
    private lateinit var eventsAdapter: FutsalEventsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FutsalHighlightsFragmentBinding.bind(view)

        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }
        setupRecyclerView()
        setupSocketConnection()
    }
    private fun setupRecyclerView() {
        eventsAdapter = FutsalEventsAdapter(eventsList) { _ ->

        }
        binding.rvMatchEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter
        }

    }
    private fun updateEvents(newEvents: List<FutsalEventDTO>) {
        val sorted = newEvents.sortedByDescending { it.eventTimeSeconds }

        eventsList.clear()
        eventsList.addAll(sorted)
        eventsAdapter.notifyDataSetChanged()

        binding.rvMatchEvents.visibility =
            if (eventsList.isEmpty()) View.GONE else View.VISIBLE
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
        matchResponse?.id?.let {
            WebSocketManager.socketStateListener = { state ->
                activity?.runOnUiThread {
                    when (state) {
                        is SocketState.Connected -> {}
                        is SocketState.Error -> requireContext().toastShort("Socket Error: ${state.message}")
                        is SocketState.Disconnected -> {}
                    }
                }
            }

            WebSocketManager.messageListener = { jsonString ->
                val updatedScore = runCatching {
                    Gson().fromJson(jsonString, FutsalScoreDTO::class.java)
                }.getOrNull()
                updatedScore?.let {score ->
                    activity?.runOnUiThread {
                        updateEvents(score.futsalEvents)
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(match: MatchResponse): FutsalHighLightsFragment {
            return FutsalHighLightsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}