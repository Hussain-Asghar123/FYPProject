package com.example.fypproject.VolleyBallFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.VolleyBallEventsAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.VolleyBallScoringActivity
import com.example.fypproject.ScoringDTO.VollayBallScoreDTO
import com.example.fypproject.ScoringDTO.VolleyballEvent
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.VolleyballHighlightsFragmentBinding
import com.google.gson.Gson

class VolleyBallHighLightsFragment : Fragment(R.layout.volleyball_highlights_fragment) {

    private var _binding: VolleyballHighlightsFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private val SOCKET_KEY = "VolleyBallHighLightsFragment"

    private val eventsList = mutableListOf<VolleyballEvent>()
    private lateinit var eventsAdapter: VolleyBallEventsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = VolleyballHighlightsFragmentBinding.bind(view)

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }

        setupRecyclerView()
        registerSocketListeners()
        (activity as? VolleyBallScoringActivity)?.latestScore?.let {
            it.volleyballEvents?.let { events -> updateEvents(events) }
        }

    }

    private fun setupRecyclerView() {
        eventsAdapter = VolleyBallEventsAdapter(eventsList) { _ -> }
        binding.rvMatchEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatchEvents.adapter = eventsAdapter
    }
    fun onScoreUpdated(score: VollayBallScoreDTO) {
        if (_binding == null) return
        score.volleyballEvents?.let { updateEvents(it) }
    }

    private fun updateEvents(newEvents: List<VolleyballEvent>) {
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
            (activity as? VolleyBallScoringActivity)?.latestScore?.let { onScoreUpdated(it) }
        } else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }


    companion object {
        fun newInstance(match: MatchResponse) = VolleyBallHighLightsFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}