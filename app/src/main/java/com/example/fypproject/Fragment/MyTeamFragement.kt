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
    private var accountId: Long = -1
    private var playerId: Long = -1
    private var currentTeamId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragementMyTeamBinding.bind(view)

        val prefs = requireContext().getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)

        tournamentId = arguments?.getLong("tournamentId") ?: -1
        accountId = prefs.getLong("id", -1)
        playerId = prefs.getLong("playerId", -1)

        setupUI()
        checkTeamExists()
    }

    private fun setupUI() {
        playerAdapter = PlayerAdapter()
        binding.rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayers.adapter = playerAdapter

        binding.btnAddInitialTeam.setOnClickListener {
            showCreateTeamDialog()
        }

        binding.btnFilterOrAdd.setOnClickListener {
            addPlayerToTeam()
        }

        binding.btnSendRequest.setOnClickListener {
            submitTeamRequest()
        }

        binding.spinnerPlayers.setOnItemClickListener { parent, view, position, id ->
            if (position >= 0 && position < allAvailablePlayers.size) {
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
                    currentTeamId = team.teamId
                    currentTeamPlayers = team.players
                    showTeamUI(team)
                    loadAvailablePlayers()
                } else {
                    showCreateTeamOnly()
                }
                setLoading(false)
            } catch (e: Exception) {
                setLoading(false)
                showCreateTeamOnly()
            }
        }
    }

    private fun showTeamUI(team: TeamResponse) {
        binding.layoutTeamManagement.visibility = View.VISIBLE
        binding.btnAddInitialTeam.visibility = View.GONE

        binding.tvTeamNameDisplay.text = team.teamName ?: "My Team"
        binding.tvTeamStatus.text = "DRAFT"

        playerAdapter.submitList(team.players)

        binding.tvTeamStatus.text = team.teamStatus
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
                    allAvailablePlayers = response.body()!!
                    setupDropdownFilter()
                }
            } catch (e: Exception) {
                toastShort("Error: ${e.message}")
            }
        }
    }

    private fun setupDropdownFilter() {
        if (allAvailablePlayers.isEmpty()) {
            toastShort("No players available")
            return
        }

        val playerDisplayNames = allAvailablePlayers.map {
            "${it.username} (-${it.name})"
        }.toMutableList()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            playerDisplayNames
        )

        binding.spinnerPlayers.apply {
            setAdapter(adapter)
            setOnClickListener {
                showDropDown()
            }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    adapter.filter.filter(s)
                }

                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
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
                    us=""
                )
                val response = api.createPlayerRequest(request)
                setLoading(false)

                if (response.isSuccessful) {
                    toastShort("Request sent to ${selectedPlayer!!.name}")
                    binding.spinnerPlayers.setText("", false)
                    selectedPlayer = null
                    checkTeamExists()
                } else {
                    toastShort("Failed to send request")
                }
            } catch (e: Exception) {
                setLoading(false)
                toastShort("Error: ${e.message}")
            }
        }
    }

    private fun submitTeamRequest() {
        if (currentTeamId == null) {
            toastShort("Team not found")
            return
        }

        if (playerId == -1L) {
            toastShort("Player ID not found")
            return
        }

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = TeamRequest(
                    teamId = currentTeamId!!,
                    playerId = playerId
                )

                toastShort("Submitting team...")

                val response = api.createTeamRequest(request)
                setLoading(false)

                if (response.isSuccessful) {
                    toastShort("Team submitted successfully!")
                    checkTeamExists()
                } else {
                    toastShort("Failed to submit team - ${response.code()}")
                }
            } catch (e: Exception) {
                setLoading(false)
                toastShort("Error: ${e.message}")
            }
        }
    }

    private fun showCreateTeamDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_team, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etTeamName)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<View>(R.id.btnSaveTeam).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) {
                createTeam(name, dialog)
            } else toastShort("Team name required")
        }

        dialog.show()
    }

    private fun createTeam(name: String, dialog: androidx.appcompat.app.AlertDialog) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = api.createTeam(tournamentId, playerId, CreateTeamRequestDto(name))
                setLoading(false)
                if (response.isSuccessful && response.body() != null) {
                    currentTeamId = response.body()?.teamId
                    dialog.dismiss()
                    toastShort("Team created")
                    checkTeamExists()
                } else {
                    toastShort("Team create failed")
                }
            } catch (e: Exception) {
                setLoading(false)
                toastShort("Error: ${e.message}")
            }
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
        fun newInstance(tournamentId: Long): MyTeamFragment {
            return MyTeamFragment().apply {
                arguments = Bundle().apply {
                    putLong("tournamentId", tournamentId)
                }
            }
        }
    }
}