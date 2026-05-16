package com.example.fypproject.Fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Activity.ActivityGernateFixtures
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

    private val allFixtures = mutableListOf<FixturesResponse>()
    private val filteredList = mutableListOf<FixturesResponse>()
    private var role: String = ""

    // ── Activity Result Launcher — Generate ya Create activity se wapas aane par reload ──
    private val generateLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadFixtures()   // Naye fixtures load karo
        }
    }

    private val createLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadFixtures()
        }
    }

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
        setupGenerateButton()
        loadFixtures()
    }

    private fun checkAdminButton() {
        val isAdmin = role.equals("ADMIN", ignoreCase = true)
        binding.btnAddFixture.visibility = if (isAdmin) View.VISIBLE else View.GONE
        // Generate button bhi admin ke liye show karo (XML mein id: btnGenerate)
        binding.btnGenerate?.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun setupRecycler() {
        adapter = FixturesAdapter(
            matches = filteredList,
            role = role,
            onClick = { fixture -> openStartScoring(fixture) },
            onEdit  = { fixture -> openUpdate(fixture) }
        )
        binding.rvFixtures.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFixtures.adapter = adapter
    }

    private fun loadFixtures() {
        if (tournamentId == -1L) return
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                // Teams aur Fixtures dono saath fetch karo
                val fixturesResponse = api.getMatchesByTournament(tournamentId)
                val teamsResponse = api.getTeamsByTournament(tournamentId) // ← ADD

                // Team count directly teams se lo
                if (teamsResponse.isSuccessful) {
                    latestTeamCount = teamsResponse.body()?.size ?: 0
                }

                if (fixturesResponse.isSuccessful) {
                    val matches = fixturesResponse.body() ?: emptyList()
                    allFixtures.clear()
                    allFixtures.addAll(matches)
                    filteredList.clear()
                    filteredList.addAll(matches.filter {
                        it.status == "UPCOMING" || it.status == "LIVE"
                    })
                    adapter.notifyDataSetChanged()
                    checkEmptyState()
                } else {
                    toastLong(NetworkUi.userMessage(fixturesResponse, "No fixtures found"))
                    checkEmptyState()
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
                checkEmptyState()
            } finally {
                setLoading(false)
            }
        }
    }

    private var latestTeamCount = 0

    private fun setupAddButton() {
        binding.btnAddFixture.setOnClickListener {
            val intent = Intent(requireContext(), CreateFixtureActivity::class.java)
            intent.putExtra("tournamentId", tournamentId)
            intent.putExtra("sportId", sportId)
            createLauncher.launch(intent)
        }
    }

    private fun setupGenerateButton() {
        binding.btnGenerate?.setOnClickListener {
            val intent = Intent(requireContext(), ActivityGernateFixtures::class.java)
            intent.putExtra("tournamentId", tournamentId)
            intent.putExtra("sportId", sportId)
            intent.putExtra("teamCount", latestTeamCount)
            generateLauncher.launch(intent)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAddFixture.isEnabled = !isLoading
    }

    private fun checkEmptyState() {
        if (_binding == null) return
        val isEmpty = filteredList.isEmpty()
        binding.rvFixtures.visibility  = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun openStartScoring(fixture: FixturesResponse) {
        val role = requireContext()
            .getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("role", "")
        if (role.equals("ADMIN", ignoreCase = true)) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        role = requireActivity()
            .getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("role", "") ?: ""
        checkAdminButton()
        adapter.updateRole(role)
        loadFixtures()
    }

    companion object {
        fun newInstance(tournamentId: Long, sportId: Long): FixturesFragement {
            val fragment = FixturesFragement()
            val args = Bundle().apply {
                putLong("tournamentId", tournamentId)
                putLong("sportId", sportId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}