package com.example.fypproject.Activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.Adapter.TeamSpinnerAdapter
import com.example.fypproject.DTO.FixturesRequest
import com.example.fypproject.DTO.TeamDTO
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityCreateFixtureBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateFixtureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateFixtureBinding
    private val teamList = mutableListOf<TeamDTO>()
    private val venueList = listOf(
        "Shahbaz Sharif Sport Complex",
        "Divisional Public School",
        "BIIT Ground",
        "Post Graduate College"
    )
    private var tournamentId: Long = -1L
    private var sportId: Long = -1L

    private var isUpdatingSpinners = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateFixtureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tournamentId = intent.getLongExtra("tournamentId", -1L)
        sportId = intent.getLongExtra("sportId", -1L)
        updateFixtureTypeUI(sportId)

        if (tournamentId == -1L) {
            toastShort("Invalid tournament")
            finish()
            return
        }

        setupVenueSpinner()
        setupDatePicker()
        setupTimePicker()
        fetchTeams()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { createFixture() }
    }

    private fun updateFixtureTypeUI(sportId: Long) {
        when (sportId) {
            1L -> binding.etOvers.visibility = View.VISIBLE
            else -> binding.etOvers.visibility = View.GONE
        }
    }

    // ─────────────────────────────────────────────
    // TEAM SPINNERS
    // ─────────────────────────────────────────────

    private fun setupTeamSpinners() {
        updateTeamSpinners(selectedTeam1Id = null, selectedTeam2Id = null)
    }

    private fun updateTeamSpinners(selectedTeam1Id: Long?, selectedTeam2Id: Long?) {
        if (isUpdatingSpinners) return
        isUpdatingSpinners = true

        // Team1 spinner — Team2 ki selected team hatao
        val team1List = if (selectedTeam2Id != null)
            teamList.filter { it.id != selectedTeam2Id }
        else
            teamList.toList()

        val adapter1 = TeamSpinnerAdapter(this, team1List)
        binding.spinnerTeam1.adapter = adapter1

        // Pehle se jo select tha usse wapas select karo
        if (selectedTeam1Id != null) {
            val pos1 = team1List.indexOfFirst { it.id == selectedTeam1Id }
            if (pos1 >= 0) binding.spinnerTeam1.setSelection(pos1)
        }

        // Team2 spinner — Team1 ki selected team hatao
        val team2List = if (selectedTeam1Id != null)
            teamList.filter { it.id != selectedTeam1Id }
        else
            teamList.toList()

        val adapter2 = TeamSpinnerAdapter(this, team2List)
        binding.spinnerTeam2.adapter = adapter2

        // Pehle se jo select tha usse wapas select karo
        if (selectedTeam2Id != null) {
            val pos2 = team2List.indexOfFirst { it.id == selectedTeam2Id }
            if (pos2 >= 0) binding.spinnerTeam2.setSelection(pos2)
        }

        isUpdatingSpinners = false
    }

    private fun setupSpinnerListeners() {

        binding.spinnerTeam1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (isUpdatingSpinners) return
                val selectedTeam1 = parent.getItemAtPosition(position) as? TeamDTO
                val selectedTeam2 = binding.spinnerTeam2.selectedItem as? TeamDTO
                updateTeamSpinners(
                    selectedTeam1Id = selectedTeam1?.id,
                    selectedTeam2Id = selectedTeam2?.id
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerTeam2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (isUpdatingSpinners) return
                val selectedTeam1 = binding.spinnerTeam1.selectedItem as? TeamDTO
                val selectedTeam2 = parent.getItemAtPosition(position) as? TeamDTO
                updateTeamSpinners(
                    selectedTeam1Id = selectedTeam1?.id,
                    selectedTeam2Id = selectedTeam2?.id
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // ─────────────────────────────────────────────
    // FETCH TEAMS
    // ─────────────────────────────────────────────

    private fun fetchTeams() {
        showLoading(true)
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = api.getTeamsByTournamentId(tournamentId)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    teamList.clear()
                    teamList.addAll(response.body()!!)
                    setupTeamSpinners()       // Filtered spinners setup karo
                    setupSpinnerListeners()   // Listeners attach karo
                    checkEmptyState()
                } else {
                    toastShort("No teams found")
                    checkEmptyState()
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
                checkEmptyState()
            } finally {
                showLoading(false)
                binding.btnBack.isEnabled = true
            }
        }
    }

    // ─────────────────────────────────────────────
    // VENUE SPINNER
    // ─────────────────────────────────────────────

    private fun setupVenueSpinner() {
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            venueList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVenue.adapter = adapter
    }

    // ─────────────────────────────────────────────
    // DATE & TIME PICKERS
    // ─────────────────────────────────────────────

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    binding.etDate.setText("%04d-%02d-%02d".format(y, m + 1, d))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTimePicker() {
        binding.etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, h, m ->
                    binding.etTime.setText("%02d:%02d".format(h, m))
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    // ─────────────────────────────────────────────
    // CREATE FIXTURE
    // ─────────────────────────────────────────────

    private fun createFixture() {

        if (teamList.isEmpty()) {
            toastShort("Teams not loaded yet")
            return
        }

        val team1 = binding.spinnerTeam1.selectedItem as? TeamDTO
        val team2 = binding.spinnerTeam2.selectedItem as? TeamDTO

        if (team1 == null || team2 == null) {
            toastShort("Please select teams")
            return
        }

        if (team1.id == team2.id) {
            toastShort("Team 1 and Team 2 cannot be same")
            return
        }

        val dateText = binding.etDate.text.toString().trim()
        val timeText = binding.etTime.text.toString().trim()

        if (dateText.isEmpty() || timeText.isEmpty()) {
            toastShort("Date and time required")
            return
        }

        if (!dateText.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            toastShort("Invalid date format")
            return
        }

        val parsedTime = try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).parse(timeText)
        } catch (e: Exception) {
            null
        }

        if (parsedTime == null) {
            toastShort("Invalid time format")
            return
        }

        val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(parsedTime)

        val overs = if (binding.etOvers.visibility == View.VISIBLE) {
            val text = binding.etOvers.text.toString().trim()
            if (text.isEmpty()) {
                toastShort("Enter overs")
                return
            }
            text.toIntOrNull() ?: run {
                toastShort("Invalid overs value")
                return
            }
        } else null

        val scorerIdText = binding.etScorerId.text.toString().trim()
        val mediaScorerIdText = binding.etMediaScorerId.text.toString().trim()
        val scorerId = if (scorerIdText.isEmpty()) null else scorerIdText
        val mediaScorerId = if (mediaScorerIdText.isEmpty()) null else mediaScorerIdText

        val fixtureRequest = FixturesRequest(
            tournamentId = tournamentId,
            team1Id = team1.id,
            team2Id = team2.id,
            venue = binding.spinnerVenue.selectedItem.toString(),
            date = dateText,
            time = formattedTime,
            overs = overs ?: 0,
            scorerId = scorerId,
            mediaScorerUsername = mediaScorerId
        )

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = api.createFixture(fixtureRequest)
                if (response.isSuccessful) {
                    toastShort("Fixture created successfully")
                    finish()
                } else {
                    toastLong(NetworkUi.userMessage(response, "Fixture creation failed"))
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    // ─────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility =
            if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnBack.isEnabled = !isLoading
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        val isEmpty = teamList.isEmpty()
        if (isEmpty) {
            toastShort("No teams available")
            binding.btnSave.isEnabled = false
        } else {
            binding.btnSave.isEnabled = true
        }
    }
}