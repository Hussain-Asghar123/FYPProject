package com.example.fypproject.Fragment

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.PlayerAdapter
import com.example.fypproject.DTO.*
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.FragementMyTeamBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MyTeamFragment : Fragment(R.layout.fragement_my_team) {

    private var _binding: FragementMyTeamBinding? = null
    private val binding get() = _binding!!

    private lateinit var playerAdapter: PlayerAdapter

    private var allAvailablePlayers: List<PlayerResponse> = emptyList()
    private var currentTeamPlayers: List<Player> = emptyList()
    private var selectedPlayer: PlayerResponse? = null

    private var tournamentId: Long = -1
    private var sportId: Long = -1
    private var accountId: Long = -1
    private var playerId: Long = -1
    private var currentTeamId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragementMyTeamBinding.bind(view)

        val prefs = requireContext()
            .getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)

        tournamentId = arguments?.getLong("tournamentId") ?: -1
        sportId = arguments?.getLong("sportId") ?: -1
        accountId = prefs.getLong("id", -1)
        playerId = prefs.getLong("playerId", -1)

        setupUI()
        checkTeamExists()
    }

    private fun setupUI() {
        playerAdapter = PlayerAdapter()
        binding.rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayers.adapter = playerAdapter

        binding.btnAddInitialTeam.setOnClickListener { showCreateTeamDialog() }
        binding.btnFilterOrAdd.setOnClickListener { addPlayerToTeam() }
        binding.btnSendRequest.setOnClickListener { submitTeamRequest() }

        binding.spinnerPlayers.setOnItemClickListener { _, _, position, _ ->
            if (position in allAvailablePlayers.indices) {
                selectedPlayer = allAvailablePlayers[position]
                toastShort("Selected: ${selectedPlayer!!.name}")
            }
        }
    }

    private fun checkTeamExists() {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response =
                    api.getTeamByTournamentAndAccount(tournamentId, accountId)

                if (response.isSuccessful && response.body() != null) {
                    val team = response.body()!!
                    currentTeamId = team.teamId
                    currentTeamPlayers = team.players
                    showTeamUI(team)
                    loadAvailablePlayers()
                } else {
                    showCreateTeamOnly()
                }
            } catch (_: Exception) {
                showCreateTeamOnly()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showTeamUI(team: TeamResponse) {
        binding.layoutTeamManagement.visibility = View.VISIBLE
        binding.btnAddInitialTeam.visibility = View.GONE

        binding.tvTeamNameDisplay.text = team.teamName ?: "My Team"
        binding.tvTeamStatus.text = team.teamStatus
        playerAdapter.submitList(team.players)
        binding.btnSendRequest.isEnabled = true
    }

    private fun showCreateTeamOnly() {
        binding.layoutTeamManagement.visibility = View.GONE
        binding.btnAddInitialTeam.visibility = View.VISIBLE
    }

    private fun loadAvailablePlayers() {
        if (currentTeamId == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.getAllPlayerAccounts(currentTeamId!!)
                if (response.isSuccessful && response.body() != null) {
                    val currentPlayerIds = currentTeamPlayers.map { it.id.toLong() }.toSet()
                    allAvailablePlayers = response.body()!!
                        .filter { it.playerId !in currentPlayerIds }
                    setupDropdownFilter()
                }
            } catch (e: Exception) {
                toastShort(e.message ?: "Error")
            }
        }
    }

    private fun setupDropdownFilter() {
        if (allAvailablePlayers.isEmpty()) {
            toastShort("No players available")
            return
        }

        val names = allAvailablePlayers.map {
            "${it.name} (${it.username})"
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            names
        )

        binding.spinnerPlayers.setAdapter(adapter)
        binding.spinnerPlayers.inputType = android.text.InputType.TYPE_NULL
        binding.spinnerPlayers.setOnClickListener { binding.spinnerPlayers.showDropDown() }
    }

    private fun addPlayerToTeam() {
        if (selectedPlayer == null) {
            toastShort("Please select a player")
            return
        }
        if (currentTeamId == null) {
            toastShort("Team not found")
            return
        }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = PlayerRequest(
                    playerId = selectedPlayer!!.playerId,
                    teamId = currentTeamId!!,
                    tournamentId = tournamentId,
                    us = ""
                )

                val response = api.createPlayerRequest(request)
                if (response.isSuccessful) {
                    toastShort("Request sent")
                    selectedPlayer = null
                    binding.spinnerPlayers.setText("", false)
                    checkTeamExists()
                } else {
                    toastShort("Request failed")
                }
            } catch (e: Exception) {
                toastShort(e.message ?: "Error")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun submitTeamRequest() {
        if (currentTeamId == null || playerId == -1L) {
            toastShort("Invalid team/player")
            return
        }

        val totalPlayers = currentTeamPlayers.size
        var minPlayers: Int
        var maxPlayers: Int

        when (sportId) {
            1L -> { minPlayers = 11; maxPlayers = 15 }
            2L -> { minPlayers = 7; maxPlayers = 11 }
            3L -> { minPlayers = 7; maxPlayers = 12 }
            4L, 5L, 6L -> { minPlayers = 1; maxPlayers = 3 }
            7L -> { minPlayers = 8; maxPlayers = 11 }
            8L-> { minPlayers = 1; maxPlayers = 1 }
            else -> {
                toastShort("Invalid sport")
                return
            }
        }

        if (totalPlayers < minPlayers) {
            toastShort("Minimum $minPlayers players required")
            return
        }

        if (totalPlayers > maxPlayers) {
            toastShort("Maximum $maxPlayers players allowed")
            return
        }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.createTeamRequest(
                    TeamRequest(
                        teamId       = currentTeamId!!,
                        playerId     = playerId,
                        tournamentId = tournamentId   // ✅ JS se match
                    )
                )
                if (response.isSuccessful) {
                    toastShort("Team submitted")
                    checkTeamExists()
                } else {
                    toastShort("Submit failed")
                }
            } catch (e: Exception) {
                toastShort(e.message ?: "Error")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showCreateTeamDialog() {
        val dialogView =
            layoutInflater.inflate(R.layout.dialog_add_team, null)
        val etName =
            dialogView.findViewById<android.widget.EditText>(R.id.etTeamName)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btnSaveTeam).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) createTeam(name, dialog)
            else toastShort("Team name required")
        }

        dialog.show()
    }

    private fun createTeam(
        name: String,
        dialog: androidx.appcompat.app.AlertDialog
    ) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response =
                    api.createTeam(tournamentId, playerId, CreateTeamRequestDto(name))
                if (response.isSuccessful) {
                    dialog.dismiss()
                    toastShort("Team created")
                    checkTeamExists()
                } else toastShort("Create failed")
            } catch (e: Exception) {
                toastShort(e.message ?: "Error")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(show: Boolean) {
        binding.progressOverlay.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tournamentId: Long, sportId: Long) =
            MyTeamFragment().apply {
                arguments = Bundle().apply {
                    putLong("tournamentId", tournamentId)
                    putLong("sportId", sportId)
                }
            }
    }
}
