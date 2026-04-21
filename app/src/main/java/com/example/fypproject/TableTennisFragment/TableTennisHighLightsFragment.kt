package com.example.fypproject.TableTennisFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TableTennisEventAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.TableTennisScoringActivity
import com.example.fypproject.ScoringDTO.TableTennisEvent
import com.example.fypproject.ScoringDTO.TableTennisScoringDTO
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.TabletennisHighlightsFragmentBinding
import com.google.gson.Gson

class TableTennisHighLightsFragment : Fragment(R.layout.tabletennis_highlights_fragment) {

    private var _binding: TabletennisHighlightsFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private val eventsList = mutableListOf<TableTennisEvent>()
    private val SOCKET_KEY = "TableTennisHighLightsFragment"
    private lateinit var eventsAdapter: TableTennisEventAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = TabletennisHighlightsFragmentBinding.bind(view)
        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }
        setupRecyclerView()
        (activity as? TableTennisScoringActivity)?.latestScore?.let {
            it.tableTennisEvents?.let { events -> updateEvents(events) }
        }
    }
    fun onScoreUpdated(score: TableTennisScoringDTO) {
        if (_binding == null) return
        score.tableTennisEvents?.let { updateEvents(it) }
    }

    private fun setupRecyclerView() {
        eventsAdapter = TableTennisEventAdapter(eventsList) { _ -> }
        binding.rvMatchEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatchEvents.adapter = eventsAdapter
    }

    private fun updateEvents(newEvents: List<TableTennisEvent>) {
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
            (activity as? TableTennisScoringActivity)?.latestScore?.let { onScoreUpdated(it) }
        } else unregisterSocketListeners()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): TableTennisHighLightsFragment {
            return TableTennisHighLightsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}