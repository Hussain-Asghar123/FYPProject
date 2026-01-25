package com.example.fypproject.Fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TeamAdapter
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.FragmentsAllTeamsBinding
import kotlinx.coroutines.launch

class AllTeamsFragement : Fragment(R.layout.fragments_all_teams) {

    private var _binding: FragmentsAllTeamsBinding? = null
    private val binding get() = _binding!!
    private var tournamentId: Long = -1L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentsAllTeamsBinding.bind(view)

        tournamentId = arguments?.getLong("tournamentId") ?: -1L

        binding.rvAllTeams.layoutManager = LinearLayoutManager(requireContext())

        fetchTeams()
    }

    private fun fetchTeams() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val response = api.getTeamsByTournament(tournamentId)
                if (response.isSuccessful && response.body() != null) {
                    val teams = response.body()!!
                    val adapter = TeamAdapter(teams)
                    binding.rvAllTeams.adapter = adapter
                } else {
                    toastLong(NetworkUi.userMessage(response, "No teams found"))
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
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
        fun newInstance(tournamentId: Long): AllTeamsFragement {
            val fragment = AllTeamsFragement()
            val args = Bundle()
            args.putLong("tournamentId", tournamentId)
            fragment.arguments = args
            return fragment
        }
    }
}