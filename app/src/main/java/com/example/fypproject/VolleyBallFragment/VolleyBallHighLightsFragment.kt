package com.example.fypproject.VolleyBallFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.VolleyBallEventsAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
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
        setupSocketConnection()
    }

    private fun setupRecyclerView() {
        eventsAdapter = VolleyBallEventsAdapter(eventsList) { _ -> }
        binding.rvMatchEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatchEvents.adapter = eventsAdapter
    }

    private fun updateEvents(newEvents: List<VolleyballEvent>) {
        val sorted = newEvents.sortedByDescending { it.eventTimeSeconds }
        eventsList.clear()
        eventsList.addAll(sorted)
        eventsAdapter.notifyDataSetChanged()

        binding.rvMatchEvents.visibility =
            if (eventsList.isEmpty()) View.GONE else View.VISIBLE
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
                        val score = Gson().fromJson(jsonString, VollayBallScoreDTO::class.java)
                        val events = score?.volleyballEvents
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

    companion object {
        fun newInstance(match: MatchResponse) = VolleyBallHighLightsFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}