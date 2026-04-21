package com.example.fypproject.FutsalFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.FutsalEventsAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.FutsalScoringActivity
import com.example.fypproject.ScoringDTO.FutsalEventDTO
import com.example.fypproject.ScoringDTO.FutsalScoreDTO
import com.example.fypproject.databinding.FutsalHighlightsFragmentBinding

class FutsalHighLightsFragment : Fragment(R.layout.futsal_highlights_fragment) {

    private var _binding: FutsalHighlightsFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    private val eventsList = mutableListOf<FutsalEventDTO>()
    private lateinit var eventsAdapter: FutsalEventsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FutsalHighlightsFragmentBinding.bind(view)

        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }

        setupRecyclerView()

        // ── JS ki tarah: jo data already aa chuka hai wo dikhao ──
        (activity as? FutsalScoringActivity)?.latestScore?.let {
            updateEvents(it.futsalEvents)
        }
    }

    // Activity is function ko call karegi jab naya data aaye
    fun onScoreUpdated(score: FutsalScoreDTO) {
        if (_binding == null) return
        updateEvents(score.futsalEvents)
    }

    private fun setupRecyclerView() {
        eventsAdapter = FutsalEventsAdapter(eventsList) { _ -> }
        binding.rvMatchEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter
        }
    }

    private fun updateEvents(newEvents: List<FutsalEventDTO>) {
        if (_binding == null) return
        val sorted = newEvents.sortedByDescending { it.eventTimeSeconds }
        eventsList.clear()
        eventsList.addAll(sorted)
        eventsAdapter.notifyDataSetChanged()
        binding.rvMatchEvents.visibility =
            if (eventsList.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // Tab switch pe cached data refresh karo
        if (!hidden) {
            (activity as? FutsalScoringActivity)?.latestScore?.let {
                updateEvents(it.futsalEvents)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): FutsalHighLightsFragment {
            return FutsalHighLightsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}