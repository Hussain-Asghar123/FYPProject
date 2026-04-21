package com.example.fypproject.CricketFragment

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.BallByBallAdapter
import com.example.fypproject.ScoringDTO.Ball
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Sockets.JsonConverter
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.BallsFragmentBinding
import kotlinx.coroutines.launch

class BallsFragment : Fragment(R.layout.balls_fragment) {

    private var _binding: BallsFragmentBinding? = null
    private val binding get() = _binding!!

    private var matchResponse: MatchResponse? = null
    private val SOCKET_KEY = "BallsFragment"
    private var activeTeamId: Long? = null

    private lateinit var ballAdapter: BallByBallAdapter
    private val ballList = mutableListOf<Ball>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = BallsFragmentBinding.bind(view)

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }

        setupRecyclerView()
        setupTeamTabs()

        matchResponse?.let {
            activeTeamId = it.team1Id
            binding.tvTeam1.text = it.team1Name?.uppercase() ?: "TEAM 1"
            binding.tvTeam2.text = it.team2Name?.uppercase() ?: "TEAM 2"
            fetchBalls()
        }

        setupSocketConnection()
    }

    private fun setupRecyclerView() {
        ballAdapter = BallByBallAdapter()
        binding.rvBallByBall.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply{
                reverseLayout=true
                stackFromEnd = false
            }
            adapter = ballAdapter
        }
    }

    private fun setupTeamTabs() {
        binding.tvTeam1.setOnClickListener {
            activeTeamId = matchResponse?.team1Id
            highlightTab(true)
            resetAndFetch()
        }

        binding.tvTeam2.setOnClickListener {
            activeTeamId = matchResponse?.team2Id
            highlightTab(false)
            resetAndFetch()
        }

        highlightTab(true)
    }

    private fun highlightTab(isTeam1: Boolean) {
        if (isTeam1) {
            binding.tvTeam1.setBackgroundResource(R.drawable.tab_selected)
            binding.tvTeam1.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.tvTeam2.setBackgroundResource(R.drawable.tab_unselected)
            binding.tvTeam2.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
        } else {
            binding.tvTeam2.setBackgroundResource(R.drawable.tab_selected)
            binding.tvTeam2.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.tvTeam1.setBackgroundResource(R.drawable.tab_unselected)
            binding.tvTeam1.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
        }
    }

    private fun resetAndFetch() {
        ballList.clear()
        ballAdapter.submitList(emptyList())
        fetchBalls()
    }

    private fun fetchBalls() {
        val matchId = matchResponse?.id ?: return
        val teamId = activeTeamId ?: return

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = api.getMatchBalls(matchId, teamId)

                if (response.isSuccessful) {
                    val balls = response.body() ?: emptyList()
                    ballList.clear()
                    ballList.addAll(balls)
                    ballAdapter.submitList(ballList.toList())

                    if (ballList.isNotEmpty()) {
                        binding.rvBallByBall.scrollToPosition(0)
                    } else {
                        showEmpty()
                    }
                } else {
                    showEmpty()
                }

            } catch (e: Exception) {
                requireContext().toastShort("Error: ${e.localizedMessage}")
                showEmpty()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvBallByBall.visibility  = if (show) View.GONE   else View.VISIBLE
        binding.emptyLayout.visibility   = View.GONE
    }

    private fun showEmpty() {
        binding.emptyLayout.visibility   = View.VISIBLE
        binding.rvBallByBall.visibility  = View.GONE
        binding.loadingLayout.visibility = View.GONE
    }

    private fun setupSocketConnection() {
        matchResponse?.id?.let { _ ->

            WebSocketManager.addStateListener(SOCKET_KEY) { state ->
                activity?.runOnUiThread {
                    if (state is SocketState.Error) {
                        requireContext().toastShort("Socket Error: ${state.message}")
                    }
                }
            }

            WebSocketManager.addMessageListener(SOCKET_KEY) {
            }
        }
    }
    fun onSocketUpdate() {
        if (_binding == null) return
        fetchBalls()
    }

    private fun registerSocketListeners() = setupSocketConnection()

    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }

    override fun onResume() {
        super.onResume()
        registerSocketListeners()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) registerSocketListeners()
        else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse): BallsFragment {
            return BallsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }
}