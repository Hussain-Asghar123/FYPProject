package com.example.fypproject.TugOfWarFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TugOfWarEventAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.TugOfWarScoringActivity
import com.example.fypproject.ScoringDTO.TugOfWarEvent
import com.example.fypproject.ScoringDTO.TugOfWarScoreDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.databinding.TugofwarHighlightFragmentBinding

class TugOfWarHighlightsFragment : Fragment(R.layout.tugofwar_highlight_fragment) {

	private var _binding: TugofwarHighlightFragmentBinding? = null
	private val binding get() = _binding!!
	private var matchResponse: MatchResponse? = null
	private val eventsList = mutableListOf<TugOfWarEvent>()
	private lateinit var eventsAdapter: TugOfWarEventAdapter

	private val SOCKET_KEY = "TugOfWarHighlightsFragment"

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		_binding = TugofwarHighlightFragmentBinding.bind(view)
		arguments?.let { bundle ->
			matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				bundle.getSerializable("match_response", MatchResponse::class.java)
			} else {
				@Suppress("DEPRECATION")
				bundle.getSerializable("match_response") as? MatchResponse
			}
		}
		setupRecyclerView()
		(activity as? TugOfWarScoringActivity)?.latestScore?.let {
			it.tugOfWarEvents?.let { events -> updateEvents(events) }
		}
	}

	override fun onResume() {
		super.onResume()
		registerSocketListeners()
	}

	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) {
			registerSocketListeners()
			(activity as? TugOfWarScoringActivity)?.latestScore?.let { onScoreUpdated(it) }
		} else unregisterSocketListeners()
	}

	override fun onPause() {
		super.onPause()
		unregisterSocketListeners()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		unregisterSocketListeners()
		_binding = null
	}

	fun onScoreUpdated(score: TugOfWarScoreDTO) {
		if (_binding == null) return
		score.tugOfWarEvents?.let { updateEvents(it) }
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

	private fun setupRecyclerView() {
		eventsAdapter = TugOfWarEventAdapter(eventsList)
		binding.rvMatchEvents.layoutManager = LinearLayoutManager(requireContext())
		binding.rvMatchEvents.adapter = eventsAdapter
	}

	private fun updateEvents(newEvents: List<TugOfWarEvent>) {
		val sorted = newEvents.sortedByDescending { it.id ?: 0L }
		eventsList.clear()
		eventsList.addAll(sorted)
		eventsAdapter.notifyDataSetChanged()
		binding.rvMatchEvents.visibility =
			if (eventsList.isEmpty()) View.GONE else View.VISIBLE
	}

	companion object {
		fun newInstance(match: MatchResponse) = TugOfWarHighlightsFragment().apply {
			arguments = Bundle().apply { putSerializable("match_response", match) }
		}
	}
}