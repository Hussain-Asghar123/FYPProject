package com.example.fypproject.HockeyFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.HockeyEventsAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Scoring.HockeyScoringActivity
import com.example.fypproject.ScoringDTO.HockeyEventDTO
import com.example.fypproject.ScoringDTO.HockeyScoringDto
import com.example.fypproject.databinding.FragmentHockeyEventsBinding

class HockeyEventsFragment : Fragment(R.layout.fragment_hockey_events) {

    private var _binding: FragmentHockeyEventsBinding? = null
    private val binding get() = _binding!!

    private var matchResponse: MatchResponse? = null
    private val eventsList = mutableListOf<HockeyEventDTO>()
    private lateinit var eventsAdapter: HockeyEventsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHockeyEventsBinding.bind(view)

        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }

        setupRecyclerView()

        (activity as? HockeyScoringActivity)?.latestScore?.let { updateEvents(it) }
    }

    private fun setupRecyclerView() {
        eventsAdapter = HockeyEventsAdapter(eventsList) { event ->
            // In future, media click here if needed
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventsAdapter
    }

    fun onScoreUpdated(score: HockeyScoringDto) {
        if (_binding == null) return
        updateEvents(score)
    }

    private fun updateEvents(score: HockeyScoringDto) {
        if (_binding == null) return

        eventsList.clear()
        eventsList.addAll(score.hockeyEvents)

        if (eventsList.isEmpty()) {
            binding.tvNoEvents.visibility = View.VISIBLE
            binding.rvEvents.visibility = View.GONE
        } else {
            binding.tvNoEvents.visibility = View.GONE
            binding.rvEvents.visibility = View.VISIBLE
            eventsAdapter.notifyDataSetChanged()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            (activity as? HockeyScoringActivity)?.latestScore?.let { updateEvents(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): HockeyEventsFragment {
            return HockeyEventsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}