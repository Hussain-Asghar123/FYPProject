package com.example.fypproject.Activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
    private var tournamentId: Long = -1L
    private var matchData: MatchResponse? = null
    private var selectedTossWinnerId: Long? = null
    private var selectedDecision: String? = null

    private val selectedColor = Color.parseColor("#4CAF50")
    private val defaultColor = Color.parseColor("#E31212")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartScoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matchId = intent.getLongExtra("matchId", -1L)
        tournamentId = intent.getLongExtra("tournamentId", -1L)

        if (matchId == -1L) {
            toastShort("Invalid match")
            finish()
            return
        }

        setupButtons()
        fetchMatchDetails()
    }


    private fun fetchMatchDetails() {
        lifecycleScope.launch {
            try {
                val response = api.getMatchById1(matchId)
                if (response.isSuccessful) {
                    matchData = response.body()
                    bindDataToUI()
                } else {
                    toastShort("Failed to fetch match")
                }
            } catch (e: Exception) {
                toastLong("Network error")
            }
        }
    }


    private fun bindDataToUI() {
        val match = matchData ?: return

        binding.teamAName.text = match.team1Name
        binding.teamBName.text = match.team2Name
        binding.venueText.text = match.venue
        binding.dateText.text = match.date
        binding.timeText.text = match.time
        binding.oversText.text = match.overs.toString()
        binding.scorerText.text = match.scorerId.toString()

        binding.tossTeamABtn.text = match.team1Name
        binding.tossTeamBBtn.text = match.team2Name

        when (match.status) {
            MatchStatus.ABANDONED, MatchStatus.COMPLETED -> {
                binding.abandonYesBtn.isEnabled = false
                binding.abandonYesBtn.alpha = 0.5f
            }
            MatchStatus.LIVE -> {
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

        binding.decisionBatBtn.setOnClickListener {
            selectedDecision = "BAT"
            updateButtonColors(binding.decisionBatBtn, binding.decisionBowlBtn)
        }

        binding.decisionBowlBtn.setOnClickListener {
            selectedDecision = "BOWL"
            updateButtonColors(binding.decisionBowlBtn, binding.decisionBatBtn)
        }

        binding.startScoringBtn.setOnClickListener {
            if (selectedTossWinnerId == null || selectedDecision == null) {
                toastShort("Select toss & decision first")
                return@setOnClickListener
            }
            startMatchCall()
        }

        binding.abandonYesBtn.setOnClickListener {
            showAbandonDialog()
        }

        binding.abandonNoBtn.setOnClickListener {
            toastShort("Action cancelled")
        }
    }


    private fun startMatchCall() {
        val payload = matchData?.copy(
            tossWinnerId = selectedTossWinnerId,
            decision = selectedDecision,
            status = MatchStatus.LIVE
        ) ?: return

        lifecycleScope.launch {
            try {
                val response = api.startMatch(matchId, payload)
                if (response.isSuccessful) {
                    val intent = Intent(this@StartScoringActivity, CricketScoringActivity::class.java)
                    intent.putExtra("match", payload)
                    startActivity(intent)
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
