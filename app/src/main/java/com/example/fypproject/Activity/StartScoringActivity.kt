package com.example.fypproject.Activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
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

    private var ttGames: Int = 4
    private var ttPointsPerGame: Int = 11

    private var towRounds: Int = 3

    private var bdSets: Int = 2
    private var bdPointsPerSet: Int = 21
    private var bdFinalSetPoints: Int = 30

    private var scorerUsername: String = ""

    private val colorSelected = Color.parseColor("#4CAF50")
    private val colorDefault  = Color.parseColor("#E31212")

    private val isCricket     get() = sportId == 1L
    private val isFutsal      get() = sportId == 2L
    private val isVolleyball  get() = sportId == 3L
    private val isTableTennis get() = sportId == 4L
    private val isBadminton   get() = sportId == 5L
    private val isLudo        get() = sportId == 6L
    private val isTugOfWar    get() = sportId == 7L

    private val isChess get() = sportId == 8L

    private val sportDecisions = mapOf(
        1L to Pair("Bat",             "Bowl"),
        2L to Pair("Kickoff",         "Choose Side"),
        3L to Pair("Give Service",    "Take Service"),
        4L to Pair("Choose Service",  "Choose Side"),
        5L to Pair("Choose Service",  "Choose Side"),
        6L to null,
        7L to null,
        8L to Pair("White",           "Black")
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

        binding.tableTennisConfigSection.visibility =
            if (isTableTennis) View.VISIBLE else View.GONE
        if (isTableTennis) {
            refreshTableTennisLabels()
            setupTableTennisSteppers()
        }
        binding.tugOfWarConfigSection.visibility =
            if (isTugOfWar) View.VISIBLE else View.GONE
        if (isTugOfWar) {
            towRounds = matchData?.sets ?: 3
            refreshTugOfWarLabels()
            setupTugOfWarSteppers()
        }

        binding.badmintonConfigSection.visibility =
            if (isBadminton) View.VISIBLE else View.GONE
        if (isBadminton) {
            bdSets          = matchData?.sets           ?: 2
            bdPointsPerSet  = matchData?.pointsPerSet   ?: 21
            bdFinalSetPoints = matchData?.finalSetPoints ?: 30
            refreshBadmintonLabels()
            setupBadmintonSteppers()
        }

        val decisions = sportDecisions[sportId]
        if (decisions != null) {
            binding.decisionSection.visibility   = View.VISIBLE
            binding.decisionOption1Btn.text      = decisions.first
            binding.decisionOption2Btn.text      = decisions.second
            updateDecisionLabel()
        } else {
            binding.decisionSection.visibility = View.GONE
        }

        binding.tossSection.visibility = View.VISIBLE

        binding.tossLabel.text = when {
            isCricket     -> "Who Won The Toss?"
            isFutsal      -> "Who Kicks Off?"
            isVolleyball  -> "Who Serves First?"
            isBadminton   -> "Who Serves First?"
            isTableTennis -> "Who Serves First?"
            isTugOfWar    -> "Who Starts First?"
            isLudo        -> "Who Starts First?"
            isChess       -> "Who Plays White?"     // ← YE ADD KARO
            else          -> "Who Won The Toss?"
        }

        binding.startScoringBtn.text = when {
            isCricket     -> "Start Match"
            isFutsal      -> "⚽ Start Futsal Match"
            isVolleyball  -> "🏐 Start Volleyball Match"
            isBadminton   -> "🏸 Start Badminton Match"
            isTableTennis -> "🏓 Start Table Tennis Match"
            isLudo        -> "🎲 Start Ludo Match"
            isTugOfWar    -> "🪢 Start Tug of War"
            isChess       -> "♟️ Start Chess Match"  // ← YE ADD KARO
            else          -> "Start Match"
        }
    }

    private fun setupVolleyballSteppers() {
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
        binding.tvSetsValue.text     = vbSets.toString()
        binding.tvPointsValue.text   = vbPointsPerSet.toString()
        binding.tvFinalPtsValue.text = vbFinalSetPoints.toString()
        binding.tvVbSummary.text     =
            "Best of ${vbSets * 2 - 1} sets · $vbPointsPerSet pts each · $vbFinalSetPoints pts tiebreak"
    }

    private fun setupTableTennisSteppers() {
        binding.btnTtGamesDecrement.setOnClickListener {
            if (ttGames > 1) { ttGames--; refreshTableTennisLabels() }
        }
        binding.btnTtGamesIncrement.setOnClickListener {
            ttGames++; refreshTableTennisLabels()
        }
        binding.btnTtPointsDecrement.setOnClickListener {
            if (ttPointsPerGame > 5) { ttPointsPerGame--; refreshTableTennisLabels() }
        }
        binding.btnTtPointsIncrement.setOnClickListener {
            ttPointsPerGame++; refreshTableTennisLabels()
        }
    }

    private fun setupTugOfWarSteppers() {
        binding.btnTowRoundsDecrement.setOnClickListener {
            if (towRounds > 1) { towRounds--; refreshTugOfWarLabels() }
        }
        binding.btnTowRoundsIncrement.setOnClickListener {
            towRounds++; refreshTugOfWarLabels()
        }
    }

    private fun refreshTugOfWarLabels() {
        binding.tvTowRoundsValue.text = towRounds.toString()
        binding.tvTowSummary.text =
            "Best of ${towRounds * 2 - 1} rounds"
    }

    private fun refreshTableTennisLabels() {
        binding.tvTtGamesValue.text  = ttGames.toString()
        binding.tvTtPointsValue.text = ttPointsPerGame.toString()
        binding.tvTtSummary.text     =
            "Best of ${ttGames * 2 - 1} · $ttPointsPerGame pts each · True deuce (no cap)"
    }

    private fun setupBadmintonSteppers() {
        binding.btnBdSetsDecrement.setOnClickListener {
            if (bdSets > 1) { bdSets--; refreshBadmintonLabels() }
        }
        binding.btnBdSetsIncrement.setOnClickListener {
            bdSets++; refreshBadmintonLabels()
        }
        binding.btnBdPointsDecrement.setOnClickListener {
            if (bdPointsPerSet > 5) { bdPointsPerSet--; refreshBadmintonLabels() }
        }
        binding.btnBdPointsIncrement.setOnClickListener {
            bdPointsPerSet++; refreshBadmintonLabels()
        }
        binding.btnBdFinalPtsDecrement.setOnClickListener {
            if (bdFinalSetPoints > 5) { bdFinalSetPoints--; refreshBadmintonLabels() }
        }
        binding.btnBdFinalPtsIncrement.setOnClickListener {
            bdFinalSetPoints++; refreshBadmintonLabels()
        }
    }

    private fun refreshBadmintonLabels() {
        binding.tvBdSetsValue.text     = bdSets.toString()
        binding.tvBdPointsValue.text   = bdPointsPerSet.toString()
        binding.tvBdFinalPtsValue.text = bdFinalSetPoints.toString()
        binding.tvBdSummary.text       =
            "Best of ${bdSets * 2 - 1} games · $bdPointsPerSet pts each · $bdFinalSetPoints pts cap"
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
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.getMatchById1(matchId)
                if (response.isSuccessful) {
                    matchData = response.body()
                    sportId   = matchData?.sportId ?: sportId
                    bindDataToUI()
                    setupSportUI()
                    checkEmptyState()
                } else {
                    toastShort("Failed to fetch match (${response.code()})")
                    checkEmptyState()
                }
            } catch (e: Exception) {
                toastLong("Network error: ${e.message}")
                checkEmptyState()
            } finally {
                showLoading(false)
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
                binding.startScoringBtn.isEnabled   = false
                binding.startScoringBtn.alpha        = 0.5f
                binding.abandonYesBtn.isEnabled     = false
                binding.abandonYesBtn.alpha          = 0.5f
                binding.tossTeamABtn.isEnabled       = false
                binding.tossTeamBBtn.isEnabled       = false
                binding.decisionOption1Btn.isEnabled = false
                binding.decisionOption2Btn.isEnabled = false
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
            val decisionNeeded   = sportDecisions[sportId] != null
            val tossSelected     = selectedTossWinnerId != null
            val decisionSelected = selectedDecision != null || !decisionNeeded

            when {
                !tossSelected     -> toastShort("Select Toss Winner First")
                !decisionSelected -> toastShort("Select Decision First")
                else              -> startMatchCall()
            }
        }

        binding.abandonYesBtn.setOnClickListener { showAbandonDialog() }
        binding.abandonNoBtn.setOnClickListener  { toastShort("Action cancelled") }
    }

    private fun startMatchCall() {
        showLoading(true)
        binding.startScoringBtn.isEnabled = false
        binding.startScoringBtn.alpha     = 0.7f

        val decisionToSend = when {
            isFutsal      -> "KICKOFF"
            isVolleyball  -> "SERVE"
            isBadminton   -> "SERVE"
            isTableTennis -> "SERVE"
            isLudo        -> "START"
            isTugOfWar    -> "PULL"
            isChess       -> "WHITE"
            else          -> selectedDecision
        }

        val payload = matchData?.copy(
            tossWinnerId   = selectedTossWinnerId,
            decision       = decisionToSend,
            scorerId       = scorerUsername.ifBlank { matchData?.scorerId },
            status         = "LIVE",
            sets           = when {
                isVolleyball  -> vbSets
                isTableTennis -> ttGames
                isBadminton   -> bdSets
                isTugOfWar    -> towRounds
                else          -> matchData?.sets
            },
            pointsPerSet   = when {
                isVolleyball  -> vbPointsPerSet
                isTableTennis -> ttPointsPerGame
                isBadminton   -> bdPointsPerSet
                else          -> matchData?.pointsPerSet
            },
            finalSetPoints = when {
                isVolleyball  -> vbFinalSetPoints
                isTableTennis -> 0
                isBadminton   -> bdFinalSetPoints
                else          -> matchData?.finalSetPoints
            },
        ) ?: run {
            toastShort("Match data missing")
            showLoading(false)
            binding.startScoringBtn.isEnabled = true
            binding.startScoringBtn.alpha     = 1f
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
                    showLoading(false)
                    binding.startScoringBtn.isEnabled = true
                    binding.startScoringBtn.alpha     = 1f
                }
            } catch (e: Exception) {
                toastLong("Network error: ${e.message}")
                showLoading(false)
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
        showLoading(true)
        binding.abandonYesBtn.isEnabled = false
        binding.abandonYesBtn.alpha     = 0.5f

        lifecycleScope.launch {
            try {
                val response = api.abandonMatch(matchId)
                if (response.isSuccessful) {
                    toastShort("Match Abandoned")
                    checkEmptyState()
                    finish()
                } else {
                    toastShort("Failed: ${response.code()}")
                    showLoading(false)
                    binding.abandonYesBtn.isEnabled = true
                    binding.abandonYesBtn.alpha     = 1f
                }
            } catch (e: Exception) {
                toastLong("Network error: ${e.message}")
                showLoading(false)
                binding.abandonYesBtn.isEnabled = true
                binding.abandonYesBtn.alpha     = 1f
            }
        }
    }

    private fun highlightButton(selected: MaterialButton, unselected: MaterialButton) {
        selected.setBackgroundColor(colorSelected)
        unselected.setBackgroundColor(colorDefault)
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        val isEmpty = matchData == null
        if (isEmpty) {
            toastShort("No match data available")
        }
    }
}