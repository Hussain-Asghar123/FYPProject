package com.example.fypproject.Activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.MatchDTO
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.MatchStatus
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Scoring.CricketScoringActivity
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityStartScoringBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class StartScoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartScoringBinding
    private var matchId: Long = -1L
    private var sportId: Long=-1L
    private var matchData: MatchResponse? = null
    private var selectedTossWinnerId: Long? = null
    private var selectedDecision: String? = null

    private val selectedColor = Color.parseColor("#4CAF50")
    private val defaultColor = Color.parseColor("#E31212")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartScoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matchData = intent.getSerializableExtra("match") as? MatchResponse

        matchId = matchData?.id ?: intent.getLongExtra("matchId", -1L)
        sportId = matchData?.sportId ?: intent.getLongExtra("sportId", -1L)

        if (matchId <= 0L) {
            toastShort("Invalid matchId: $matchId")
            finish()
            return
        }

        setupButtons()

        if (matchData != null) {
            bindDataToUI()
        } else {
            fetchMatchDetails()
        }

        handleSportUI()
    }

    private fun handleSportUI() {

        when (sportId) {

            1L -> { // Cricket
                showDecisionSection("Bat", "Bowl")
            }

            2L -> { // Futsal
                showDecisionSection("Kickoff", "Choose Side")
            }

            3L -> { // Volleyball
                showDecisionSection("Give Service", "Take Service")
            }

            4L -> { // Table Tennis
                showDecisionSection("Choose Service", "Choose Side")
            }

            5L -> { // Badminton
                showDecisionSection("Choose Service", "Choose Side")
            }

            6L -> { // Ludo
                hideDecisionSection()
            }

            7L -> { // Tug of War
                showDecisionSection("Left Side", "Right Side")
            }

            8L -> { // Chess
                showDecisionSection("White", "Black")
            }

            else -> hideDecisionSection()
        }
    }

    private fun showDecisionSection(option1: String, option2: String) {
        binding.decisionSection.visibility = View.VISIBLE
        binding.decisionOption1Btn.text = option1
        binding.decisionOption2Btn.text = option2
    }

    private fun hideDecisionSection() {
        binding.decisionSection.visibility = View.GONE
    }





    private fun fetchMatchDetails() {
        lifecycleScope.launch {
            try {
                val response = api.getMatchById1(matchId)
                if (response.isSuccessful) {
                    matchData = response.body()
                    bindDataToUI()
                } else {
                    val err = response.errorBody()?.string()
                    toastShort("Failed to fetch match (${response.code()}) id=$matchId")
                }
            } catch (e: Exception) {
                toastLong("Network error")
            }
        }
    }

    private fun bindDataToUI() {
        val match = matchData ?: return

        binding.teamAName.text = "${match.team1Name} vs ${match.team2Name}"
        binding.venueText.text = match.venue
        binding.dateText.text = match.date
        binding.timeText.text = match.time
        binding.oversText.text = match.overs.toString()
        binding.scorerText.text = match.scorerId.toString()

        binding.tossTeamABtn.text = match.team1Name
        binding.tossTeamBBtn.text = match.team2Name

        when (match.status) {
            "ABANDONED", "COMPLETED" -> {
                binding.abandonYesBtn.isEnabled = false
                binding.abandonYesBtn.alpha = 0.5f
            }
            "LIVE" -> {
                binding.abandonYesBtn.text = "Abandon (Live Match)"
            }
        }
    }


    private fun setupButtons() {

        binding.backButton.setOnClickListener { finish() }

        binding.tossTeamABtn.setOnClickListener {
            selectedTossWinnerId = matchData?.team1Id
            updateButtonColors(binding.tossTeamABtn, binding.tossTeamBBtn)
        }

        binding.tossTeamBBtn.setOnClickListener {
            selectedTossWinnerId = matchData?.team2Id
            updateButtonColors(binding.tossTeamBBtn, binding.tossTeamABtn)
        }

        binding.startScoringBtn.setOnClickListener {
            if (selectedTossWinnerId != null && selectedDecision != null)
            {
                startMatchCall()
            } else {
                toastShort("Select toss & decision first")
            }
        }

        binding.abandonYesBtn.setOnClickListener {
            showAbandonDialog()
        }

        binding.abandonNoBtn.setOnClickListener {
            toastShort("Action cancelled")
        }
        binding.decisionOption1Btn.setOnClickListener {
            selectedDecision = binding.decisionOption1Btn.text.toString()
            updateButtonColors(binding.decisionOption1Btn, binding.decisionOption2Btn)
        }

        binding.decisionOption2Btn.setOnClickListener {
            selectedDecision = binding.decisionOption2Btn.text.toString()
            updateButtonColors(binding.decisionOption2Btn, binding.decisionOption1Btn)
        }
    }


    private fun startMatchCall() {
        val payload = matchData?.copy(
            tossWinnerId = selectedTossWinnerId,
            decision = selectedDecision,
            status = "LIVE"
        ) ?: return
        lifecycleScope.launch {
            try {
                val response = api.startMatch(matchId, payload)
                if (response.isSuccessful) {
                    val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    val role = sharedPreferences.getString("role", "")
                    val username = sharedPreferences.getString("username", "") ?: ""
                    if (role.equals("ADMIN", true) || matchData?.scorerId.equals(username, true)) {
                        val intent =
                            Intent(this@StartScoringActivity, CricketScoringActivity::class.java)
                        intent.putExtra("match", payload)
                        startActivity(intent)
                    }
                    finish()
                    binding.abandonYesBtn.isEnabled = false
                    binding.abandonYesBtn.alpha = 0.5f
                } else {
                    toastShort("Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                toastLong("Network error")
            }
        }
    }

    private fun showAbandonDialog() {
        AlertDialog.Builder(this)
            .setTitle("Abandon Match")
            .setMessage("This action cannot be undone. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                abandonMatchCall()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun abandonMatchCall() {
        binding.abandonYesBtn.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = api.abandonMatch(matchId)
                if (response.isSuccessful) {
                    toastShort("Match Abandoned")
                    finish()
                } else {
                    toastShort("Failed: ${response.code()}")
                    binding.abandonYesBtn.isEnabled = true
                }
            } catch (e: Exception) {
                toastLong("Network error")
                binding.abandonYesBtn.isEnabled = true
            }
        }
    }


    private fun updateButtonColors(
        selected: MaterialButton,
        unselected: MaterialButton
    ) {
        selected.setBackgroundColor(selectedColor)
        unselected.setBackgroundColor(defaultColor)
    }
}
