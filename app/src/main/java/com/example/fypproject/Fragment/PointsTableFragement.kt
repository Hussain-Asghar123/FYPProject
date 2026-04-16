package com.example.fypproject.Fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.PtsTableAdapter
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.databinding.FragementPointsTableBinding
import kotlinx.coroutines.launch

class PointsTableFragement : Fragment(R.layout.fragement_points_table) {

    private var _binding: FragementPointsTableBinding? = null
    private val binding get() = _binding!!
    private var tournamentId: Long = -1L
    private var fallbackSport: String = "cricket"
    private lateinit var ptsTableAdapter: PtsTableAdapter
    private lateinit var emptyStateView: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragementPointsTableBinding.bind(view)
        emptyStateView = binding.tvEmptyState
        tournamentId = arguments?.getLong(ARG_TOURNAMENT_ID) ?: -1L
        fallbackSport = arguments?.getString(ARG_SPORT)?.ifBlank { null } ?: "cricket"


        setLoading(false)
        setEmptyState(false)
        updateHeaderVisibility(isFutsal = isFutsalSport(fallbackSport))

        fetchPointsTable()
    }

    private fun fetchPointsTable() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                setLoading(true)
                val response = api.getPtsTablesByTournament(tournamentId)
                if (response.isSuccessful) {
                    val ptsTable = response.body().orEmpty()
                    val detectedSport = resolveSport(ptsTable.firstOrNull()?.sport, fallbackSport)
                    val isFutsal = isFutsalSport(detectedSport)

                    updateHeaderVisibility(isFutsal)

                    if (isFutsal) {
                        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
                        binding.rvLeaderboard.adapter = PtsTableAdapter(ptsTable, detectedSport)
                    } else {
                        binding.rvLeaderboardCricket.layoutManager = LinearLayoutManager(requireContext())
                        binding.rvLeaderboardCricket.adapter = PtsTableAdapter(ptsTable, detectedSport)
                    }

                    setEmptyState(ptsTable.isEmpty())
                }
            } catch (e: Exception) {
                setEmptyState(true)
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateHeaderVisibility(isFutsal: Boolean) {
        binding.headerCricket.isVisible = !isFutsal
        binding.futsalScrollView.isVisible = isFutsal
        binding.rvLeaderboardCricket.isVisible = !isFutsal
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.isVisible = isLoading
    }

    private fun setEmptyState(isEmpty: Boolean) {
        emptyStateView.isVisible = isEmpty
        binding.rvLeaderboard.isVisible = !isEmpty
    }

    private fun resolveSport(primary: String?, fallback: String): String {
        return normalizeSport(fallback) ?: normalizeSport(primary) ?: "cricket"
    }

    private fun isFutsalSport(sport: String?): Boolean = normalizeSport(sport) == "futsal"

    private fun normalizeSport(sport: String?): String? {
        val normalized = sport?.trim()?.lowercase()
        return when (normalized) {
            "futsal", "football" -> "futsal"
            "cricket" -> "cricket"
            else -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TOURNAMENT_ID = "tournamentId"
        private const val ARG_SPORT = "sport"

        fun newInstance(tournamentId: Long, sport: String? = null): PointsTableFragement {
            val fragment = PointsTableFragement()
            val args = Bundle()
            args.putLong(ARG_TOURNAMENT_ID, tournamentId)
            args.putString(ARG_SPORT, sport)
            fragment.arguments = args
            return fragment
        }
    }
}