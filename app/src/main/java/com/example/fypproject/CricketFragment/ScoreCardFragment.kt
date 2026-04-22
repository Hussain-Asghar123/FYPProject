package com.example.fypproject.CricketFragment

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.BatsmanAdapter
import com.example.fypproject.Adapter.BowlerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.ScorecardResponse
import com.example.fypproject.Sockets.JsonConverter
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ScoreboardFragmentBinding
import kotlinx.coroutines.launch

class ScoreCardFragment : Fragment(R.layout.scoreboard_fragment) {
    private var _binding: ScoreboardFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private var showingTeamA = true
    private val SOCKET_KEY = "ScoreCardFragment"
    private lateinit var batsmanAdapter: BatsmanAdapter
    private lateinit var bowlerAdapter: BowlerAdapter
    private var ScoreCardTeamA: ScorecardResponse?=null
    private var ScoreCardTeamB:ScorecardResponse?=null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ScoreboardFragmentBinding.bind(view)

        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }

        binding.btnTeamA.text = matchResponse?.team1Name ?: "Team A"
        binding.btnTeamB.text = matchResponse?.team2Name ?: "Team B"

        binding.rvBatsmen.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBowlers.layoutManager = LinearLayoutManager(requireContext())

        batsmanAdapter = BatsmanAdapter()
        bowlerAdapter = BowlerAdapter()
        binding.rvBatsmen.adapter = batsmanAdapter
        binding.rvBowlers.adapter = bowlerAdapter

        highlightTab(true)

        binding.btnTeamA.setOnClickListener {
            showingTeamA = true
            highlightTab(true)
            ScoreCardTeamA?.let { updateUI(it) } ?: fetchScoreCard(true)
        }

        binding.btnTeamB.setOnClickListener {
            showingTeamA = false
            highlightTab(false)
            ScoreCardTeamB?.let { updateUI(it) } ?: fetchScoreCard(false)
        }

        fetchScoreCard(true)

    }

    private fun fetchScoreCard(isTeamA: Boolean) {
        val match = matchResponse ?: return
        val matchId = match.id ?: return
        val teamId = if (isTeamA) match.team1Id else match.team2Id
        teamId ?: return

        if (isTeamA == showingTeamA) {
            showLoadingState()
        }

        lifecycleScope.launch {
            try {
                val response = api.getScoreCard(matchId, teamId)
                if (response.isSuccessful) {
                    val scorecard = response.body()
                    if (scorecard != null) {
                        if (isTeamA) ScoreCardTeamA = scorecard else ScoreCardTeamB = scorecard
                        if (isTeamA == showingTeamA) updateUI(scorecard)
                    } else {
                        if (isTeamA == showingTeamA) showEmptyState()
                    }
                } else {
                    requireContext().toastShort("HTTP Error: ${response.code()}")
                    if (isTeamA == showingTeamA) showEmptyState()
                }
            } catch (e: Exception) {
                requireContext().toastShort("Exception: ${e.message}")
                if (isTeamA == showingTeamA) showEmptyState()
            }
        }
    }
    private fun updateUI(scorecard: ScorecardResponse){
        hideLoadingState()
        hideEmptyState()
        showContentView()
        batsmanAdapter.updateData(scorecard.batsmanScores)
        bowlerAdapter.updateData(scorecard.bowlerScores)
        binding.tvExtras.text =    "Extras   ${scorecard.extras}"
        binding.tvTotal.text =    " Total    ${scorecard.totalRuns}"
        binding.tvOversInfo.text = "Overs    ${scorecard.overs}.${scorecard.balls}"
    }

    private fun showLoadingState() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.contentScrollView.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
        binding.progressLoading.indeterminateTintList =
            ColorStateList.valueOf(0xFFE31212.toInt())
    }

    private fun hideLoadingState() {
        binding.progressLoading.visibility = View.GONE
        binding.progressLoading.indeterminateTintList =
            ColorStateList.valueOf(0xFFE31212.toInt())
    }

    private fun showEmptyState() {
        binding.progressLoading.visibility = View.GONE
        binding.contentScrollView.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        binding.emptyStateContainer.visibility = View.GONE
    }

    private fun showContentView() {
        binding.contentScrollView.visibility = View.VISIBLE
    }

    private fun highlightTab(isTeamA: Boolean) {
        val activeColor = 0xFFE31212.toInt()
        val inactiveColor = 0xFF333333.toInt()
        val activeText = 0xFFFFFFFF.toInt()
        val inactiveText = 0xFFAAAAAA.toInt()

        binding.btnTeamA.setBackgroundColor(if (isTeamA) activeColor else inactiveColor)
        binding.btnTeamA.setTextColor(if (isTeamA) activeText else inactiveText)
        binding.btnTeamB.setBackgroundColor(if (isTeamA) inactiveColor else activeColor)
        binding.btnTeamB.setTextColor(if (isTeamA) inactiveText else activeText)
        
        showContentView()
    }

    private fun registerSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected    -> {}
                    is SocketState.Error        -> requireContext().toastShort("Socket Error: ${state.message}")
                    is SocketState.Disconnected -> {}
                }
            }
        }
    }
    fun onSocketUpdate() {
        if (_binding == null) return
        fetchScoreCard(showingTeamA)
    }

    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }

    override fun onResume() {
        super.onResume()
        registerSocketListeners()
        fetchScoreCard(showingTeamA)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            registerSocketListeners()
            fetchScoreCard(showingTeamA)
        } else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): ScoreCardFragment {
            return ScoreCardFragment().apply {
                arguments = Bundle().apply { putSerializable("match_response", match) }
            }
        }
    }
}