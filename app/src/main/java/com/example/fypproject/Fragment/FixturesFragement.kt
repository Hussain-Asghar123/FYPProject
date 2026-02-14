package com.example.fypproject.Fragment

import android.content.Context.MODE_PRIVATE
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Activity.CreateFixtureActivity
import com.example.fypproject.Activity.StartScoringActivity
import com.example.fypproject.Activity.UpdateFixtureActivity
import com.example.fypproject.Adapter.FixturesAdapter
import com.example.fypproject.DTO.FixturesResponse
import com.example.fypproject.DTO.MatchStatus
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.FragmentFixturesBinding
import kotlinx.coroutines.launch

class FixturesFragement : Fragment(R.layout.fragment_fixtures) {

    private var _binding: FragmentFixturesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FixturesAdapter

    private var tournamentId: Long = -1L
    private var sportId: Long = -1L

    private var matchId: Long = -1L
    private val filteredList = mutableListOf<FixturesResponse>()
    private var role: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFixturesBinding.bind(view)

        role = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("role", "") ?: ""

        tournamentId = arguments?.getLong("tournamentId") ?: -1L
        sportId = arguments?.getLong("sportId") ?: -1L
        matchId = arguments?.getLong("matchId") ?: -1L

        setupRecycler()
        checkAdminButton()
        setupAddButton()
        loadFixtures()
    }

    private fun checkAdminButton() {
        binding.btnAddFixture.visibility = if (role.equals("ADMIN", true)) View.VISIBLE else View.GONE
    }

    private fun setupRecycler() {
        adapter = FixturesAdapter(
            matches = filteredList,
            role = role,
            onClick = { fixture ->
                openStartScoring(fixture)
            },
            onEdit = { fixture ->
                openUpdate(fixture)
            }
        )
        binding.rvFixtures.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFixtures.adapter = adapter
    }

    private fun loadFixtures() {
        if (tournamentId == -1L) {
            toastShort("Invalid tournament")
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val response = api.getMatchesByTournament(tournamentId)
                if (response.isSuccessful) {
                    val matches = response.body() ?: emptyList()
                    filteredList.clear()
                    filteredList.addAll(
                        matches.filter {
                            it.status== MatchStatus.UPCOMING || it.status == MatchStatus.LIVE
                        }
                    )
                    adapter.notifyDataSetChanged()
                } else {
                    toastLong(NetworkUi.userMessage(response, "No fixtures found"))
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
        binding.btnAddFixture.isEnabled = !isLoading
    }

    private fun openStartScoring(fixture: FixturesResponse) {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString("role", "")
        if (role.equals("ADMIN")) {
            val intent = Intent(requireContext(), StartScoringActivity::class.java)
            intent.putExtra("matchId", fixture.id)
            intent.putExtra("tournamentId", fixture.tournamentId)
            intent.putExtra("sportId", fixture.sportId)
            startActivity(intent)
        }
    }

    private fun openUpdate(fixture: FixturesResponse) {
        val intent = Intent(requireContext(), UpdateFixtureActivity::class.java)
        intent.putExtra("matchId", fixture.id)
        intent.putExtra("tournamentId", fixture.tournamentId)
        intent.putExtra("sportId", fixture.sportId)
        startActivity(intent)
    }

    private fun setupAddButton() {
        binding.btnAddFixture.setOnClickListener {
            val intent = Intent(requireContext(), CreateFixtureActivity::class.java)
            intent.putExtra("tournamentId", tournamentId)
            intent.putExtra("sportId", sportId)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tournamentId: Long, sportId: Long): FixturesFragement {
            val fragment = FixturesFragement()
            val args = Bundle()
            args.putLong("tournamentId", tournamentId)
            args.putLong("sportId", sportId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onResume() {
        super.onResume()
        role = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("role", "") ?: ""
        checkAdminButton()
        adapter.updateRole(role)
        loadFixtures()
    }
}