package com.example.fypproject.Fragment

import android.os.Bundle
import android.view.View
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragementPointsTableBinding.bind(view)
        tournamentId = arguments?.getLong("tournamentId") ?: -1L
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())

        fetchPointsTable()

    }
    private fun fetchPointsTable() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.getPtsTablesByTournament(tournamentId)
                if (response.isSuccessful && response.body() != null) {
                    val ptsTable = response.body()!!
                    val adapter = PtsTableAdapter(ptsTable)
                    binding.rvLeaderboard.adapter = adapter
                } else {
                    toastLong(NetworkUi.userMessage(response, "No data found"))
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tournamentId: Long): PointsTableFragement {
            val fragment = PointsTableFragement()
            val args = Bundle()
            args.putLong("tournamentId", tournamentId)
            fragment.arguments = args
            return fragment
        }
    }


}