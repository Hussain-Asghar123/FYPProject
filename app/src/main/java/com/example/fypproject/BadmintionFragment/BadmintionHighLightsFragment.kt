package com.example.fypproject.BadmintionFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.BadmintionEventAdapter
import com.example.fypproject.Adapter.VolleyBallEventsAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.BadmintionScoringActivity
import com.example.fypproject.ScoringDTO.BadmintionScoreDTO
import com.example.fypproject.ScoringDTO.BadmintonEvent
import com.example.fypproject.ScoringDTO.VollayBallScoreDTO
import com.example.fypproject.ScoringDTO.VolleyballEvent
import com.example.fypproject.Sockets.JsonConverter
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.BadmintionHighlightsFragmentBinding
import com.example.fypproject.databinding.BadmintionScoringFragmentBinding
import com.google.gson.Gson

class BadmintionHighLightsFragment: Fragment(R.layout.badmintion_highlights_fragment) {
    private var _binding: BadmintionHighlightsFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    private val SOCKET_KEY = "BadmintionHighLightsFragment"

    private val eventsList = mutableListOf<BadmintonEvent>()
    private lateinit var eventsAdapter: BadmintionEventAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = BadmintionHighlightsFragmentBinding.bind(view)
        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }
        setupRecyclerView()
        (activity as? BadmintionScoringActivity)?.latestScore?.let {
            it.badmintonEvents?.let { events -> updateEvents(events) }
        }

    }
    private fun setupRecyclerView() {
        eventsAdapter = BadmintionEventAdapter(eventsList) { _ -> }
        binding.rvMatchEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatchEvents.adapter = eventsAdapter
    }

    private fun updateEvents(newEvents: List<BadmintonEvent>) {
        val sorted = newEvents.sortedByDescending { it.eventTimeSeconds }
        eventsList.clear()
        eventsList.addAll(sorted)
        eventsAdapter.notifyDataSetChanged()

        binding.rvMatchEvents.visibility =
            if (eventsList.isEmpty()) View.GONE else View.VISIBLE
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

    fun onScoreUpdated(score: BadmintionScoreDTO) {
        if (_binding == null) return
        score.badmintonEvents?.let { updateEvents(it) }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            (activity as? BadmintionScoringActivity)?.latestScore?.let { onScoreUpdated(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): BadmintionHighLightsFragment {
            return BadmintionHighLightsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}