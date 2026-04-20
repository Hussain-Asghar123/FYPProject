package com.example.fypproject.LudoFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.LudoEventAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
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

    override fun onResume() {
        super.onResume()
        setupSocketListeners()
        matchResponse?.id?.let { WebSocketManager.connect(it) }
    }

    override fun onPause() {
        super.onPause()
        WebSocketManager.socketStateListener = null
        WebSocketManager.messageListener = null
        WebSocketManager.disconnect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        WebSocketManager.socketStateListener = null
        WebSocketManager.messageListener = null
        WebSocketManager.disconnect()
        _binding = null
    }

    private fun setupSocketListeners() {
        WebSocketManager.socketStateListener = { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected -> requireContext().toastShort("Connected")
                    is SocketState.Error -> requireContext().toastShort("Socket Error: ${state.message}")
                    is SocketState.Disconnected -> {}
                }
            }
        }
        WebSocketManager.messageListener = { jsonString ->
            activity?.runOnUiThread {
                try {
                    val score = Gson().fromJson(jsonString, LudoScoreDTO::class.java)
                    val events = score?.ludoEvents
                    if (!events.isNullOrEmpty()) {
                        updateEvents(events)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        fun newInstance(match: MatchResponse) = LudoHighlightsFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}