package com.example.fypproject.Activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.GenerateFixturesRequest
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityGernateFixturesBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class ActivityGernateFixtures : AppCompatActivity() {

    private lateinit var binding: ActivityGernateFixturesBinding

    private var tournamentId = -1L
    private var sportId = -1L
    private var teamCount = 0

    private val tournamentTypes = listOf(
        "ROUND_ROBIN",
        "LEAGUE",
        "KNOCK_OUT",
        "MIXED"
    )

    private val tournamentTypeLabels = listOf(
        "Round Robin",
        "League (Double Round Robin)",
        "Knockout",
        "Mixed (Group + Knockout)"
    )

    private val venueList = listOf(
        "Shahbaz Sharif Sport Complex",
        "Divisional Public School",
        "BIIT Ground",
        "Post Graduate College"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGernateFixturesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tournamentId = intent.getLongExtra("tournamentId", -1L)
        sportId = intent.getLongExtra("sportId", -1L)
        teamCount = intent.getIntExtra("teamCount", 0)

        if (tournamentId == -1L) {
            toastShort("Invalid tournament")
            finish()
            return
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Teams count badge
        binding.tvTeamCount.text = "$teamCount teams registered"

        // Tournament type spinner
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tournamentTypeLabels)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTournamentType.adapter = typeAdapter

        // Venue spinner
        val venueAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, venueList)
        venueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVenue.adapter = venueAdapter

        // Default values
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        binding.etStartDate.setText(today)
        binding.etStartTime.setText("09:00")
        binding.etGapMinutes.setText("120")

        // Show overs only for cricket (sportId == 1)
        binding.etOvers.visibility = if (sportId == 1L) View.VISIBLE else View.GONE

        // Disable generate if not enough teams
        if (teamCount < 2) {
            binding.btnGenerate.isEnabled = false
            binding.btnGenerate.alpha = 0.5f
            toastShort("At least 2 teams required to generate fixtures")
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.etStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    binding.etStartDate.setText("%04d-%02d-%02d".format(y, m + 1, d))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etStartTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, h, m ->
                    binding.etStartTime.setText("%02d:%02d".format(h, m))
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        binding.btnGenerate.setOnClickListener { generateFixtures() }
    }

    private fun generateFixtures() {

        // ── Validation ───────────────────────────────────────────────
        val startDate = binding.etStartDate.text.toString().trim()
        val startTime = binding.etStartTime.text.toString().trim()
        val gapText = binding.etGapMinutes.text.toString().trim()
        val venue = binding.spinnerVenue.selectedItem.toString()

        if (startDate.isEmpty()) {
            toastShort("Please select start date")
            return
        }
        if (startTime.isEmpty()) {
            toastShort("Please select start time")
            return
        }

        val gapMinutes = gapText.toIntOrNull()
        if (gapMinutes == null || gapMinutes < 30) {
            toastShort("Gap must be at least 30 minutes")
            return
        }

        val overs = if (sportId == 1L) {
            val oversText = binding.etOvers.text.toString().trim()
            if (oversText.isEmpty()) {
                toastShort("Please enter overs")
                return
            }
            oversText.toIntOrNull() ?: run {
                toastShort("Invalid overs value")
                return
            }
        } else 0

        val selectedTypeIndex = binding.spinnerTournamentType.selectedItemPosition
        val tournamentType = tournamentTypes[selectedTypeIndex]

        val scorerIdText = binding.etScorerId.text.toString().trim()
        val mediaScorerText = binding.etMediaScorerId.text.toString().trim()

        val request = GenerateFixturesRequest(
            tournamentType = tournamentType,
            startDate = startDate,
            startTime = startTime,
            gapMinutes = gapMinutes,
            venue = venue,
            overs = overs,
            scorerId = scorerIdText.ifEmpty { null },
            mediaScorerUsername = mediaScorerText.ifEmpty { null }
        )

        // ── API Call ─────────────────────────────────────────────────
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.generateFixtures(tournamentId, request)
                if (response.isSuccessful) {
                    toastShort("Fixtures generated successfully!")
                    setResult(RESULT_OK)   // Fragment ko reload karne ke liye signal
                    finish()
                } else {
                    toastLong(NetworkUi.userMessage(response, "Failed to generate fixtures"))
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnGenerate.isEnabled = !isLoading
        binding.btnBack.isEnabled = !isLoading
    }
}