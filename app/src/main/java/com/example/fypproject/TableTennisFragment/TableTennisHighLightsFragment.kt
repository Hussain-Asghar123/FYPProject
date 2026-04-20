package com.example.fypproject.TableTennisFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TableTennisEventAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
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
                    val score = Gson().fromJson(jsonString, TableTennisScoringDTO::class.java)
                    val events = score?.tableTennisEvents
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
        fun newInstance(match: MatchResponse): TableTennisHighLightsFragment {
            return TableTennisHighLightsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}