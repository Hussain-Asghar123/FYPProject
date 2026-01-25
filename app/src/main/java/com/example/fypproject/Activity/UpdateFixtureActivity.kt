package com.example.fypproject.Activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.Adapter.TeamSpinnerAdapter
import com.example.fypproject.DTO.FixturesResponse
import com.example.fypproject.DTO.TeamDTO
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityUpdateFixtureBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class UpdateFixtureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateFixtureBinding

    private val teamList = mutableListOf<TeamDTO>()
    private val venueList = listOf(
        "Shahbaz Sharif Sport Complex",
        "Divisional Public School",
        "Post Graduate College"
    )

    private var tournamentId = -1L
    private var sportId = -1L
    private var matchId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUpdateFixtureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matchId = intent.getLongExtra("matchId", -1L)
        tournamentId = intent.getLongExtra("tournamentId", -1L)
        sportId = intent.getLongExtra("sportId", -1L)



        if (matchId == -1L) {
            toastShort("Invalid match")
            finish()
            return
        }

        setupVenueSpinner()
        setupDatePicker()
        setupTimePicker()

        loadMatch(matchId)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { updateMatch() }
    }

    private fun loadMatch(id: Long) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.getMatchById(id)
                if (response.isSuccessful && response.body() != null) {
                    val fixture = response.body()!!
                    tournamentId=fixture.tournamentId
                    sportId=fixture.sportId
                    bindSimpleFields(fixture)
                    fetchTeams(fixture)
                } else {
                    toastLong(NetworkUi.userMessage(response, "Failed to load match"))
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun fetchTeams(fixture: FixturesResponse) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.getTeamsByTournamentId(fixture.tournamentId)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {

                    teamList.clear()
                    teamList.addAll(response.body()!!)

                    val adapter = TeamSpinnerAdapter(this@UpdateFixtureActivity, teamList)
                    binding.spinnerTeam1.adapter = adapter
                    binding.spinnerTeam2.adapter = adapter

                    val t1Index = teamList.indexOfFirst { it.id == fixture.team1Id }
                    val t2Index = teamList.indexOfFirst { it.id == fixture.team2Id }

                    if (t1Index != -1) binding.spinnerTeam1.setSelection(t1Index)
                    if (t2Index != -1) binding.spinnerTeam2.setSelection(t2Index)
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

    private fun bindSimpleFields(fixture: FixturesResponse) {
        binding.etDate.setText(fixture.date)
        binding.etTime.setText(fixture.time)
        binding.etOvers.setText(fixture.overs.toString())
        binding.etScorerId.setText(fixture.scorerId)
        binding.spinnerVenue.setSelection(venueList.indexOf(fixture.venue))
    }

    private fun updateMatch() {

        val team1Pos = binding.spinnerTeam1.selectedItemPosition
        val team2Pos = binding.spinnerTeam2.selectedItemPosition

        if (team1Pos == team2Pos) {
            toastShort("Same team not allowed")
            return
        }

        val team1 = teamList[team1Pos]
        val team2 = teamList[team2Pos]

        val updated = FixturesResponse(
            id = matchId,
            tournamentId = tournamentId,
            tournamentName = "",
            team1Id = team1.id,
            team1Name = team1.name,
            team2Id = team2.id,
            team2Name = team2.name,
            venue = binding.spinnerVenue.selectedItem.toString(),
            date = binding.etDate.text.toString(),
            time = binding.etTime.text.toString(),
            sportId = sportId,
            overs = binding.etOvers.text.toString().toInt(),
            scorerId = binding.etScorerId.text.toString()
        )

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.updateMatch(matchId, updated)
                if (response.isSuccessful) {
                    toastShort("Updated")
                    finish()
                } else {
                    toastLong(NetworkUi.userMessage(response, "Update failed"))
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

    private fun setupVenueSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, venueList)
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
}
