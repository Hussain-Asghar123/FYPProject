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
        setupSocketConnection()
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
    override fun onResume() {
        super.onResume()
        matchResponse?.id?.let { id ->
            matchResponse?.id?.toLong()?.let { WebSocketManager.connect(it) }
        }
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
                        is SocketState.Connected -> requireContext().toastShort("")
                        is SocketState.Error -> requireContext().toastShort("Socket Error: ${state.message}")
                        is SocketState.Disconnected -> {}
                    }
                }
            }
            WebSocketManager.messageListener = { jsonString ->
                activity?.runOnUiThread {
                    try {
                        val score = Gson().fromJson(jsonString, BadmintionScoreDTO::class.java)
                        val events = score?.badmintonEvents
                        if (!events.isNullOrEmpty()) {
                            updateEvents(events)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            WebSocketManager.connect(id)
        }
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