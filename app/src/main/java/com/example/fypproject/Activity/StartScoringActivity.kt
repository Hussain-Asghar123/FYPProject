package com.example.fypproject.Activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.MatchNavigator
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityStartScoringBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class StartScoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartScoringBinding

    private var matchData: MatchResponse? = null
    private var matchId: Long = -1L
    private var sportId: Long = -1L

    private var selectedTossWinnerId: Long? = null
    private var selectedDecision: String? = null

    private var vbSets: Int = 3
    private var vbPointsPerSet: Int = 25
    private var vbFinalSetPoints: Int = 15
    private var scorerUsername: String = ""

    private val colorSelected = Color.parseColor("#4CAF50")
    private val colorDefault  = Color.parseColor("#E31212")

    private val isCricket    get() = sportId == 1L
    private val isFutsal     get() = sportId == 2L
    private val isVolleyball get() = sportId == 3L
    private val isBadminton   get() = sportId == 4L

    private val sportDecisions = mapOf(
        1L to Pair("Bat",            "Bowl"),
        2L to Pair("Kickoff",        "Choose Side"),
        3L to Pair("Give Service",   "Take Service"),
        4L to Pair("Choose Service", "Choose Side"),
        5L to Pair("Choose Service", "Choose Side"),
        6L to null,
        7L to Pair("Left Side",      "Right Side"),
        8L to Pair("White",          "Black")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartScoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matchData = intent.getSerializableExtra("match") as? MatchResponse
        matchId   = matchData?.id      ?: intent.getLongExtra("matchId", -1L)
        sportId   = matchData?.sportId ?: intent.getLongExtra("sportId", -1L)

        if (matchId <= 0L) {
            toastShort("Invalid match")
            finish()
            return
        }

        setupButtons()

        if (matchData != null) {
            bindDataToUI()
        } else {
            fetchMatchDetails()
        }

        setupSportUI()
    }

    private fun setupSportUI() {
        binding.oversRow.visibility = if (isCricket) View.VISIBLE else View.GONE
        binding.volleyballConfigSection.visibility =
            if (isVolleyball) View.VISIBLE else View.GONE

        if (isVolleyball) {
            refreshVolleyballLabels()
            setupVolleyballSteppers()
        }

        val decisions = if (isBadminton) null else sportDecisions[sportId]
        if (decisions != null) {
            binding.decisionSection.visibility   = View.VISIBLE
            binding.decisionOption1Btn.text      = decisions.first
            binding.decisionOption2Btn.text      = decisions.second
            updateDecisionLabel()
        } else {
            binding.decisionSection.visibility = View.GONE
        }

        binding.tossLabel.text = when {
            isCricket    -> "Who Won The Toss?"
            isFutsal     -> "Who Kicks Off?"
            isVolleyball -> "Who Serves First?"
            isBadminton  -> "Who Serves First?"
            else         -> "Who Won The Toss?"
        }

        binding.startScoringBtn.text = when {
            isCricket    -> "Start Match"
            isFutsal     -> "⚽ Start Futsal Match"
            isVolleyball -> "🏐 Start Volleyball Match"
            isBadminton  -> "🏸 Start Badminton Match"
            else         -> "Start Match"
        }
    }

    private fun setupVolleyballSteppers() {
        // Sets to Win stepper
        binding.btnSetsDecrement.setOnClickListener {
            if (vbSets > 1) { vbSets--; refreshVolleyballLabels() }
        }
        binding.btnSetsIncrement.setOnClickListener {
            vbSets++; refreshVolleyballLabels()
        }

        binding.btnPointsDecrement.setOnClickListener {
            if (vbPointsPerSet > 5) { vbPointsPerSet--; refreshVolleyballLabels() }
        }
        binding.btnPointsIncrement.setOnClickListener {
            vbPointsPerSet++; refreshVolleyballLabels()
        }
        binding.btnFinalPtsDecrement.setOnClickListener {
            if (vbFinalSetPoints > 5) { vbFinalSetPoints--; refreshVolleyballLabels() }
        }
        binding.btnFinalPtsIncrement.setOnClickListener {
            vbFinalSetPoints++; refreshVolleyballLabels()
        }
    }
    private fun refreshVolleyballLabels() {
        binding.tvSetsValue.text       = vbSets.toString()
        binding.tvPointsValue.text     = vbPointsPerSet.toString()
        binding.tvFinalPtsValue.text   = vbFinalSetPoints.toString()
        binding.tvVbSummary.text       =
            "Best of ${vbSets * 2 - 1} sets · $vbPointsPerSet pts each · $vbFinalSetPoints pts tiebreak"
    }

    private fun updateDecisionLabel() {
        val selectedTeamName = when (selectedTossWinnerId) {
            matchData?.team1Id -> matchData?.team1Name ?: "Team"
            matchData?.team2Id -> matchData?.team2Name ?: "Team"
            else               -> "..."
        }
        binding.decisionLabel.text = "$selectedTeamName Decided To?"
    }

    private fun fetchMatchDetails() {
        lifecycleScope.launch {
            try {
                val response = api.getMatchById1(matchId)
                if (response.isSuccessful) {
                    matchData = response.body()
                    sportId   = matchData?.sportId ?: sportId
                    bindDataToUI()
                    setupSportUI()
                } else {
                    toastShort("Failed to fetch match (${response.code()})")
                }
            } catch (e: Exception) {
                toastLong("Network error: ${e.message}")
            }
        }
    }

    private fun bindDataToUI() {
        val match = matchData ?: return

        binding.teamAName.text  = match.team1Name ?: "Team A"
        binding.teamBName.text  = match.team2Name ?: "Team B"

        binding.venueText.text  = match.venue ?: "-"
        binding.dateText.text   = match.date?.split("T")?.get(0) ?: "-"
        binding.timeText.text   = match.time ?: "-"
        binding.scorerText.text = match.scorerId ?: "-"

        if (isCricket) {
            binding.oversText.text = if (match.overs != null) "${match.overs} Overs" else "-"
        }

        binding.tossTeamABtn.text = match.team1Name ?: "Team A"
        binding.tossTeamBBtn.text = match.team2Name ?: "Team B"

        when (match.status?.uppercase()) {
            "COMPLETED", "ABANDONED" -> {
                binding.startScoringBtn.isEnabled    = false
                binding.startScoringBtn.alpha         = 0.5f
                binding.abandonYesBtn.isEnabled      = false
                binding.abandonYesBtn.alpha           = 0.5f
                binding.tossTeamABtn.isEnabled        = false
                binding.tossTeamBBtn.isEnabled        = false
                binding.decisionOption1Btn.isEnabled  = false
                binding.decisionOption2Btn.isEnabled  = false
            }
            "LIVE" -> {
                binding.startScoringBtn.isEnabled = false
                binding.startScoringBtn.alpha      = 0.5f
                binding.abandonYesBtn.text         = "Abandon (Live)"
            }
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener { finish() }

        binding.tossTeamABtn.setOnClickListener {
            selectedTossWinnerId = matchData?.team1Id
            highlightButton(binding.tossTeamABtn, binding.tossTeamBBtn)
            updateDecisionLabel()
        }
        binding.tossTeamBBtn.setOnClickListener {
            selectedTossWinnerId = matchData?.team2Id
            highlightButton(binding.tossTeamBBtn, binding.tossTeamABtn)
            updateDecisionLabel()
        }

        binding.decisionOption1Btn.setOnClickListener {
            selectedDecision = binding.decisionOption1Btn.text.toString()
            highlightButton(binding.decisionOption1Btn, binding.decisionOption2Btn)
        }
        binding.decisionOption2Btn.setOnClickListener {
            selectedDecision = binding.decisionOption2Btn.text.toString()
            highlightButton(binding.decisionOption2Btn, binding.decisionOption1Btn)
        }

        binding.startScoringBtn.setOnClickListener {
            val decisionNeeded = !isBadminton && sportDecisions[sportId] != null
            val tossSelected     = selectedTossWinnerId != null
            val decisionSelected = selectedDecision != null || !decisionNeeded

            when {
                !tossSelected     -> toastShort("Pehle toss winner select karo")
                !decisionSelected -> toastShort("Decision bhi select karo")
                else              -> startMatchCall()
            }
        }

        binding.abandonYesBtn.setOnClickListener { showAbandonDialog() }
        binding.abandonNoBtn.setOnClickListener  { toastShort("Action cancelled") }
    }

    private fun startMatchCall() {
        binding.startScoringBtn.isEnabled = false
        binding.startScoringBtn.alpha     = 0.7f

        val decisionToSend = when {
            isFutsal     -> "KICKOFF"
            isVolleyball -> "SERVE"
            isBadminton-> "SERVE"
            else         -> selectedDecision
        }

        val payload = matchData?.copy(
            tossWinnerId   = selectedTossWinnerId,
            decision       = decisionToSend,
            scorerId       = scorerUsername.ifBlank { matchData?.scorerId },
            status         = "LIVE",
            sets           = if (isVolleyball) vbSets          else matchData?.sets,
            pointsPerSet   = if (isVolleyball) vbPointsPerSet  else matchData?.pointsPerSet,
            finalSetPoints = if (isVolleyball) vbFinalSetPoints else matchData?.finalSetPoints,
        ) ?: run {
            toastShort("Match data missing")
            return
        }

        lifecycleScope.launch {
            try {
                val response = api.startMatch(matchId, payload)
                if (response.isSuccessful) {
                    MatchNavigator.navigate(this@StartScoringActivity, payload)
                    finish()
                } else {
                    toastShort("Failed: ${response.code()}")
                    binding.startScoringBtn.isEnabled = true
                    binding.startScoringBtn.alpha     = 1f
                }
            } catch (e: Exception) {
                toastLong("Network error: ${e.message}")
                binding.startScoringBtn.isEnabled = true
                binding.startScoringBtn.alpha     = 1f
            }
        }
    }
    private fun showAbandonDialog() {
        AlertDialog.Builder(this)
            .setTitle("Are you sure?")
            .setMessage("Match will be abandoned. This cannot be undone.")
            .setPositiveButton("Yes, Abandon") { _, _ -> abandonMatchCall() }
            .setNegativeButton("No, Cancel", null)
            .show()
    }

    private fun abandonMatchCall() {
        binding.abandonYesBtn.isEnabled = false
        binding.abandonYesBtn.alpha     = 0.5f

        lifecycleScope.launch {
            try {
                val response = api.abandonMatch(matchId)
                if (response.isSuccessful) {
                    toastShort("Match Abandoned")
                    finish()
                } else {
                    toastShort("Failed: ${response.code()}")
                    binding.abandonYesBtn.isEnabled = true
                    binding.abandonYesBtn.alpha     = 1f
                }
            } catch (e: Exception) {
                toastLong("Network error: ${e.message}")
                binding.abandonYesBtn.isEnabled = true
                binding.abandonYesBtn.alpha     = 1f
            }
        }
    }
    private fun highlightButton(selected: MaterialButton, unselected: MaterialButton) {
        selected.setBackgroundColor(colorSelected)
        unselected.setBackgroundColor(colorDefault)
    }
}