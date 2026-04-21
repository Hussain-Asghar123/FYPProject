package com.example.fypproject.Fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TournamentOverViewAdapter
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.FragmentOverviewBinding
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import kotlinx.coroutines.launch

class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    private var tournamentId: Long = -1L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOverviewBinding.bind(view)

        tournamentId = arguments?.getLong("tournamentId") ?: -1L

        binding.rvTopTeams.layoutManager = LinearLayoutManager(requireContext())

        if (tournamentId != -1L) {
            fetchOverviewData(tournamentId)
        }
    }

    private fun fetchOverviewData(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val response = api.getTournamentOverview(id)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    binding.tvTotalTeams.text = data.teams.toString()
                    binding.tvPlayerType.text = data.playerType ?: "N/A"
                    binding.tvStartDate.text = data.startDate ?: "N/A"

                    binding.rvTopTeams.adapter = TournamentOverViewAdapter(data.top)
                    checkEmptyState(data.top.isEmpty())
                } else {
                    toastLong(NetworkUi.userMessage(response))
                    checkEmptyState(true)
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
                checkEmptyState(true)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun checkEmptyState(isEmpty: Boolean) {
        if (_binding == null) return
        binding.rvTopTeams.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tournamentId: Long): OverviewFragment {
            val fragment = OverviewFragment()
            val args = Bundle()
            args.putLong("tournamentId", tournamentId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onResume() {
        super.onResume()
        fetchOverviewData(tournamentId)
    }
}