package com.example.fypproject.LudoFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.LudoEventAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.LudoScoringActivity
import com.example.fypproject.ScoringDTO.LudoEvent
import com.example.fypproject.ScoringDTO.LudoScoreDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.LudoHighlightFragmentBinding
import com.google.gson.Gson

class LudoHighlightsFragment : Fragment(R.layout.ludo_highlight_fragment) {

    private var _binding: LudoHighlightFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private val SOCKET_KEY = "LudoHighlightsFragment"
    private val eventsList = mutableListOf<LudoEvent>()
    private lateinit var eventsAdapter: LudoEventAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = LudoHighlightFragmentBinding.bind(view)
        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }
        setupRecyclerView()
        (activity as? LudoScoringActivity)?.latestScore?.let {
            it.ludoEvents?.let { events -> updateEvents(events) }
        }
    }
    fun onScoreUpdated(score: LudoScoreDTO) {
        if (_binding == null) return
        score.ludoEvents?.let { updateEvents(it) }
    }

    private fun setupRecyclerView() {
        eventsAdapter = LudoEventAdapter(eventsList) { _ -> }
        binding.rvMatchEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatchEvents.adapter = eventsAdapter
    }

    private fun updateEvents(newEvents: List<LudoEvent>) {
        val sorted = newEvents.sortedByDescending { it.id ?: 0L }
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
            (activity as? LudoScoringActivity)?.latestScore?.let {
                it.ludoEvents?.let { events -> updateEvents(events) }
            }
        } else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = LudoHighlightsFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}