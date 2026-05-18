package com.example.fypproject.Activity

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.MatchNavigator
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityStartScoringBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class StartScoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartScoringBinding

    private var matchData: MatchResponse? = null
    private var matchId: Long = -1L
    private var sportId: Long = -1L
    private var futsalHalfMins: Int = 20

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

    private var bdFormat: String = "singles"
    private var ttFormat: String = "singles"
    private var ludoFormat: String = "1v1"

    private var squadTeam1 = listOf<TeamPlayerDto>()
    private var squadTeam2 = listOf<TeamPlayerDto>()

    private val sel1 = mutableSetOf<Long>()
    private val sel2 = mutableSetOf<Long>()

    private val team1PlayingIds get() = sel1.toMutableList()
    private val team2PlayingIds get() = sel2.toMutableList()

    private val colorGreen = Color.parseColor("#4CAF50")
    private val colorRed   = Color.parseColor("#E31212")

    private val isCricket     get() = sportId == 1L
    private val isFutsal      get() = sportId == 2L
    private val isVolleyball  get() = sportId == 3L
    private val isTableTennis get() = sportId == 4L
    private val isBadminton   get() = sportId == 5L
    private val isLudo        get() = sportId == 6L
    private val isTugOfWar    get() = sportId == 7L
    private val isChess       get() = sportId == 8L

    private val needsLineup get() = isCricket ||isFutsal || isVolleyball || isBadminton || isTableTennis || isLudo ||isChess

    private val sportDecisions = mapOf(
        1L to Pair("Bat", "Bowl"),
        8L to Pair("White", "Black"),
    )

    private val maxPerTeam: Int
        get() = when {
            isCricket->11
            isBadminton   -> if (bdFormat == "singles") 1 else 2
            isTableTennis -> if (ttFormat == "singles") 1 else 2
            isLudo -> if (ludoFormat == "1v1") 1 else 2
            isChess->1
            else          -> Int.MAX_VALUE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartScoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matchData = intent.getSerializableExtra("match") as? MatchResponse
        matchId   = matchData?.id      ?: intent.getLongExtra("matchId", -1L)
        sportId   = matchData?.sportId ?: intent.getLongExtra("sportId", -1L)

        if (matchId <= 0L) { toastShort("Invalid match"); finish(); return }

        setupButtons()
        if (matchData != null) bindDataToUI() else fetchMatchDetails()
        setupSportUI()
    }

    private fun setupSportUI() {
        binding.oversRow.visibility                 = if (isCricket)     View.VISIBLE else View.GONE
        binding.volleyballConfigSection.visibility  = if (isVolleyball)  View.VISIBLE else View.GONE
        binding.tableTennisConfigSection.visibility = if (isTableTennis) View.VISIBLE else View.GONE
        binding.tugOfWarConfigSection.visibility    = if (isTugOfWar)    View.VISIBLE else View.GONE
        binding.badmintonConfigSection.visibility   = if (isBadminton)   View.VISIBLE else View.GONE
        binding.chessConfigSection.visibility       = if (isChess)       View.VISIBLE else View.GONE
        binding.futsalConfigSection.visibility = if (isFutsal) View.VISIBLE else View.GONE
        if (isFutsal) setupFutsalSteppers()
        binding.ludoConfigSection.visibility = if (isLudo) View.VISIBLE else View.GONE
        if (isLudo) setupLudoFormat()

        binding.formatSection?.visibility = if (isBadminton || isTableTennis) View.VISIBLE else View.GONE

        if (isBadminton || isTableTennis) setupFormatToggle()
        if (isVolleyball)  { refreshVolleyballLabels();  setupVolleyballSteppers()  }
        if (isTableTennis) { refreshTableTennisLabels(); setupTableTennisSteppers() }
        if (isTugOfWar)    { towRounds = matchData?.sets ?: 3; refreshTugOfWarLabels(); setupTugOfWarSteppers() }
        if (isBadminton)   {
            bdSets           = matchData?.sets           ?: 2
            bdPointsPerSet   = matchData?.pointsPerSet   ?: 21
            bdFinalSetPoints = matchData?.finalSetPoints ?: 30
            refreshBadmintonLabels(); setupBadmintonSteppers()
        }

        if (needsLineup && matchData?.status?.uppercase() == "UPCOMING") {
            binding.squadCard.visibility = View.VISIBLE
            binding.tvSquadTeam1Label.text = matchData?.team1Name ?: "Team 1"
            binding.tvSquadTeam2Label.text = matchData?.team2Name ?: "Team 2"
            fetchSquads()
        } else {
            binding.squadCard.visibility = View.GONE
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
            isChess       -> "Who Plays White?"
            else          -> "Who Won The Toss?"
        }

        val decisions = sportDecisions[sportId]
        if (decisions != null) {
            binding.decisionSection.visibility = View.VISIBLE
            binding.decisionOption1Btn.text    = decisions.first
            binding.decisionOption2Btn.text    = decisions.second
            updateDecisionLabel()
        } else {
            binding.decisionSection.visibility = View.GONE
        }

        binding.startScoringBtn.text = when {
            isCricket     -> "Start Match"
            isFutsal      -> "⚽ Start Futsal Match"
            isVolleyball  -> "🏐 Start Volleyball Match"
            isBadminton   -> "🏸 Start Badminton Match"
            isTableTennis -> "🏓 Start Table Tennis Match"
            isLudo        -> "🎲 Start Ludo Match"
            isTugOfWar    -> "🪢 Start Tug of War"
            isChess       -> "♟️ Start Chess Match"
            else          -> "Start Match"
        }
    }

    private fun setupFutsalSteppers() {
        binding.btnFutsalHalfDecrement.setOnClickListener {
            if (futsalHalfMins > 5) { futsalHalfMins--; refreshFutsalLabels() }
        }
        binding.btnFutsalHalfIncrement.setOnClickListener {
            futsalHalfMins++; refreshFutsalLabels()
        }
    }

    private fun refreshFutsalLabels() {
        binding.tvFutsalHalfValue.text = futsalHalfMins.toString()
        binding.tvFutsalSummary.text = "$futsalHalfMins minutes per half"
    }

    private fun setupLudoFormat() {
        fun highlight() {
            binding.btnLudo1v1.setBackgroundColor(
                if (ludoFormat == "1v1") Color.parseColor("#EA580C") else Color.parseColor("#E0E0E0")
            )
            binding.btnLudo2v2.setBackgroundColor(
                if (ludoFormat == "2v2") Color.parseColor("#EA580C") else Color.parseColor("#E0E0E0")
            )
            binding.btnLudo1v1.setTextColor(if (ludoFormat == "1v1") Color.WHITE else Color.parseColor("#333333"))
            binding.btnLudo2v2.setTextColor(if (ludoFormat == "2v2") Color.WHITE else Color.parseColor("#333333"))
            binding.tvLudoFormatSummary.text = if (ludoFormat == "1v1")
                "1 player per team — first to 4 home runs wins"
            else
                "2 players per team — first to 8 home runs wins"
        }
        highlight()

        binding.btnLudo1v1.setOnClickListener {
            ludoFormat = "1v1"; sel1.clear(); sel2.clear()
            highlight()
            if (squadTeam1.isNotEmpty()) refreshInlineSquadColumns()
        }
        binding.btnLudo2v2.setOnClickListener {
            ludoFormat = "2v2"; sel1.clear(); sel2.clear()
            highlight()
            if (squadTeam1.isNotEmpty()) refreshInlineSquadColumns()
        }
    }

    private fun setupFormatToggle() {
        val btnSingles = binding.btnSingles ?: return
        val btnDoubles = binding.btnDoubles ?: return

        fun highlight() {
            val fmt = if (isBadminton) bdFormat else ttFormat
            btnSingles.setBackgroundColor(if (fmt == "singles") colorGreen else Color.parseColor("#E0E0E0"))
            btnDoubles.setBackgroundColor(if (fmt == "doubles") colorGreen else Color.parseColor("#E0E0E0"))
            btnSingles.setTextColor(if (fmt == "singles") Color.WHITE else Color.parseColor("#333333"))
            btnDoubles.setTextColor(if (fmt == "doubles") Color.WHITE else Color.parseColor("#333333"))
        }
        highlight()

        btnSingles.setOnClickListener {
            if (isBadminton) bdFormat = "singles" else ttFormat = "singles"
            sel1.clear(); sel2.clear()
            highlight()
            if (squadTeam1.isNotEmpty()) refreshInlineSquadColumns()
        }
        btnDoubles.setOnClickListener {
            if (isBadminton) bdFormat = "doubles" else ttFormat = "doubles"
            sel1.clear(); sel2.clear()
            highlight()
            if (squadTeam1.isNotEmpty()) refreshInlineSquadColumns()
        }
    }

    private fun fetchSquads() {
        val t1 = matchData?.team1Id ?: return
        val t2 = matchData?.team2Id ?: return
        lifecycleScope.launch {
            showLoading(true)
            try {
                val r1 = async(Dispatchers.IO) { api.getPlayersByTeam(t1) }
                val r2 = async(Dispatchers.IO) { api.getPlayersByTeam(t2) }
                squadTeam1 = r1.await().body() ?: emptyList()
                squadTeam2 = r2.await().body() ?: emptyList()
            } catch (e: Exception) {
                toastShort("Could not load players")
            } finally {
                showLoading(false)
            }
            refreshInlineSquadColumns()
        }
    }

    private fun refreshInlineSquadColumns() {
        buildPlayerColumn(squadTeam1, sel1, binding.llTeam1Players)
        buildPlayerColumn(squadTeam2, sel2, binding.llTeam2Players)
        updateSquadHint()
    }

    private fun buildPlayerColumn(
        squad: List<TeamPlayerDto>,
        sel: MutableSet<Long>,
        col: LinearLayout
    ) {
        col.removeAllViews()
        val dp = resources.displayMetrics.density

        if (squad.isEmpty()) {
            col.addView(TextView(this).apply {
                text = "No players"
                textSize = 12f
                setTextColor(Color.parseColor("#AAAAAA"))
                setPadding((4*dp).toInt(), (4*dp).toInt(), (4*dp).toInt(), (4*dp).toInt())
            })
            return
        }

        val cap = maxPerTeam
        squad.forEach { player ->
            val id = player.id ?: return@forEach
            val isSelected = id in sel
            val isCapped   = !isSelected && sel.size >= cap

            val btn = TextView(this).apply {
                text     = player.name ?: "Player"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding((10*dp).toInt(), (10*dp).toInt(), (10*dp).toInt(), (10*dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (6*dp).toInt() }
                background = GradientDrawable().apply {
                    setColor(when {
                        isSelected -> colorGreen
                        isCapped   -> Color.parseColor("#E0E0E0")
                        else       -> Color.parseColor("#F1F5F9")
                    })
                    cornerRadius = 8 * dp
                }
                setTextColor(when {
                    isSelected -> Color.WHITE
                    isCapped   -> Color.parseColor("#AAAAAA")
                    else       -> Color.parseColor("#1e293b")
                })
                alpha = if (isCapped) 0.5f else 1f
            }

            if (!isCapped) {
                btn.setOnClickListener {
                    if (id in sel) sel.remove(id) else sel.add(id)
                    buildPlayerColumn(squad, sel, col)
                    updateSquadHint()
                }
            }
            col.addView(btn)
        }
    }

    private fun updateSquadHint() {
        val cap = maxPerTeam
        binding.tvSquadHint?.text = when {
            cap == Int.MAX_VALUE -> "${sel1.size} selected · ${sel2.size} selected"
            sel1.size == cap && sel2.size == cap -> "✓ Squad confirmed — ${sel1.size} per team"
            else -> "Select ${if (cap == 1) "1 player" else "$cap players"} per team  (${sel1.size}/$cap · ${sel2.size}/$cap)"
        }
    }

    private fun setupVolleyballSteppers() {
        binding.btnSetsDecrement.setOnClickListener { if (vbSets > 1) { vbSets--; refreshVolleyballLabels() } }
        binding.btnSetsIncrement.setOnClickListener { vbSets++; refreshVolleyballLabels() }
        binding.btnPointsDecrement.setOnClickListener { if (vbPointsPerSet > 5) { vbPointsPerSet--; refreshVolleyballLabels() } }
        binding.btnPointsIncrement.setOnClickListener { vbPointsPerSet++; refreshVolleyballLabels() }
        binding.btnFinalPtsDecrement.setOnClickListener { if (vbFinalSetPoints > 5) { vbFinalSetPoints--; refreshVolleyballLabels() } }
        binding.btnFinalPtsIncrement.setOnClickListener { vbFinalSetPoints++; refreshVolleyballLabels() }
    }

    private fun refreshVolleyballLabels() {
        binding.tvSetsValue.text     = vbSets.toString()
        binding.tvPointsValue.text   = vbPointsPerSet.toString()
        binding.tvFinalPtsValue.text = vbFinalSetPoints.toString()
        binding.tvVbSummary.text     = "Best of ${vbSets * 2 - 1} sets · $vbPointsPerSet pts each · $vbFinalSetPoints pts tiebreak"
    }

    private fun setupTableTennisSteppers() {
        binding.btnTtGamesDecrement.setOnClickListener { if (ttGames > 1) { ttGames--; refreshTableTennisLabels() } }
        binding.btnTtGamesIncrement.setOnClickListener { ttGames++; refreshTableTennisLabels() }
        binding.btnTtPointsDecrement.setOnClickListener { if (ttPointsPerGame > 5) { ttPointsPerGame--; refreshTableTennisLabels() } }
        binding.btnTtPointsIncrement.setOnClickListener { ttPointsPerGame++; refreshTableTennisLabels() }
    }

    private fun refreshTableTennisLabels() {
        binding.tvTtGamesValue.text  = ttGames.toString()
        binding.tvTtPointsValue.text = ttPointsPerGame.toString()
        binding.tvTtSummary.text     = "Best of ${ttGames * 2 - 1} · $ttPointsPerGame pts each · True deuce (no cap)"
    }

    private fun setupTugOfWarSteppers() {
        binding.btnTowRoundsDecrement.setOnClickListener { if (towRounds > 1) { towRounds--; refreshTugOfWarLabels() } }
        binding.btnTowRoundsIncrement.setOnClickListener { towRounds++; refreshTugOfWarLabels() }
    }

    private fun refreshTugOfWarLabels() {
        binding.tvTowRoundsValue.text = towRounds.toString()
        binding.tvTowSummary.text     = "Best of ${towRounds * 2 - 1} rounds"
    }

    private fun setupBadmintonSteppers() {
        binding.btnBdSetsDecrement.setOnClickListener { if (bdSets > 1) { bdSets--; refreshBadmintonLabels() } }
        binding.btnBdSetsIncrement.setOnClickListener { bdSets++; refreshBadmintonLabels() }
        binding.btnBdPointsDecrement.setOnClickListener { if (bdPointsPerSet > 5) { bdPointsPerSet--; refreshBadmintonLabels() } }
        binding.btnBdPointsIncrement.setOnClickListener { bdPointsPerSet++; refreshBadmintonLabels() }
        binding.btnBdFinalPtsDecrement.setOnClickListener { if (bdFinalSetPoints > 5) { bdFinalSetPoints--; refreshBadmintonLabels() } }
        binding.btnBdFinalPtsIncrement.setOnClickListener { bdFinalSetPoints++; refreshBadmintonLabels() }
    }

    private fun refreshBadmintonLabels() {
        binding.tvBdSetsValue.text     = bdSets.toString()
        binding.tvBdPointsValue.text   = bdPointsPerSet.toString()
        binding.tvBdFinalPtsValue.text = bdFinalSetPoints.toString()
        binding.tvBdSummary.text       = "Best of ${bdSets * 2 - 1} games · $bdPointsPerSet pts each · $bdFinalSetPoints pts cap"
    }

    private fun updateDecisionLabel() {
        val name = when (selectedTossWinnerId) {
            matchData?.team1Id -> matchData?.team1Name ?: "Team"
            matchData?.team2Id -> matchData?.team2Name ?: "Team"
            else               -> "..."
        }
        binding.decisionLabel.text = "$name Decided To?"
    }

    private fun bindDataToUI() {
        val match = matchData ?: return
        binding.teamAName.text  = match.team1Name ?: "Team A"
        binding.teamBName.text  = match.team2Name ?: "Team B"
        binding.venueText.text  = match.venue     ?: "-"
        binding.dateText.text   = match.date?.split("T")?.get(0) ?: "-"
        binding.timeText.text   = match.time      ?: "-"
        binding.scorerText.text = match.scorerId  ?: "-"
        if (isCricket) binding.oversText.text = if (match.overs != null) "${match.overs} Overs" else "-"
        binding.tossTeamABtn.text = match.team1Name ?: "Team A"
        binding.tossTeamBBtn.text = match.team2Name ?: "Team B"
        binding.tvSquadTeam1Label.text = match.team1Name ?: "Team 1"
        binding.tvSquadTeam2Label.text = match.team2Name ?: "Team 2"
        when (match.status?.uppercase()) {
            "COMPLETED", "ABANDONED" -> {
                binding.startScoringBtn.isEnabled    = false; binding.startScoringBtn.alpha    = 0.5f
                binding.abandonYesBtn.isEnabled      = false; binding.abandonYesBtn.alpha      = 0.5f
                binding.tossTeamABtn.isEnabled       = false; binding.tossTeamBBtn.isEnabled   = false
                binding.decisionOption1Btn.isEnabled = false; binding.decisionOption2Btn.isEnabled = false
            }
            "LIVE" -> {
                binding.startScoringBtn.isEnabled = false; binding.startScoringBtn.alpha = 0.5f
                binding.abandonYesBtn.text = "Abandon (Live)"
            }
        }
    }

    private fun fetchMatchDetails() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.getMatchById1(matchId)
                if (response.isSuccessful) {
                    matchData = response.body()
                    sportId   = matchData?.sportId ?: sportId
                    bindDataToUI(); setupSportUI(); checkEmptyState()
                } else { toastShort("Failed to fetch match (${response.code()})"); checkEmptyState() }
            } catch (e: Exception) { toastLong("Network error: ${e.message}"); checkEmptyState()
            } finally { showLoading(false) }
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener { finish() }
        binding.tossTeamABtn.setOnClickListener {
            selectedTossWinnerId = matchData?.team1Id
            highlightBtn(binding.tossTeamABtn, binding.tossTeamBBtn); updateDecisionLabel()
        }
        binding.tossTeamBBtn.setOnClickListener {
            selectedTossWinnerId = matchData?.team2Id
            highlightBtn(binding.tossTeamBBtn, binding.tossTeamABtn); updateDecisionLabel()
        }
        binding.decisionOption1Btn.setOnClickListener {
            selectedDecision = binding.decisionOption1Btn.text.toString()
            highlightBtn(binding.decisionOption1Btn, binding.decisionOption2Btn)
        }
        binding.decisionOption2Btn.setOnClickListener {
            selectedDecision = binding.decisionOption2Btn.text.toString()
            highlightBtn(binding.decisionOption2Btn, binding.decisionOption1Btn)
        }
        binding.startScoringBtn.setOnClickListener {
            val decisionNeeded   = sportDecisions[sportId] != null
            val tossSelected     = selectedTossWinnerId != null
            val decisionSelected = selectedDecision != null || !decisionNeeded

            if (needsLineup && squadTeam1.isNotEmpty()) {
                val cap = maxPerTeam
                if (!isCricket && cap != Int.MAX_VALUE && (sel1.size != cap || sel2.size != cap)) {
                    val label = if (cap == 1) "1 player" else "$cap players"
                    toastShort("Select exactly $label per team"); return@setOnClickListener
                }
            }
            if (needsLineup && squadTeam1.isNotEmpty()) {
                val cap = maxPerTeam
                if (!isCricket && (isBadminton || isTableTennis || isLudo) && cap != Int.MAX_VALUE
                    && (sel1.size != cap || sel2.size != cap)) {
                    val label = if (cap == 1) "1 player" else "$cap players"
                    toastShort("Select exactly $label per team")
                    return@setOnClickListener
                }
            }

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
            isCricket     -> selectedDecision
            isFutsal      -> "KICKOFF"
            isVolleyball  -> "SERVE"
            isBadminton   -> "SERVE"
            isTableTennis -> "SERVE"
            isLudo        -> "START"
            isTugOfWar    -> "PULL"
            isChess       -> selectedDecision?.uppercase() ?: "WHITE"
            else          -> selectedDecision
        }

        val payload = matchData?.copy(
            tossWinnerId     = selectedTossWinnerId,
            decision         = decisionToSend,
            status           = "LIVE",
            sets             = when { isVolleyball -> vbSets; isTableTennis -> ttGames; isBadminton -> bdSets; isTugOfWar -> towRounds; else -> matchData?.sets },
            pointsPerSet     = when { isVolleyball -> vbPointsPerSet; isTableTennis -> ttPointsPerGame; isBadminton -> bdPointsPerSet; else -> matchData?.pointsPerSet },
            finalSetPoints   = when { isVolleyball -> vbFinalSetPoints; isTableTennis -> 0; isBadminton -> bdFinalSetPoints; else -> matchData?.finalSetPoints },
            team1PlayingIds  = if (needsLineup) team1PlayingIds.ifEmpty { null } else null,
            team2PlayingIds  = if (needsLineup) team2PlayingIds.ifEmpty { null } else null,
            halfDurationMins = if (isFutsal) futsalHalfMins else matchData?.halfDurationMins,
            scorerId         = binding.etScorerUsername.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() } ?: matchData?.scorerId,
            matchFormat      = when {
                isLudo  -> ludoFormat
                isChess -> "1v1"
                else    -> matchData?.matchFormat
            },
        ) ?: run {
            toastShort("Match data missing"); showLoading(false)
            binding.startScoringBtn.isEnabled = true; binding.startScoringBtn.alpha = 1f; return
        }

        lifecycleScope.launch {
            try {
                val response = api.startMatch(matchId, payload)
                if (response.isSuccessful) {
                    MatchNavigator.navigate(this@StartScoringActivity, payload); finish()
                } else {
                    toastShort("Failed: ${response.code()}"); showLoading(false)
                    binding.startScoringBtn.isEnabled = true; binding.startScoringBtn.alpha = 1f
                }
            } catch (e: Exception) {
                toastLong("Network error: ${e.message}"); showLoading(false)
                binding.startScoringBtn.isEnabled = true; binding.startScoringBtn.alpha = 1f
            }
        }
    }

    private fun showAbandonDialog() {
        AlertDialog.Builder(this)
            .setTitle("Are you sure?")
            .setMessage("Match will be abandoned. This cannot be undone.")
            .setPositiveButton("Yes, Abandon") { _, _ -> abandonMatchCall() }
            .setNegativeButton("No, Cancel", null).show()
    }

    private fun abandonMatchCall() {
        showLoading(true)
        binding.abandonYesBtn.isEnabled = false; binding.abandonYesBtn.alpha = 0.5f
        lifecycleScope.launch {
            try {
                val response = api.abandonMatch(matchId)
                if (response.isSuccessful) {
                    binding.startScoringBtn.isEnabled    = false
                    binding.abandonYesBtn.isEnabled      = false
                    binding.tossTeamABtn.isEnabled       = false; binding.tossTeamBBtn.isEnabled = false
                    binding.decisionOption1Btn.isEnabled = false; binding.decisionOption2Btn.isEnabled = false
                    binding.abandonYesBtn.text           = "🚫 Match Abandoned"
                    binding.abandonYesBtn.setBackgroundColor(Color.parseColor("#888888"))
                    binding.startScoringBtn.text         = "🚫 Match Abandoned"
                    toastLong("Match Abandoned Successfully!"); showLoading(false)
                } else {
                    toastShort("Failed: ${response.code()}"); showLoading(false)
                    binding.abandonYesBtn.isEnabled = true; binding.abandonYesBtn.alpha = 1f
                }
            } catch (e: Exception) {
                toastLong("Network error: ${e.message}"); showLoading(false)
                binding.abandonYesBtn.isEnabled = true; binding.abandonYesBtn.alpha = 1f
            }
        }
    }

    private fun highlightBtn(selected: MaterialButton, unselected: MaterialButton) {
        selected.setBackgroundColor(colorGreen)
        unselected.setBackgroundColor(colorRed)
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        if (matchData == null) toastShort("No match data available")
    }
}