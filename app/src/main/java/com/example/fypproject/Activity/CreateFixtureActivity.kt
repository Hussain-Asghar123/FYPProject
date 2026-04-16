package com.example.fypproject.Activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
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
        "Post Graduate College"
    )
    private var tournamentId: Long = -1L
    private var sportId: Long = -1L


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
            1L -> {
                binding.etOvers.visibility = View.VISIBLE
                binding.etSets.visibility = View.GONE
            }

            4L, 5L -> {
                binding.etSets.visibility = View.VISIBLE
                binding.etOvers.visibility = View.GONE
            }

            else -> {
                binding.etOvers.visibility = View.GONE
                binding.etSets.visibility = View.GONE
            }
        }
    }


    private fun fetchTeams() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.getTeamsByTournamentId(tournamentId)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {

                    teamList.clear()
                    teamList.addAll(response.body()!!)

                    val adapter = TeamSpinnerAdapter(this@CreateFixtureActivity, teamList)
                    binding.spinnerTeam1.adapter = adapter
                    binding.spinnerTeam2.adapter = adapter
                    binding.btnSave.isEnabled = true
                } else {
                    toastShort("No teams found")
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setupVenueSpinner() {
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            venueList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVenue.adapter = adapter
    }

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
        val scorerId = if (scorerIdText.isEmpty()) null else scorerIdText

        val fixtureRequest = FixturesRequest(
            tournamentId = tournamentId,
            team1Id = team1.id,
            team2Id = team2.id,
            venue = binding.spinnerVenue.selectedItem.toString(),
            date = dateText,
            time = formattedTime,
            overs = overs ?: 0,
            scorerId = scorerId
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

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnSave.isEnabled = !isLoading
    }
}


