package com.example.fypproject.Fragment

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
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
    private var currentTeamStatus: String = ""
    private var creatorPlayerId: Long? = null

    // ── canEdit: sirf creator DRAFT ya REJECTED status mein edit kar sakta hai ──
    private val isCreator get() = creatorPlayerId == playerId
    private val canEdit get() = isCreator &&
            (currentTeamStatus == "DRAFT" || currentTeamStatus == "REJECTED")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragementMyTeamBinding.bind(view)

        val prefs = requireContext()
            .getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)

        tournamentId = arguments?.getLong("tournamentId") ?: -1L
        sportId      = arguments?.getLong("sportId") ?: -1L
        accountId    = prefs.getLong("id", -1L)
        playerId     = prefs.getLong("playerId", -1L)

        setupUI()
        checkTeamExists()
    }

    private fun setupUI() {
        playerAdapter = PlayerAdapter(
            canRemove = false,
            onRemove  = { pid -> handleRemovePlayer(pid) }
        )
        binding.rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayers.adapter = playerAdapter

        binding.btnAddInitialTeam.setOnClickListener { showCreateTeamDialog() }
        binding.btnReuseTeam.setOnClickListener      { showReuseTeamSheet()   }
        binding.btnFilterOrAdd.setOnClickListener    { addPlayerToTeam()      }
        binding.btnSendRequest.setOnClickListener    { submitTeamRequest()    }

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
                val response = api.getTeamByTournamentAndAccount(tournamentId, accountId)
                if (response.isSuccessful && response.body() != null) {
                    val team = response.body()!!
                    currentTeamId      = team.teamId
                    currentTeamPlayers = team.players
                    currentTeamStatus  = team.teamStatus ?: ""
                    creatorPlayerId    = team.creatorPlayerId

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
        binding.btnAddInitialTeam.visibility    = View.GONE
        binding.btnReuseTeam.visibility         = View.GONE

        binding.tvTeamNameDisplay.text = team.teamName ?: "My Team"
        binding.tvTeamStatus.text      = team.teamStatus

        // ── canEdit ke mutabiq adapter update ──
        playerAdapter.setCanRemove(canEdit)
        playerAdapter.submitList(team.players)

        // ── REJECTED team ke liye green "Re-submit" button ──
        if (canEdit && currentTeamStatus == "REJECTED") {
            binding.btnSendRequest.text = "Re-submit Team for Approval"
            binding.btnSendRequest.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(
                        requireContext(), android.R.color.holo_green_dark))
        } else {
            binding.btnSendRequest.text = "Register Full Team"
            binding.btnSendRequest.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E31212"))
        }

        binding.btnSendRequest.isEnabled = true
    }

    private fun showCreateTeamOnly() {
        binding.layoutTeamManagement.visibility = View.GONE
        binding.btnAddInitialTeam.visibility    = View.VISIBLE
        binding.btnReuseTeam.visibility         = View.VISIBLE  // "Reuse" button bhi dikhao
    }

    // ── Player Remove ─────────────────────────────────────────────────────────
    private fun handleRemovePlayer(pid: Long) {
        if (currentTeamId == null) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Player")
            .setMessage("Is player ko team se remove karna chahte ho?")
            .setPositiveButton("Remove") { _, _ ->
                doRemovePlayer(pid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doRemovePlayer(pid: Long) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api.removePlayerFromTeam(currentTeamId!!, pid)
                toastShort("Player removed")
                checkTeamExists()
            } catch (e: Exception) {
                toastShort(e.message ?: "Failed to remove player")
            } finally {
                setLoading(false)
            }
        }
    }

    // ── Reuse Team ────────────────────────────────────────────────────────────
    private fun showReuseTeamSheet() {
        ReuseTeamBottomSheet.newInstance(tournamentId) {
            checkTeamExists()   // success callback — team refresh karo
        }.show(childFragmentManager, "reuse_team")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Baaki existing functions same rahenge
    // ─────────────────────────────────────────────────────────────────────────

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
        if (allAvailablePlayers.isEmpty()) return
        val names = allAvailablePlayers.map { "${it.name} (${it.username})" }
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, names)
        binding.spinnerPlayers.setAdapter(adapter)
        binding.spinnerPlayers.threshold = 1
        binding.spinnerPlayers.setOnClickListener { binding.spinnerPlayers.showDropDown() }
        binding.spinnerPlayers.setOnItemClickListener { _, _, _, _ ->
            val text = binding.spinnerPlayers.text.toString()
            selectedPlayer = allAvailablePlayers.find { "${it.name} (${it.username})" == text }
        }
    }

    private fun addPlayerToTeam() {
        if (selectedPlayer == null) { toastShort("Please select a player"); return }
        if (currentTeamId == null)  { toastShort("Team not found"); return }
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
                } else toastShort("Request failed")
            } catch (e: Exception) {
                toastShort(e.message ?: "Error")
            } finally { setLoading(false) }
        }
    }

    private fun submitTeamRequest() {
        if (currentTeamId == null || playerId == -1L) {
            toastShort("Invalid team/player"); return
        }
        val total = currentTeamPlayers.size
        val (min, max) = when (sportId) {
            1L         -> 11 to 15   // Cricket
            2L         -> 7  to 11   // Futsal
            3L         -> 7  to 12   // Volleyball
            4L, 5L, 6L -> 1  to 3    // Badminton / Table Tennis / Chess
            7L         -> 8  to 11   // Tug of War
            8L         -> 1  to 1    // Ludo
            9L         -> 5 to 16   // Hockey
            else       -> { toastShort("Invalid sport"); return }
        }
        if (total < min) { toastShort("Minimum $min players required"); return }
        if (total > max) { toastShort("Maximum $max players allowed"); return }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.createTeamRequest(
                    TeamRequest(teamId = currentTeamId!!, playerId = playerId,
                        tournamentId = tournamentId))
                if (response.isSuccessful) {
                    toastShort(if (currentTeamStatus == "REJECTED")
                        "Team re-submitted!" else "Team submitted")
                    checkTeamExists()
                } else toastShort("Submit failed")
            } catch (e: Exception) {
                toastShort(e.message ?: "Error")
            } finally { setLoading(false) }
        }
    }

    private fun showCreateTeamDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_team, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etTeamName)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView).create()
        dialogView.findViewById<View>(R.id.btnSaveTeam).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) createTeam(name, dialog)
            else toastShort("Team name required")
        }
        dialog.show()
    }

    private fun createTeam(name: String, dialog: AlertDialog) {
        if (playerId == -1L) {
            toastShort("Player profile not found. Please login again.")
            return
        }
        if (tournamentId == -1L) {
            toastShort("Tournament not found. Please try again.")
            return
        }
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.createTeam(tournamentId, playerId, CreateTeamRequestDto(name))
                if (response.isSuccessful) {
                    dialog.dismiss(); toastShort("Team created"); checkTeamExists()
                } else {
                    val errorBody = response.errorBody()?.string()
                    toastShort("Create failed (${response.code()}): ${errorBody ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                toastShort(e.message ?: "Error")
            } finally { setLoading(false) }
        }
    }

    private fun setLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
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