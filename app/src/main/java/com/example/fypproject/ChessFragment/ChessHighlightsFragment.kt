package com.example.fypproject.ChessFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.ChessEventAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.ChessScoringActivity
import com.example.fypproject.ScoringDTO.ChessEvent
import com.example.fypproject.ScoringDTO.ChessScoreDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.databinding.ChessHighlightFragmentBinding

class ChessHighlightsFragment : Fragment(R.layout.chess_highlight_fragment) {

    private var _binding: ChessHighlightFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private val SOCKET_KEY = "ChessHighlightsFragment"
    private val eventsList = mutableListOf<ChessEvent>()
    private lateinit var eventsAdapter: ChessEventAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ChessHighlightFragmentBinding.bind(view)
        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }
        setupRecyclerView()
        showLoadingState()
        // ✅ Cached data load karo
        (activity as? ChessScoringActivity)?.latestScore?.let {
            it.chessEvents?.let { events -> updateEvents(events) }
        } ?: run {
            showEmptyState()
        }
    }

    fun onScoreUpdated(score: ChessScoreDTO) {
        if (_binding == null) return
        score.chessEvents?.let {
            if (it.isNotEmpty()) {
                updateEvents(it)
            } else {
                showEmptyState()
            }
        } ?: run {
            showEmptyState()
        }
    }

    private fun setupRecyclerView() {
        eventsAdapter = ChessEventAdapter(eventsList) { _ -> }
        binding.rvMatchEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatchEvents.adapter = eventsAdapter
    }

    private fun updateEvents(newEvents: List<ChessEvent>) {
        val sorted = newEvents.sortedByDescending { it.id ?: 0L }
        eventsList.clear()
        eventsList.addAll(sorted)
        eventsAdapter.notifyDataSetChanged()

        if (eventsList.isEmpty()) {
            showEmptyState()
        } else {
            hideLoadingState()
            hideEmptyState()
            showContentView()
        }
    }

    private fun showLoadingState() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.rvMatchEvents.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
    }

    private fun hideLoadingState() {
        binding.progressLoading.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressLoading.visibility = View.GONE
        binding.rvMatchEvents.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        binding.emptyStateContainer.visibility = View.GONE
    }

    private fun showContentView() {
        binding.rvMatchEvents.visibility = View.VISIBLE
    }

    private fun registerSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected    -> { /* silent */ }
                    is SocketState.Error        -> { /* handle if needed */ }
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
            (activity as? ChessScoringActivity)?.latestScore?.let { onScoreUpdated(it) }
        } else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = ChessHighlightsFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}