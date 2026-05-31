package com.example.fypproject.CricketFragment

import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.Adapter.PlayerSelectionAdapter
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.BallViewHelper
import com.example.fypproject.ScoringDTO.CricketBall
import com.example.fypproject.ScoringDTO.ScoreDTO
import com.example.fypproject.Sockets.JsonConverter
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ScoringFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import com.example.fypproject.Dialog.MilestoneDialog
import com.example.fypproject.Utils.MilestoneDetector
import com.example.fypproject.Dialog.SubstitutePlayerDialog

class ScoringFragment : Fragment(R.layout.scoring_fragment) {

    private var _binding: ScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private var prevDataSnapshot: Map<String, Any?>? = null
    private var innings: Int = 1
    private var inningsId: Long? = null
    private var pendingComment: String? = null
    private var isInningsInitialized = false
    private var isBallPending = false
    private val SOCKET_KEY = "ScoringFragment"

    private var selectedVotePlayerId: Long?    = null
    private var selectedVotePlayerName: String = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null
    private var availableBatters: List<TeamPlayerDto> = emptyList()
    private var availableBowlers: List<TeamPlayerDto> = emptyList()

    private var lastReceivedScore: ScoreDTO? = null

    private var isSuperOverPending = false
    private var isSuperOver: Boolean = false
    private var isSuperOverInnings: Int = 1

    private var team1Id: Long = -1L
    private var team2Id: Long = -1L
    private var team1Name: String = ""
    private var team2Name: String = ""
    private var battingTeamId: Long = -1L
    private var bowlingTeamId: Long = -1L
    private var battingTeamName: String = ""
    private var bowlingTeamName: String = ""

    private var currentStrikerId: Long? = null
    private var currentNonStrikerId: Long? = null
    private var currentBowlerId: Long? = null
    private var selectedPenaltyRuns = 5

    // ── Feature: Double Wicket ─────────────────────────────────
    private var isDoubleWicket: Boolean = false

    // ── Feature: Commentator Mode ──────────────────────────────
    private var isCommentator: Boolean = false
    private var commentatorUsername: String = ""

    // ── Feature: NoBall + RunOut ───────────────────────────────
    private var isNoBallRunOut: Boolean = false
    private var noBallRunOutRuns: Int = 0

    private var isEndingMatch: Boolean = false
    private var b1Selected = false
    private var b2Selected = false
    private var bowlerSelected = false
    private val displayedBalls = mutableListOf<CricketBall>()
    private var currentOvers = 0
    private var currentBalls = 0
    private var row1PlayerId: Long? = null
    private var row2PlayerId: Long? = null
    private var isFirstInnings: Boolean = true

    private var canEdit: Boolean = false
    private var wicketFielderId: Long? = null
    private var wicketOutPlayerId: Long? = null
    private var wicketRunOutRuns: Int = 0
    private var pendingBallId: Long? = null
    private var cameraImageUri: Uri? = null
    private var isUploading = false

    private var canAddMedia: Boolean = false

    private var isVotingActive: Boolean = false
    private var summaryPollingJob: kotlinx.coroutines.Job? = null
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) cameraImageUri?.let { uploadMediaFile(it) }
        }
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uploadMediaFile(it) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ScoringFragmentBinding.bind(view)

        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }

        matchResponse?.let { match ->
            calculateTeams(match)
            updateHeaderUI(ScoreDTO())
            if (match.status == "COMPLETED" || match.status == "MATCH_COMPLETE") {
                canEdit = false
                loadAndShowVotingThenSummary()
                return
            }
            match.id?.let { clearMatchEnded(it) }
            registerSocketListeners()
        }

        canEdit = computeCanEdit(matchResponse)
        isDoubleWicket = matchResponse?.doubleWicket ?: false

        commentatorUsername = matchResponse?.commentatorUsername?.trim().orEmpty()
        val myUsername = requireActivity()
            .getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("username", "")?.trim().orEmpty()

        isCommentator = commentatorUsername.isNotBlank()
                && myUsername.equals(commentatorUsername, ignoreCase = true)
                && !canEdit


        if (canEdit) {
            val restored = restoreSelectionState()
            when {
                !restored -> showOnly(binding.layoutSelectPlayer.root)
                b1Selected && b2Selected && bowlerSelected ->
                    showOnly(binding.layoutMainScoring.root)
                b1Selected && b2Selected && !bowlerSelected -> {
                    showOnly(binding.layoutSelectBowler.root)
                    binding.layoutSelectBowler.btnSelectBowler.text = "Select Next Bowler"
                }
                else -> showOnly(binding.layoutSelectPlayer.root)
            }
            setupAdminSelectionFlow()
            setupScoringPanel()
            setupExtrasPanels()
            setupWicketPanel()
            setupMorePanel()
        } else {
            showOnly(binding.layoutUserHistory)
        }

        matchResponse?.let {
            registerSocketListeners()
        }

        attachLiveChat()
    }

    // ─── State Persistence ────────────────────────────────────────

    private fun saveSelectionState() {
        val matchId = matchResponse?.id ?: return
        requireActivity().getSharedPreferences("ScoringPrefs", MODE_PRIVATE).edit().apply {
            putBoolean("b1_$matchId",          b1Selected)
            putBoolean("b2_$matchId",          b2Selected)
            putBoolean("bowler_$matchId",      bowlerSelected)
            putBoolean("firstInnings_$matchId", isFirstInnings)
            putLong("striker_$matchId",        currentStrikerId    ?: -1L)
            putLong("nonStriker_$matchId",     currentNonStrikerId ?: -1L)
            putLong("bowlerId_$matchId",       currentBowlerId     ?: -1L)
            putLong("inningsId_$matchId",       inningsId           ?: -1L)
            putString("strikerName_$matchId",    binding.tvBatsman1Name.text.toString())
            putString("nonStrikerName_$matchId", binding.tvBatsman2Name.text.toString())
            putString("bowlerName_$matchId",     binding.tvBowlerName.text.toString())
            apply()
        }
    }

    private fun restoreSelectionState(): Boolean {
        val matchId = matchResponse?.id ?: return false
        val prefs = requireActivity().getSharedPreferences("ScoringPrefs", MODE_PRIVATE)

        // Pehli baar join — kuch save nahi hua
        if (!prefs.contains("b1_$matchId")) return false

        b1Selected     = prefs.getBoolean("b1_$matchId",     false)
        b2Selected     = prefs.getBoolean("b2_$matchId",     false)
        bowlerSelected = prefs.getBoolean("bowler_$matchId", false)

        currentStrikerId    = prefs.getLong("striker_$matchId",    -1L).takeIf { it > 0 }
        currentNonStrikerId = prefs.getLong("nonStriker_$matchId", -1L).takeIf { it > 0 }
        currentBowlerId     = prefs.getLong("bowlerId_$matchId",   -1L).takeIf { it > 0 }
        inningsId           = prefs.getLong("inningsId_$matchId",  -1L).takeIf { it > 0 }

        // Innings 2 tha to teams swap karo
        val savedFirstInnings = prefs.getBoolean("firstInnings_$matchId", true)
        if (!savedFirstInnings && isFirstInnings) {
            val tempId      = battingTeamId
            battingTeamId   = bowlingTeamId
            bowlingTeamId   = tempId
            battingTeamName = if (battingTeamId == team1Id) team1Name else team2Name
            bowlingTeamName = if (bowlingTeamId == team1Id) team1Name else team2Name
            isFirstInnings  = false
        }

        // UI names restore karo
        if (b1Selected) {
            val name = prefs.getString("strikerName_$matchId", "Batsman 1") ?: "Batsman 1"
            binding.tvBatsman1Name.text = name
            binding.layoutSelectPlayer.btnSelectBatsman.text = "Batsmen: Selected"
        }
        if (b2Selected) {
            val name = prefs.getString("nonStrikerName_$matchId", "Batsman 2") ?: "Batsman 2"
            binding.tvBatsman2Name.text = name
        }
        if (bowlerSelected) {
            val name = prefs.getString("bowlerName_$matchId", "Bowler") ?: "Bowler"
            binding.tvBowlerName.text = name
            binding.layoutSelectPlayer.btnSelectBowler.text  = "Bowler: $name"
            binding.layoutSelectBowler.btnSelectBowler.text  = "Bowler: $name"
        }

        binding.tvTeamName.text = battingTeamName
        return b1Selected // striker selected = match shuru ho chuka tha
    }

    private fun clearSelectionState() {
        val matchId = matchResponse?.id ?: return
        requireActivity().getSharedPreferences("ScoringPrefs", MODE_PRIVATE).edit().apply {
            remove("b1_$matchId");          remove("b2_$matchId")
            remove("bowler_$matchId");      remove("firstInnings_$matchId")
            remove("striker_$matchId");     remove("nonStriker_$matchId")
            remove("bowlerId_$matchId")
            remove("strikerName_$matchId"); remove("nonStrikerName_$matchId")
            remove("bowlerName_$matchId")
            apply()
        }
    }
    private fun clearMatchEnded(matchId: Long) {
        requireActivity()
            .getSharedPreferences("MatchPrefs", MODE_PRIVATE)
            .edit()
            .remove("match_ended_$matchId")
            .apply()
    }
    override fun onResume() {
        super.onResume()
        if (_binding != null && canEdit && b1Selected && b2Selected && bowlerSelected) {
            showOnly(binding.layoutMainScoring.root)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) registerSocketListeners()
        else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        summaryPollingJob?.cancel()
        unregisterSocketListeners()
        _binding = null
    }

    private fun computeCanEdit(match: MatchResponse?): Boolean {
        val prefs       = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val role        = prefs.getString("role", "")?.trim().orEmpty()
        val username    = prefs.getString("username", "")?.trim().orEmpty()
        val scorer      = match?.scorerId?.trim().orEmpty()
        val mediaScorer = match?.mediaScorerUsername?.trim().orEmpty()

        // canAddMedia alag compute karo — original logic mat badlo
        canAddMedia = role.equals("ADMIN", true)
                || scorer.equals(username, true)
                || mediaScorer.equals(username, true)

        // canEdit mein sirf mediaScorer wali line hatao
        return role.equals("ADMIN", true)
                || scorer.equals(username, true)
    }

    private fun saveMediaBallId(ballId: Long) {
        val matchId = matchResponse?.id ?: return
        requireActivity()
            .getSharedPreferences("MediaPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("media_ball_${matchId}_${ballId}", true)
            .apply()
    }

    private fun hasSavedMedia(ballId: Long): Boolean {
        val matchId = matchResponse?.id ?: return false
        return requireActivity()
            .getSharedPreferences("MediaPrefs", MODE_PRIVATE)
            .getBoolean("media_ball_${matchId}_${ballId}", false)
    }

    private fun calculateTeams(match: MatchResponse) {
        team1Id = match.team1Id ?: -1L
        team2Id = match.team2Id ?: -1L
        team1Name = match.team1Name ?: ""
        team2Name = match.team2Name ?: ""
        isInningsInitialized = true

        val tossWinnerId = match.tossWinnerId ?: -1L

        if (match.decision == "BAT") {
            battingTeamId = tossWinnerId
            bowlingTeamId = if (battingTeamId == team1Id) team2Id else team1Id
        } else {
            battingTeamId = if (tossWinnerId == team1Id) team2Id else team1Id
            bowlingTeamId = tossWinnerId
        }

        battingTeamName = if (battingTeamId == team1Id) team1Name else team2Name
        bowlingTeamName = if (bowlingTeamId == team1Id) team1Name else team2Name
    }

    private fun registerSocketListeners() {
        unregisterSocketListeners()
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected    -> requireContext().toastShort("Live Connected!")
                    is SocketState.Error        -> requireContext().toastShort("Socket Error: ${state.message}")
                    is SocketState.Disconnected -> {}
                }
            }
        }
        WebSocketManager.addMessageListener(SOCKET_KEY) { jsonString ->
            try {
                val jo = org.json.JSONObject(jsonString)
                val jsonMap = jo.keys().asSequence().associateWith { key -> jo.opt(key) }
                val milestone = MilestoneDetector.detectCricketMilestone(
                    displayedBalls, jsonMap, prevDataSnapshot, isDoubleWicket
                )
                prevDataSnapshot = jsonMap
                milestone?.let {
                    activity?.runOnUiThread {
                        MilestoneDialog.show(childFragmentManager, it)
                    }
                }
            } catch (_: Exception) {}
            val updatedScore = JsonConverter.fromJson(jsonString)
            println("📥 Received JSON: $jsonString")
            updatedScore?.let {
                activity?.runOnUiThread { updateScoreboardUI(it) }
            }
        }
    }

    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }

    private fun attachLiveChat() {
        if (childFragmentManager.findFragmentById(R.id.liveChatContainer) != null) return
        val matchId = matchResponse?.id ?: return

        val prefs = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val username = prefs.getString("username", null)
        val role = prefs.getString("role", "")?.trim().orEmpty()
        val isAdmin = role.equals("ADMIN", ignoreCase = true)

        val displayUsername = username ?: "Guest"
        val isGuest = username.isNullOrBlank()

        childFragmentManager.beginTransaction()
            .replace(R.id.liveChatContainer, LiveChatFragment.newInstance(
                matchId = matchId,
                username = displayUsername,
                isCommentator = isCommentator
            ))
            .commit()
    }

    private fun updateScoreboardUI(score: ScoreDTO) {

        if (_binding == null || !isAdded) return
        isBallPending = false
        setScoringPanelEnabled(true)
        lastReceivedScore = score
        score.availableBatters?.let { availableBatters = it }
        score.availableBowlers?.let { availableBowlers = it }


        if (isEndingMatch) {

            if (score.status == "COMPLETED" ||
                score.status == "MATCH_COMPLETE" ||
                score.matchEnd) {
                isEndingMatch = false
                loadAndShowVotingThenSummary()
                return
            }

            return
        }
        if (binding.layoutMatchSummary.root.visibility == View.VISIBLE ||
            binding.layoutVoting.root.visibility == View.VISIBLE) {
            return
        }

        if (binding.layoutMatchSummary.root.visibility == View.VISIBLE) {
            return
        }
        if (score.inningsId != null && score.inningsId != -1L) {
            this.inningsId = score.inningsId
        }

        val normalizedScore = normalizeStats(score)

        normalizedScore.batsmanId?.takeIf    { it > 0 }?.let { currentStrikerId    = it }
        normalizedScore.nonStrikerId?.takeIf { it > 0 }?.let { currentNonStrikerId = it }
        normalizedScore.bowlerId?.takeIf     { it > 0 }?.let { currentBowlerId     = it }

        normalizedScore.batsman1Stats?.playerId?.takeIf { it > 0 }?.let { row1PlayerId = it }
        normalizedScore.batsman2Stats?.playerId?.takeIf { it > 0 }?.let { row2PlayerId = it }

        this.currentOvers = normalizedScore.overs
        this.currentBalls = normalizedScore.balls

        autoRecoverSelectionState(normalizedScore)

        // User side innings swap — canEdit se independent
        if (!score.superOver && !score.firstInnings && isFirstInnings) {
            switchInnings()
        }
        if (score.status == "COMPLETED" || score.status == "MATCH_COMPLETE" || score.matchEnd) {
            loadAndShowVotingThenSummary()
            return
        }
        handleModalLogic(normalizedScore)


        binding.tvTeamName.text = battingTeamName
        binding.tvMainScore.text = "${normalizedScore.runs}-${normalizedScore.wickets}"
        binding.tvOtherStats.text =
            "Extras: ${normalizedScore.extra} | " +
                    "Overs: ${normalizedScore.overs}.${normalizedScore.balls} | " +
                    "CRR: ${String.format("%.2f", normalizedScore.crr)}"

        if (normalizedScore.firstInnings) {
            binding.tvInningsLabel.text = "Innings 1"
            binding.tvTargetStatus.visibility = View.GONE
        } else {
            binding.tvInningsLabel.text = "Innings 2"
            binding.tvTargetStatus.visibility = View.VISIBLE
            val target = normalizedScore.target.takeIf { it > 0 } ?: "—"
            val rrr = String.format("%.2f", normalizedScore.rrr)
            binding.tvTargetStatus.text = "Target: $target | RRR: $rrr"
        }

        normalizedScore.batsman1Stats?.let { stats ->
            if (stats.playerId != null && stats.playerId > 0) {
                val isStriker = stats.playerId == normalizedScore.batsmanId
                binding.tvBatsman1Name.text = "${if (isStriker) "🏏 " else ""}${stats.playerName}"
                binding.tvBatsman1R.text  = stats.runs.toString()
                binding.tvBatsman1B.text  = stats.ballsFaced.toString()
                binding.tvBatsman14s.text = stats.fours.toString()
                binding.tvBatsman16s.text = stats.sixes.toString()
                val sr = if (stats.ballsFaced > 0)
                    stats.runs.toDouble() / stats.ballsFaced * 100 else 0.0
                binding.tvBatsman1SR.text = String.format("%.1f", sr)
            }
        }

        normalizedScore.batsman2Stats?.let { stats ->
            if (stats.playerId != null && stats.playerId > 0) {
                val isStriker = stats.playerId == normalizedScore.batsmanId
                binding.tvBatsman2Name.text = "${if (isStriker) "🏏 " else ""}${stats.playerName}"
                binding.tvBatsman2R.text  = stats.runs.toString()
                binding.tvBatsman2B.text  = stats.ballsFaced.toString()
                binding.tvBatsman24s.text = stats.fours.toString()
                binding.tvBatsman26s.text = stats.sixes.toString()
                val sr = if (stats.ballsFaced > 0)
                    stats.runs.toDouble() / stats.ballsFaced * 100 else 0.0
                binding.tvBatsman2SR.text = String.format("%.1f", sr)
            }
        }

        normalizedScore.bowlerStats?.let { stats ->
            if (stats.playerId != null && stats.playerId > 0) {
                binding.tvBowlerName.text = stats.playerName
                binding.tvBowlerO.text   = "${stats.ballsBowled / 6}.${stats.ballsBowled % 6}"
                binding.tvBowlerR.text   = stats.runsConceded.toString()
                binding.tvBowlerW.text   = stats.wickets.toString()
                val eco = if (stats.ballsBowled > 0)
                    stats.runsConceded / (stats.ballsBowled / 6.0) else 0.0
                binding.tvBowlerEco.text = String.format("%.1f", eco)
            }
        }

        updateBallContainer(normalizedScore)

        if (canEdit) checkInningsComplete(normalizedScore)
        if (canEdit) saveSelectionState()
    }

    private fun handleModalLogic(score: ScoreDTO) {
        if (!canEdit) return
//        matchResponse?.id?.let {
//            if (hasMatchEnded(it)) { loadAndShowVotingThenSummary(); return }
//        }
        if (score.comment == "Super_Over") {
            showOnly(binding.layoutInningsUndo.root)
            binding.layoutInningsUndo.btnSuperOver.visibility = View.VISIBLE
            binding.layoutInningsUndo.btnEndInnings.text = "End Match"
            return
        }

        if (score.comment == "DLS_UPDATED") {
            showOnly(binding.layoutMainScoring.root)
            return
        }

        if (score.comment == "End_Innings" || score.eventType == "End_Innings") {
            showOnly(binding.layoutInningsUndo.root)
            when {
                isSuperOver && isSuperOverInnings == 1 -> {
                    binding.layoutInningsUndo.btnSuperOver.visibility = View.GONE
                    binding.layoutInningsUndo.btnEndInnings.text = "End Super Over Innings"
                }
                isSuperOver && isSuperOverInnings == 2 -> {

                    binding.layoutInningsUndo.btnSuperOver.visibility = View.GONE
                    binding.layoutInningsUndo.btnEndInnings.text = "End Match"
                }
                !isFirstInnings -> {

                    binding.layoutInningsUndo.btnSuperOver.visibility = View.VISIBLE
                    binding.layoutInningsUndo.btnEndInnings.text = "End Match"
                }
                else -> {

                    binding.layoutInningsUndo.btnSuperOver.visibility = View.GONE
                    binding.layoutInningsUndo.btnEndInnings.text = "End Innings"
                }
            }
            return
        }

        if (score.status == "COMPLETED" || score.status == "MATCH_COMPLETE" || score.matchEnd) {
            // ✅ FIX 3: only call if not already in voting/summary
            if (!isVotingActive &&
                binding.layoutMatchSummary.root.visibility != View.VISIBLE &&
                binding.layoutVoting.root.visibility != View.VISIBLE) {
                loadAndShowVotingThenSummary()
            }
            return
        }

        if (!score.firstInnings && isFirstInnings && !isSuperOver) { switchInnings(); return }

        if (score.balls == 0 && score.overs == 0 && score.wickets == 0 && score.runs == 0) {
            if (!b1Selected || !b2Selected || !bowlerSelected)
                showOnly(binding.layoutSelectPlayer.root)
            return
        }

        if (b1Selected && b2Selected && bowlerSelected) {
            if (binding.layoutSelectPlayer.root.visibility == View.VISIBLE ||
                binding.layoutSelectBowler.root.visibility == View.VISIBLE)
                showOnly(binding.layoutMainScoring.root)
        }

        // ✅ REPLACE KARO IS SE
        if (score.balls == 0 && score.overs > 0) {
            if (bowlerSelected) {

                // ✅ Super Over hai to bowler modal mat dikhao
                if (score.superOver) {
                    showOnly(binding.layoutInningsUndo.root)
                    binding.layoutInningsUndo.btnSuperOver.visibility = View.GONE
                    binding.layoutInningsUndo.btnEndInnings.text =
                        if (isSuperOverInnings == 1) "End Super Over Innings"
                        else "End Match"
                    return
                }

                bowlerSelected = false
                currentBowlerId = null
                binding.tvBowlerName.text = "Select Bowler"
                resetBowlerUI()
                showOnly(binding.layoutSelectBowler.root)
                binding.layoutSelectBowler.btnSelectBowler.text = "Select Next Bowler"
                requireContext().toastShort("Over complete! Select next bowler.")
            }
        }
    }
    private fun checkInningsComplete(score: ScoreDTO) {
        if (!isSuperOver && score.firstInnings != isFirstInnings) return

        val totalOvers = matchResponse?.overs ?: return
        val oversComplete = score.overs >= totalOvers && score.balls == 0 && score.overs > 0 && currentBalls == 0
        val wicketsComplete = score.wickets >= 10

        if ((oversComplete || wicketsComplete) &&
            score.eventType != "End_Innings" &&
            score.comment != "End_Innings" &&
            score.comment != "Super_Over"
        ) {
            if (binding.layoutInningsUndo.root.visibility != View.VISIBLE) {
                showOnly(binding.layoutInningsUndo.root)
                when {
                    isSuperOver && isSuperOverInnings == 1 -> {
                        binding.layoutInningsUndo.btnSuperOver.visibility = View.GONE
                        binding.layoutInningsUndo.btnEndInnings.text = "End Super Over Innings"
                    }
                    isSuperOver && isSuperOverInnings == 2 -> {
                        binding.layoutInningsUndo.btnSuperOver.visibility = View.GONE
                        binding.layoutInningsUndo.btnEndInnings.text = "End Match"
                    }
                    !isFirstInnings -> {
                        binding.layoutInningsUndo.btnSuperOver.visibility = View.VISIBLE
                        binding.layoutInningsUndo.btnEndInnings.text = "End Match"
                    }
                }
            }
        }
    }

    private fun normalizeStats(score: ScoreDTO): ScoreDTO {
        val stats1 = score.batsman1Stats
        val stats2 = score.batsman2Stats

        if (score.eventType == "wicket" || score.eventType == "noball_runout"||
            stats1?.playerId == null || stats1.playerId <= 0 ||
            stats2?.playerId == null || stats2.playerId <= 0
        ) {
            if (stats1?.playerId != null && stats1.playerId > 0) row1PlayerId = stats1.playerId
            if (stats2?.playerId != null && stats2.playerId > 0) row2PlayerId = stats2.playerId
            return score
        }

        if (row1PlayerId == null && stats1.playerId != null && stats1.playerId > 0) {
            row1PlayerId = stats1.playerId
            row2PlayerId = stats2.playerId
            return score
        }

        if (stats1.playerId != null && stats2.playerId != null &&
            stats1.playerId == row2PlayerId && stats2.playerId == row1PlayerId
        ) {
            return score.copy(
                event = score.event ?: "",
                eventType = score.eventType ?: "",
                batsman1Stats = stats2,
                batsman2Stats = stats1
            )
        }

        if (stats1.playerId != null &&
            stats1.playerId != row1PlayerId && stats1.playerId != row2PlayerId
        ) {
            row1PlayerId = stats1.playerId
            return score
        }

        if (stats2.playerId != null &&
            stats2.playerId != row1PlayerId && stats2.playerId != row2PlayerId
        ) {
            row2PlayerId = stats2.playerId
            return score
        }

        return score
    }

    private fun switchInnings() {
        if (!isFirstInnings) return
        if (isSuperOver) return

        val tempTeamId = battingTeamId
        battingTeamId  = bowlingTeamId
        bowlingTeamId  = tempTeamId
        isFirstInnings = false
        battingTeamName = if (battingTeamId == team1Id) team1Name else team2Name

        resetForNewInnings()

        displayedBalls.clear()
        binding.ballContainer.removeAllViews()

        binding.tvTeamName.text     = battingTeamName
        binding.tvMainScore.text    = "0-0"
        binding.tvInningsLabel.text = "Innings 2"
        binding.tvTargetStatus.visibility = View.VISIBLE

        if (canEdit) showOnly(binding.layoutSelectPlayer.root)

        requireContext().toastShort("Innings Complete! Select new players.")
    }

    private fun setScoringPanelEnabled(enabled: Boolean) {
        binding.layoutMainScoring.apply {
            btnDot.isEnabled    = enabled
            btnRun1.isEnabled   = enabled
            btnRun2.isEnabled   = enabled
            btnRun3.isEnabled   = enabled
            btnRun4.isEnabled   = enabled
            btnRun6.isEnabled   = enabled
            btnWide.isEnabled   = enabled
            btnNoBall.isEnabled = enabled
            btnBye.isEnabled    = enabled
            btnLegBye.isEnabled = enabled
            btnOut.isEnabled    = enabled
            btnMore.isEnabled   = enabled
            btnUndo.isEnabled   = enabled

            val alpha = if (enabled) 1.0f else 0.45f
            btnDot.alpha    = alpha
            btnRun1.alpha   = alpha
            btnRun2.alpha   = alpha
            btnRun3.alpha   = alpha
            btnRun4.alpha   = alpha
            btnRun6.alpha   = alpha
            btnWide.alpha   = alpha
            btnNoBall.alpha = alpha
            btnBye.alpha    = alpha
            btnLegBye.alpha = alpha
            btnOut.alpha    = alpha
            btnMore.alpha   = alpha
            btnUndo.alpha   = alpha
        }
    }

    private fun resetForNewInnings() {
        clearSelectionState()
        currentStrikerId    = null
        currentNonStrikerId = null
        currentBowlerId     = null
        row1PlayerId = null
        row2PlayerId = null
        b1Selected    = false
        b2Selected    = false
        bowlerSelected = false


        currentOvers = 0
        currentBalls = 0

        binding.layoutSelectPlayer.btnSelectBatsman.text = "Select Batsman"
        binding.layoutSelectPlayer.btnSelectBowler.text  = "Select Bowler"
        resetBatsman1UI()
        resetBatsman2UI()
        resetBowlerUI()
        binding.tvBatsman1Name.text = "Batsman 1"
        binding.tvBatsman2Name.text = "Batsman 2"
        binding.tvBowlerName.text   = "Bowler"
    }

    private fun autoRecoverSelectionState(score: ScoreDTO) {
        if (!canEdit) return
        if (score.firstInnings != isFirstInnings) return

        val hasPlayerData = score.batsman1Stats?.playerId != null &&
                score.batsman1Stats!!.playerId!! > 0

        if (hasPlayerData && (!b1Selected || !b2Selected || !bowlerSelected)) {
            score.batsman1Stats?.let { stats ->
                if (stats.playerId != null && stats.playerId > 0) {
                    currentStrikerId = score.batsmanId
                    row1PlayerId = stats.playerId
                    binding.tvBatsman1Name.text = "🏏 ${stats.playerName}"
                    binding.layoutSelectPlayer.btnSelectBatsman.text = "B1: ${stats.playerName}"
                    b1Selected = true
                }
            }
            score.batsman2Stats?.let { stats ->
                if (stats.playerId != null && stats.playerId > 0) {
                    currentNonStrikerId = score.nonStrikerId
                    row2PlayerId = stats.playerId
                    binding.tvBatsman2Name.text = stats.playerName ?: "Batsman 2"
                    binding.layoutSelectPlayer.btnSelectBatsman.text = "Batsmen: Selected"
                    b2Selected = true
                }
            }
            score.bowlerStats?.let { stats ->
                if (stats.playerId != null && stats.playerId > 0) {
                    currentBowlerId = score.bowlerId
                    binding.tvBowlerName.text = stats.playerName ?: "Bowler"
                    binding.layoutSelectPlayer.btnSelectBowler.text = "Bowler: ${stats.playerName}"
                    bowlerSelected = true
                }
            }
            if (b1Selected && b2Selected && bowlerSelected) {
                showOnly(binding.layoutMainScoring.root)
                isInningsInitialized = true
            }
        }
    }

    private fun updateBallContainer(score: ScoreDTO) {
        val allBalls = score.cricketBalls ?: return
        val sortedBalls = allBalls.sortedBy { it.id ?: 0L }

        val existingMediaMap = displayedBalls
            .filter { it.hasMedia }
            .associate { it.id to true }

        val mergedBalls = sortedBalls.map { newBall ->
            val ballId = newBall.id
            val hasMedia = existingMediaMap.containsKey(ballId)
                    || (ballId != null && hasSavedMedia(ballId))
            if (hasMedia) newBall.copy(mediaCount = maxOf(newBall.mediaCount, 1))
            else newBall
        }

        if (mergedBalls == displayedBalls) return

        displayedBalls.clear()
        displayedBalls.addAll(mergedBalls)
        rebuildBallContainer()
    }

    private fun rebuildBallContainer() {
        if (_binding == null || !isAdded) return
        val container  = binding.ballContainer
        val scrollView = binding.ballScrollView

        container.removeAllViews()
        for (ball in displayedBalls) {
            val ballView = BallViewHelper.createBallView(requireContext(), ball)
            ballView.setOnClickListener {
                if (canAddMedia) {
                    ball.id?.let { ballId -> showMediaDialog(ballId) }
                }
            }
            container.addView(ballView)
        }
        scrollView.post { scrollView.fullScroll(View.FOCUS_RIGHT) }
    }

    private fun showMediaDialog(ballId: Long) {
        val matchId   = matchResponse?.id ?: return
        val accountId = requireActivity()
            .getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getLong("id", -1L)

        BallMediaBottomSheet
            .newInstance(ballId, matchId, accountId)
            .show(childFragmentManager, BallMediaBottomSheet.TAG)
    }

    private fun openCamera() {
        val imageFile = File(
            requireContext().cacheDir,
            "camera_${System.currentTimeMillis()}.jpg"
        )
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun uploadMediaFile(uri: Uri) {
        val matchId = matchResponse?.id ?: return
        val ballId  = pendingBallId    ?: return

        isUploading = true
        showUploadProgress(true)
        requireContext().toastShort("Uploading...")

        val commentToSend = pendingComment

        lifecycleScope.launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val tempFile = File(
                    requireContext().cacheDir,
                    "upload_${System.currentTimeMillis()}.jpg"
                )
                tempFile.outputStream().use { out -> inputStream?.copyTo(out) }

                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart    = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val matchIdBody = matchId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val ballIdBody  = ballId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val commentBody = commentToSend?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.createMedia(matchIdBody, ballIdBody, filePart, commentBody)
                }

                if (response.isSuccessful) {
                    requireContext().toastShort("Upload Successful!")
                    saveMediaBallId(ballId)

                    val updatedIndex = displayedBalls.indexOfFirst { it.id == ballId }
                    if (updatedIndex != -1) {
                        displayedBalls[updatedIndex] = displayedBalls[updatedIndex].copy(mediaCount = 1)
                        activity?.runOnUiThread { rebuildBallContainer() }
                    }
                } else {
                    requireContext().toastShort("Upload failed: ${response.code()}")
                }

            } catch (e: Exception) {
                requireContext().toastShort("Upload failed: ${e.message}")
            } finally {
                showUploadProgress(false)
                isUploading    = false
                pendingBallId  = null
                pendingComment = null
            }
        }
    }

    private fun showUploadProgress(show: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateHeaderUI(score: ScoreDTO) {
        binding.tvTeamName.text = battingTeamName
        this.currentOvers = score.overs
        this.currentBalls = score.balls
        if (innings == 1) {
            binding.tvInningsLabel.text = "Innings 1"
            binding.tvTargetStatus.visibility = View.GONE
        } else {
            binding.tvInningsLabel.text = "Innings 2"
            binding.tvTargetStatus.visibility = View.VISIBLE
        }
    }

    private fun setupScoringPanel() {
        binding.layoutMainScoring.apply {
            btnDot.setOnClickListener  { sendBallEvent(0, "0") }
            btnRun1.setOnClickListener { sendBallEvent(1, "1") }
            btnRun2.setOnClickListener { sendBallEvent(2, "2") }
            btnRun3.setOnClickListener { sendBallEvent(3, "3") }
            btnRun4.setOnClickListener { sendBallEvent(4, "4") }
            btnRun6.setOnClickListener { sendBallEvent(6, "6") }

            btnUndo.setOnClickListener {
                if (isBallPending) return@setOnClickListener
                isBallPending = true
                setScoringPanelEnabled(false)
                JsonConverter.sendScore(ScoreDTO().apply {
                    this.matchId   = matchResponse?.id
                    this.inningsId = currentInningsId()
                    this.undo      = true
                })
            }

            btnWide.setOnClickListener   { showOnly(binding.layoutWidePanel.root)   }
            btnNoBall.setOnClickListener { showOnly(binding.layoutNoBallPanel.root) }
            btnBye.setOnClickListener    { showOnly(binding.layoutByePanel.root)    }
            btnLegBye.setOnClickListener { showOnly(binding.layoutLegByePanel.root) }
            btnOut.setOnClickListener    { showOnly(binding.layoutWicketPanel.root) }
            btnMore.setOnClickListener   { showOnly(binding.layoutMorePanel.root)   }
        }
        binding.layoutWidePanel.btnCloseWide.setOnClickListener     { showOnly(binding.layoutMainScoring.root) }
        binding.layoutNoBallPanel.btnCloseNoBall.setOnClickListener { showOnly(binding.layoutMainScoring.root) }
        binding.layoutByePanel.btnCloseBye.setOnClickListener       { showOnly(binding.layoutMainScoring.root) }
        binding.layoutLegByePanel.btnCloseLegBye.setOnClickListener { showOnly(binding.layoutMainScoring.root) }
        binding.layoutWicketPanel.btnCloseWicket.setOnClickListener { showOnly(binding.layoutMainScoring.root) }
        binding.layoutMorePanel.btnCloseMore.setOnClickListener     { showOnly(binding.layoutMainScoring.root) }
    }

    private fun sendBallEvent(runs: Int, event: String) {
        if (currentStrikerId == null || currentNonStrikerId == null || currentBowlerId == null) {
            requireContext().toastShort("Players not selected!")
            return
        }
        if (isBallPending) return
        isBallPending = true
        setScoringPanelEnabled(false)
        JsonConverter.sendScore(ScoreDTO().apply {
            this.matchId      = matchResponse?.id
            this.teamId       = battingTeamId
            this.firstInnings = isFirstInnings
            this.inningsId    = currentInningsId()
            this.batsmanId    = currentStrikerId
            this.nonStrikerId = currentNonStrikerId
            this.bowlerId     = currentBowlerId
            this.overs        = currentOvers
            this.balls        = currentBalls
            this.runsOnThisBall = runs
            this.event        = event
            this.eventType    = if (runs == 4 || runs == 6) "boundary" else "run"
            this.isLegal      = true
            this.comment      = ""
            this.status       = "LIVE"
        })
    }

    private fun setupExtrasPanels() {
        binding.layoutWidePanel.apply {
            btnWd1.setOnClickListener { sendExtraEvent(0, "wide",  false) }
            btnWd2.setOnClickListener { sendExtraEvent(1, "wide",  false) }
            btnWd3.setOnClickListener { sendExtraEvent(2, "wide",  false) }
            btnWd4.setOnClickListener { sendExtraEvent(3, "wide",  false) }
            btnWd5.setOnClickListener { sendExtraEvent(4, "wide",  false) }
            btnWd6.setOnClickListener { sendExtraEvent(5, "wide",  false) }
            btnWd7.setOnClickListener { sendExtraEvent(6, "wide",  false) }
        }
        binding.layoutNoBallPanel.apply {
            btnNb1.setOnClickListener { sendExtraEvent(0, "noball", false) }
            btnNb2.setOnClickListener { sendExtraEvent(1, "noball", false) }
            btnNb3.setOnClickListener { sendExtraEvent(2, "noball", false) }
            btnNb4.setOnClickListener { sendExtraEvent(3, "noball", false) }
            btnNb5.setOnClickListener { sendExtraEvent(4, "noball", false) }
            btnNb6.setOnClickListener { sendExtraEvent(5, "noball", false) }
            btnNb7.setOnClickListener { sendExtraEvent(6, "noball", false) }
            btnNbRunOut.setOnClickListener {
                showNoBallRunOutRunsDialog()
            }
//            btnSendCustomNb.setOnClickListener {
//                val customRuns = etCustomNbRuns.text.toString().toIntOrNull()
//                if (customRuns == null) {
//                    requireContext().toastShort("Pehle runs enter karo")
//                    return@setOnClickListener
//                }
//                sendCustomNoBallEvent(customRuns)
//                etCustomNbRuns.setText("")
//                showOnly(binding.layoutMainScoring.root)
//            }
        }
        binding.layoutByePanel.apply {
            btnBye1.setOnClickListener { sendExtraEvent(1, "bye", true) }
            btnBye2.setOnClickListener { sendExtraEvent(2, "bye", true) }
            btnBye3.setOnClickListener { sendExtraEvent(3, "bye", true) }
            btnBye4.setOnClickListener { sendExtraEvent(4, "bye", true) }
            btnBye5.setOnClickListener { sendExtraEvent(5, "bye", true) }
            btnBye6.setOnClickListener { sendExtraEvent(6, "bye", true) }
            btnBye7.setOnClickListener { sendExtraEvent(7, "bye", true) }
        }
        binding.layoutLegByePanel.apply {
            btnLb1.setOnClickListener { sendExtraEvent(1, "legbye", true) }
            btnLb2.setOnClickListener { sendExtraEvent(2, "legbye", true) }
            btnLb3.setOnClickListener { sendExtraEvent(3, "legbye", true) }
            btnLb4.setOnClickListener { sendExtraEvent(4, "legbye", true) }
            btnLb5.setOnClickListener { sendExtraEvent(5, "legbye", true) }
            btnLb6.setOnClickListener { sendExtraEvent(6, "legbye", true) }
            btnLb7.setOnClickListener { sendExtraEvent(7, "legbye", true) }
        }
    }
    private fun sendCustomNoBallEvent(customRuns: Int) {
        if (isBallPending) return
        isBallPending = true
        setScoringPanelEnabled(false)

        val base = lastReceivedScore ?: ScoreDTO()
        JsonConverter.sendScore(base.cleanForSend().copy(
            matchId        = matchResponse?.id,
            teamId         = battingTeamId,
            inningsId      = currentInningsId(),
            batsmanId      = currentStrikerId,
            nonStrikerId   = currentNonStrikerId,
            bowlerId       = currentBowlerId,
            overs          = currentOvers,
            balls          = currentBalls,
            runsOnThisBall = customRuns,
            event          = customRuns.toString(),
            eventType      = "noball",
            status         = "LIVE",
            isLegal        = false,
            firstInnings   = isFirstInnings,
            undo           = false,
            comment        = ""
        ))
    }

    private fun currentInningsId(): Long? {
        val id = if (isFirstInnings) {
            this.inningsId?.takeIf { it > 0 } ?: matchResponse?.inningsId?.takeIf { it > 0 }
        } else {
            this.inningsId?.takeIf { it > 0 }
        }
        android.util.Log.d("INNINGS_ID", "currentInningsId = $id | isFirstInnings=$isFirstInnings | this.inningsId=${this.inningsId}")
        return id
    }

    private fun sendExtraEvent(extraRuns: Int, type: String, isLegal: Boolean) {
        if (currentStrikerId == null || currentBowlerId == null) {
            requireContext().toastShort("Select players first!")
            return
        }
        if (isBallPending) return
        isBallPending = true
        setScoringPanelEnabled(false)
        JsonConverter.sendScore(ScoreDTO().apply {
            this.matchId      = matchResponse?.id
            this.teamId       = battingTeamId
            this.firstInnings = isFirstInnings
            this.inningsId    = currentInningsId()
            this.batsmanId    = currentStrikerId
            this.nonStrikerId = currentNonStrikerId
            this.bowlerId     = currentBowlerId
            this.overs        = currentOvers
            this.balls        = currentBalls
            this.status       = "LIVE"
            this.isLegal      = isLegal
            this.eventType    = type
            this.event        = extraRuns.toString()
        })
        showOnly(binding.layoutMainScoring.root)
    }

    private fun setupMorePanel() {
        binding.layoutMorePanel.apply {
            btnDLS.setOnClickListener        { showOnly(binding.layoutDLS.root)        }
            btnAbandon.setOnClickListener    { showAbandonConfirmationDialog()         }
            btnPenalty.setOnClickListener    { resetPenaltyPanel(); showOnly(binding.layoutPenalty.root) }
            btnSubstitute.setOnClickListener {
                showOnly(binding.layoutMainScoring.root)
                val dialog = SubstitutePlayerDialog.newInstance(
                    matchResponse?.id ?: return@setOnClickListener,
                    inningsId,
                    team1Id, team2Id, team1Name, team2Name,
                    battingTeamId,
                    availableBatters, availableBowlers,
                    listOfNotNull(currentStrikerId, currentNonStrikerId),
                    listOfNotNull(currentBowlerId)
                )
                dialog.onSuccess = object : SubstitutePlayerDialog.OnSubstituteSuccess {
                    override fun onSuccess(updatedScore: com.example.fypproject.ScoringDTO.ScoreDTO?) {
                        showOnly(binding.layoutMainScoring.root)
                        if (updatedScore != null) {
                            applySubstituteResult(updatedScore)
                        } else {
                            requireContext().toastShort("Squad updated!")
                        }
                    }
                }
                dialog.show(childFragmentManager, "Substitute")
            }
        }
        binding.layoutDLS.btnCloseDLS.setOnClickListener               { showOnly(binding.layoutMainScoring.root) }
        binding.layoutAbandon.btnCloseAbandon.setOnClickListener       { showOnly(binding.layoutMainScoring.root) }
        binding.layoutPenalty.btnClosePenalty.setOnClickListener       { showOnly(binding.layoutMainScoring.root) }

        setupEndInningsAction()
        setupDLSAction()
        setupPenaltyAction()
    }

    private fun applySubstituteResult(score: ScoreDTO) {
        if (_binding == null || !isAdded) return

        // ── IDs update karo (React: setStrikerId, setNonStrikerId, setBowlerId) ──
        score.batsmanId?.takeIf    { it > 0 }?.let {
            currentStrikerId = it
            row1PlayerId     = it
        }
        score.nonStrikerId?.takeIf { it > 0 }?.let {
            currentNonStrikerId = it
            row2PlayerId        = it
        }
        score.bowlerId?.takeIf     { it > 0 }?.let {
            currentBowlerId = it
        }

        // ── Available lists update karo ──
        score.availableBatters?.let { availableBatters = it }
        score.availableBowlers?.let { availableBowlers = it }

        // ── Batsman 1 stats force-update (naam + 0 se reset) ──
        score.batsman1Stats?.let { stats ->
            if (stats.playerId != null && stats.playerId > 0) {
                val isStriker = stats.playerId == score.batsmanId
                binding.tvBatsman1Name.text = "${if (isStriker) "🏏 " else ""}${stats.playerName}"
                binding.tvBatsman1R.text    = stats.runs.toString()
                binding.tvBatsman1B.text    = stats.ballsFaced.toString()
                binding.tvBatsman14s.text   = stats.fours.toString()
                binding.tvBatsman16s.text   = stats.sixes.toString()
                val sr = if (stats.ballsFaced > 0)
                    stats.runs.toDouble() / stats.ballsFaced * 100 else 0.0
                binding.tvBatsman1SR.text   = String.format("%.1f", sr)
            }
        }

        // ── Batsman 2 stats force-update ──
        score.batsman2Stats?.let { stats ->
            if (stats.playerId != null && stats.playerId > 0) {
                val isStriker = stats.playerId == score.batsmanId
                binding.tvBatsman2Name.text = "${if (isStriker) "🏏 " else ""}${stats.playerName}"
                binding.tvBatsman2R.text    = stats.runs.toString()
                binding.tvBatsman2B.text    = stats.ballsFaced.toString()
                binding.tvBatsman24s.text   = stats.fours.toString()
                binding.tvBatsman26s.text   = stats.sixes.toString()
                val sr = if (stats.ballsFaced > 0)
                    stats.runs.toDouble() / stats.ballsFaced * 100 else 0.0
                binding.tvBatsman2SR.text   = String.format("%.1f", sr)
            }
        }

        // ── Bowler stats force-update ──
        score.bowlerStats?.let { stats ->
            if (stats.playerId != null && stats.playerId > 0) {
                binding.tvBowlerName.text = stats.playerName ?: "Bowler"
                binding.tvBowlerO.text    = "${stats.ballsBowled / 6}.${stats.ballsBowled % 6}"
                binding.tvBowlerR.text    = stats.runsConceded.toString()
                binding.tvBowlerW.text    = stats.wickets.toString()
                val eco = if (stats.ballsBowled > 0)
                    stats.runsConceded / (stats.ballsBowled / 6.0) else 0.0
                binding.tvBowlerEco.text  = String.format("%.1f", eco)
            }
        }

        // ── lastReceivedScore update karo taake agle ball mein sahi data jaye ──
        lastReceivedScore = score

        requireContext().toastShort("Substitution applied!")
    }


    private fun setupEndInningsAction() {

        binding.layoutInningsUndo.btnUndo.setOnClickListener {
            JsonConverter.sendScore(ScoreDTO().apply {
                this.matchId   = matchResponse?.id
                this.inningsId = currentInningsId()
                this.undo      = true
            })
            showOnly(binding.layoutMainScoring.root)
        }
        binding.layoutInningsUndo.btnEndInnings.setOnClickListener {
            val scoreToSend = lastReceivedScore ?: return@setOnClickListener

            when {
                isSuperOver && isSuperOverInnings == 1 -> {
                    isSuperOverInnings = 2
                    isFirstInnings = false
                    val tempId = battingTeamId
                    battingTeamId   = bowlingTeamId
                    bowlingTeamId   = tempId
                    battingTeamName = if (battingTeamId == team1Id) team1Name else team2Name
                    bowlingTeamName = if (bowlingTeamId == team1Id) team1Name else team2Name

                    resetForNewInnings()
                    displayedBalls.clear()
                    binding.ballContainer.removeAllViews()
                    binding.tvTeamName.text = battingTeamName

                    JsonConverter.sendScore(scoreToSend.cleanForSend().copy(  // ✅
                        eventType = "End_Innings", event = "0", comment = "", undo = false, superOver = true, firstInnings = true
                    ))
                    showOnly(binding.layoutSelectPlayer.root)
                    requireContext().toastShort("Super Over Innings 1 done! Doosri team ke liye players select karo.")
                }
                isSuperOver && isSuperOverInnings == 2 -> {
                    isEndingMatch = true
                    matchResponse?.id?.let { markMatchEnded(it) }
                    JsonConverter.sendScore(scoreToSend.cleanForSend().copy(  // ✅
                        eventType = "End_Innings", event = "0", comment = "", undo = false, superOver = true, firstInnings = false
                    ))
                    showOnly(binding.layoutMainScoring.root)
                }
                !isFirstInnings -> {
                    isEndingMatch = true
                    JsonConverter.sendScore(scoreToSend.cleanForSend().copy(  // ✅
                        eventType = "End_Innings", event = "0", comment = "", undo = false
                    ))
                    showOnly(binding.layoutMainScoring.root)
                }
                else -> {
                    JsonConverter.sendScore(scoreToSend.cleanForSend().copy(  // ✅
                        eventType = "End_Innings", event = "0", comment = "", undo = false
                    ))
                    showOnly(binding.layoutMainScoring.root)
                }
            }
        }

        binding.layoutInningsUndo.btnSuperOver.setOnClickListener {
            clearSelectionState()
            val base = lastReceivedScore ?: run {
                requireContext().toastShort("Score data missing, retry karo")
                return@setOnClickListener
            }
            JsonConverter.sendScore(base.cleanForSend().copy(  // ✅
                eventType = "Super_Over", event = "0", comment = "", undo = false
            ))
            isSuperOverPending = false
            isSuperOver = true
            isSuperOverInnings = 1
            isFirstInnings = true
            binding.layoutInningsUndo.btnSuperOver.visibility = View.GONE
            resetForNewInnings()
            displayedBalls.clear()
            binding.ballContainer.removeAllViews()
            showOnly(binding.layoutSelectPlayer.root)
            requireContext().toastShort("Super Over! Select players.")
        }
    }
    private fun triggerEndInnings() {
        JsonConverter.sendScore(ScoreDTO().apply {
            this.matchId      = matchResponse?.id
            this.inningsId    = currentInningsId()
            this.teamId       = battingTeamId
            this.eventType    = "End_Innings"
            this.comment      = null
            this.undo         = false
            this.firstInnings = isFirstInnings
        })
        showOnly(binding.layoutMainScoring.root)
        requireContext().toastShort("Innings ended!")
    }

    private fun hasAlreadyVoted(matchId: Long): Boolean {
        val accountId = getAccountId()
        return requireActivity().getSharedPreferences("VotePrefs", MODE_PRIVATE)
            .getBoolean("voted_match_${matchId}_user_${accountId}", false)
    }

    private fun markAsVoted(matchId: Long) {
        val accountId = getAccountId()
        requireActivity().getSharedPreferences("VotePrefs", MODE_PRIVATE)
            .edit().putBoolean("voted_match_${matchId}_user_${accountId}", true).apply()
    }

    private fun getAccountId(): Long {
        val prefs = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return prefs.getLong("id", -1L)
    }

    private fun loadAndShowSummary() {
        if (_binding == null || !isAdded) return
        if (isVotingActive) return

        val summary = binding.layoutMatchSummary
        showOnly(summary.root)
        summary.layoutLoading.visibility = View.VISIBLE
        summary.layoutContent.visibility = View.GONE
        summary.tvError.visibility       = View.GONE

        val matchId = matchResponse?.id ?: run {
            summary.tvError.text = "Match ID not found."
            summary.layoutLoading.visibility = View.GONE
            summary.tvError.visibility = View.VISIBLE
            return
        }

        summaryPollingJob?.cancel()
        summaryPollingJob = lifecycleScope.launch {
            repeat(42) {
                if (_binding == null || !isAdded) return@launch

                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getMatchSummary(matchId)
                    }
                    val data = response.body()
                    if (data != null && data.result != null) {
                        withContext(Dispatchers.Main) {
                            if (_binding == null || !isAdded) return@withContext
                            summary.layoutLoading.visibility = View.GONE
                            summary.layoutContent.visibility = View.VISIBLE
                            summary.tvError.visibility       = View.GONE
                            bindSummaryData(data)
                        }
                        return@launch
                    }
                } catch (e: Exception) { }

                kotlinx.coroutines.delay(10_000)
            }

            withContext(Dispatchers.Main) {
                if (_binding == null || !isAdded) return@withContext
                showSummaryError("Summary load nahi hui. Dobara try karein.")
            }
        }
    }

    private fun loadAndShowVotingThenSummary() {
        if(isVotingActive)return
        clearSelectionState()
        if (_binding == null || !isAdded) return

        val matchId   = matchResponse?.id ?: run { loadAndShowSummary(); return }
        val accountId = getAccountId()

        if (hasAlreadyVoted(matchId)) {
            loadAndShowSummary()
            return
        }

        val v = binding.layoutVoting
        isVotingActive = true
        showOnly(v.root)

        v.tvVoteTeam1Name.text    = team1Name.ifEmpty { "Team 1" }
        v.tvVoteTeam2Name.text    = team2Name.ifEmpty { "Team 2" }
        selectedVotePlayerId      = null
        selectedVotePlayerName    = ""
        v.btnSubmitVote.isEnabled = false
        v.layoutSelectedPlayerBanner.visibility = View.GONE
        v.etVoteFeedback.text?.clear()

        lifecycleScope.launch {
            try {
                val resp1 = withContext(Dispatchers.IO) { RetrofitInstance.api.getPlayersByTeam(team1Id) }
                val resp2 = withContext(Dispatchers.IO) { RetrofitInstance.api.getPlayersByTeam(team2Id) }
                val players1 = if (resp1.isSuccessful) resp1.body() ?: emptyList() else emptyList()
                val players2 = if (resp2.isSuccessful) resp2.body() ?: emptyList() else emptyList()

                val onPicked: (com.example.fypproject.DTO.TeamPlayerDto, VotePlayerAdapter) -> Unit =
                    { player, fromAdapter ->
                        if (fromAdapter === voteAdapter1) voteAdapter2?.clearSelection()
                        else                               voteAdapter1?.clearSelection()
                        selectedVotePlayerId   = player.id
                        selectedVotePlayerName = player.name ?: ""
                        v.tvSelectedVotePlayer.text = selectedVotePlayerName
                        v.layoutSelectedPlayerBanner.visibility = View.VISIBLE
                        v.btnSubmitVote.isEnabled = true
                    }

                voteAdapter1 = VotePlayerAdapter(players1, onPicked)
                voteAdapter2 = VotePlayerAdapter(players2, onPicked)

                v.rvVoteTeam1.layoutManager = LinearLayoutManager(requireContext())
                v.rvVoteTeam1.adapter = voteAdapter1

                v.rvVoteTeam2.layoutManager = LinearLayoutManager(requireContext())
                v.rvVoteTeam2.adapter = voteAdapter2

            } catch (e: Exception) {
                requireContext().toastShort("Could not load players: ${e.message}")
            }
        }

        v.btnSubmitVote.setOnClickListener {
            val playerId = selectedVotePlayerId ?: return@setOnClickListener
            isVotingActive = false
            val feedback = v.etVoteFeedback.text?.toString()?.trim()
                .takeIf { !it.isNullOrEmpty() }
            submitVote(matchId, accountId, playerId, feedback)
        }

        v.btnSkipVote.setOnClickListener {
            isVotingActive = false
            loadAndShowSummary()
        }
    }

    private fun markMatchEnded(matchId: Long) {
        requireActivity()
            .getSharedPreferences("MatchPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("match_ended_$matchId", true)
            .apply()
    }

    private fun hasMatchEnded(matchId: Long): Boolean {
        val prefs = requireActivity().getSharedPreferences("MatchPrefs", MODE_PRIVATE)
        return prefs.getBoolean("match_ended_$matchId", false)
    }
    private fun submitVote(matchId: Long, accountId: Long, playerId: Long, feedback: String? = null) {
        if (accountId == -1L) {
            requireContext().toastShort("Account not found. Please login again.")
            loadAndShowSummary()
            return
        }

        val v = binding.layoutVoting
        v.btnSubmitVote.isEnabled = false
        v.btnSubmitVote.text      = "Submitting…"
        v.btnSkipVote.isEnabled   = false

        val body = buildMap<String, Any?> {
            put("matchId",   matchId)
            put("accountId", accountId)
            put("playerId",  playerId)
            put("feedback",  feedback)
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.submitVote(body)
                }

                when {
                    response.isSuccessful -> {
                        markAsVoted(matchId)
                        requireContext().toastShort("Vote submitted!")
                        loadAndShowSummary()
                    }
                    response.code() == 409 -> {
                        markAsVoted(matchId)
                        requireContext().toastShort("You already voted for this match.")
                        loadAndShowSummary()
                    }
                    response.code() == 404 -> {
                        requireContext().toastShort("Match or player not found.")
                        v.btnSubmitVote.isEnabled = true
                        v.btnSubmitVote.text      = "Submit & View Summary"
                        v.btnSkipVote.isEnabled   = true
                    }
                    else -> {
                        requireContext().toastShort("Vote failed (${response.code()}). Try again.")
                        v.btnSubmitVote.isEnabled = true
                        v.btnSubmitVote.text      = "Submit & View Summary"
                        v.btnSkipVote.isEnabled   = true
                    }
                }
            } catch (e: Exception) {
                requireContext().toastShort("Network error: ${e.message}")
                v.btnSubmitVote.isEnabled = true
                v.btnSubmitVote.text      = "Submit & View Summary"
                v.btnSkipVote.isEnabled   = true
            }
        }
    }

    private fun bindSummaryData(data: com.example.fypproject.ScoringDTO.MatchSummaryDto) {
        val summary      = binding.layoutMatchSummary
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.colorprimary)
        val redColor     = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)

        summary.tvMatchResult.text  = data.result
        summary.tvTeam1Name.text    = data.team1Name
        summary.tvTeam1Score.text   = "${data.team1Runs}-${data.team1Wickets}"
        summary.tvTeam1Overs.text   = "(${data.team1Overs})"
        summary.tvTeam2Name.text    = data.team2Name
        summary.tvTeam2Score.text   = "${data.team2Runs}-${data.team2Wickets}"
        summary.tvTeam2Overs.text   = "(${data.team2Overs})"
        summary.tvManOfTheMatch.text = data.manOfTheMatch ?: "—"
        summary.tvTeam1HeaderPerformers.text = data.team1Name
        summary.tvTeam2HeaderPerformers.text = data.team2Name

        summary.layoutTeam1Batsmen.removeAllViews()
        data.topBatsmen1?.forEach { p ->
            addPerformerRow(summary.layoutTeam1Batsmen, p.playerName ?: "", "${p.runs} runs", primaryColor)
        }
        summary.layoutTeam1Bowlers.removeAllViews()
        data.topBowlers1?.forEach { p ->
            addPerformerRow(summary.layoutTeam1Bowlers, p.playerName ?: "", "${p.wickets}-${p.runsConceded}", redColor)
        }
        summary.layoutTeam2Batsmen.removeAllViews()
        data.topBatsmen2?.forEach { p ->
            addPerformerRow(summary.layoutTeam2Batsmen, p.playerName ?: "", "${p.runs} runs", primaryColor)
        }
        summary.layoutTeam2Bowlers.removeAllViews()
        data.topBowlers2?.forEach { p ->
            addPerformerRow(summary.layoutTeam2Bowlers, p.playerName ?: "", "${p.wickets}-${p.runsConceded}", redColor)
        }

        summary.layoutLoading.visibility = View.GONE
        summary.tvError.visibility       = View.GONE
        summary.layoutContent.visibility = View.VISIBLE
    }

    private fun addPerformerRow(
        container: android.widget.LinearLayout,
        name: String,
        stat: String,
        statColor: Int
    ) {
        val row = layoutInflater.inflate(R.layout.item_performer_row, container, false)
        row.findViewById<android.widget.TextView>(R.id.tvPlayerName).text = name
        val tvStat = row.findViewById<android.widget.TextView>(R.id.tvPlayerStat)
        tvStat.text = stat
        tvStat.setTextColor(statColor)
        container.addView(row)
    }

    private fun showSummaryError(message: String) {
        activity?.runOnUiThread {
            val summary = binding.layoutMatchSummary
            summary.layoutLoading.visibility = View.GONE
            summary.layoutContent.visibility = View.GONE
            summary.tvError.visibility       = View.VISIBLE
            summary.tvError.text             = message
        }
    }

    private fun setupDLSAction() {
        binding.layoutDLS.btnConfirmDLS.setOnClickListener {
            val targetText = binding.layoutDLS.etDLSTarget.text.toString().trim()
            if (targetText.isEmpty()) {
                requireContext().toastShort("Target enter karo"); return@setOnClickListener
            }
            val revisedTarget = targetText.toIntOrNull()
            if (revisedTarget == null || revisedTarget <= 0) {
                requireContext().toastShort("Valid number enter karo"); return@setOnClickListener
            }

            // ✅ React: {...data, eventType:"dls", dlsTarget:newTarget, event:"0"}
            val base = lastReceivedScore ?: return@setOnClickListener
            JsonConverter.sendScore(base.copy(
                eventType  = "dls",
                event      = "0",
                dlsTarget  = revisedTarget,
                undo       = false,
                comment    = ""
            ))
            binding.layoutDLS.etDLSTarget.text?.clear()
            showOnly(binding.layoutMainScoring.root)
            requireContext().toastShort("DLS target set: $revisedTarget")
        }
    }



    private fun setupPenaltyAction() {

        // ── Preset buttons — JS jaisa ──────────────────────────
        val presetButtons = listOf(
            binding.layoutPenalty.btn1Run  to 1,
            binding.layoutPenalty.btn2Runs to 2,
            binding.layoutPenalty.btn3Runs to 3,
            binding.layoutPenalty.btn5Runs to 5,
            binding.layoutPenalty.btn10Runs to 10,
        )

        presetButtons.forEach { (btn, runs) ->
            btn.setOnClickListener {
                selectedPenaltyRuns = runs
                binding.layoutPenalty.etPenaltyRuns.setText(runs.toString())
                updatePresetSelection(presetButtons, runs)
            }
        }

        // ── Custom input ───────────────────────────────────────
        binding.layoutPenalty.etPenaltyRuns.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val runs = s.toString().toIntOrNull()
                    val valid = runs != null && runs > 0
                    selectedPenaltyRuns = runs ?: selectedPenaltyRuns
                    binding.layoutPenalty.btnConfirmPenalty.isEnabled = valid
                    if (valid) {
                        val label = if (runs == 1) "Run" else "Runs"
                        binding.layoutPenalty.btnConfirmPenalty.text = "+ $runs Penalty $label"
                    } else {
                        binding.layoutPenalty.btnConfirmPenalty.text = "Award Penalty"
                    }
                }
            }
        )

        // ── Confirm — FIXED: event="PENALTY", runs mein value ──
        binding.layoutPenalty.btnConfirmPenalty.setOnClickListener {
            val runs = selectedPenaltyRuns
            if (runs <= 0) { requireContext().toastShort("Valid runs enter karo"); return@setOnClickListener }

            val base = lastReceivedScore ?: return@setOnClickListener  // null ho toh return
            JsonConverter.sendScore(base.copy(
                eventType      = "penalty",
                event          = runs.toString(),  // "5" not "PENALTY"
                runsOnThisBall = runs,
                undo           = false,
                comment        = ""
            ))
            showOnly(binding.layoutMainScoring.root)
            requireContext().toastShort("Penalty $runs runs added!")
        }

        // Default 5 select karo start mein
        binding.layoutPenalty.btn5Runs.performClick()
    }

    private fun updatePresetSelection(
        buttons: List<Pair<Button, Int>>,
        selected: Int
    ) {
        buttons.forEach { (btn, runs) ->
            if (runs == selected) {
                // Selected — white background, red text
                btn.setBackgroundResource(R.drawable.bg_white_solid)
                btn.setTextColor(android.graphics.Color.parseColor("#E31212"))
            } else {
                // Unselected — outline only
                btn.setBackgroundResource(R.drawable.bg_white_outline)
                btn.setTextColor(android.graphics.Color.WHITE)
            }
        }
    }

    private fun resetPenaltyPanel() {
        selectedPenaltyRuns = 5
        binding.layoutPenalty.etPenaltyRuns.setText("5")
        binding.layoutPenalty.btn5Runs.performClick()
        binding.layoutPenalty.btnConfirmPenalty.isEnabled = true
        binding.layoutPenalty.btnConfirmPenalty.text = "+ 5 Penalty Runs"
    }

    private fun showAbandonConfirmationDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Abandon Match")
        builder.setMessage("Are you sure you want to abandon this match?")
        builder.setPositiveButton("Yes, Abandon") { dialog, _ -> abandonMatch(); dialog.dismiss() }
        builder.setNegativeButton("No, Go Back")  { dialog, _ ->
            dialog.dismiss(); showOnly(binding.layoutMainScoring.root)
        }
        builder.setCancelable(false)
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFFF5555.toInt())
        alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF4CAF50.toInt())
    }

    private fun abandonMatch() {
        val matchId = matchResponse?.id ?: run {
            requireContext().toastShort("Match ID not found!"); return
        }
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { RetrofitInstance.api.abandonMatch(matchId) }
                if (response.isSuccessful) {
                    clearSelectionState()
                    activity?.runOnUiThread {
                        showOnly(binding.layoutMatchSummary.root)

                        binding.layoutMatchSummary.apply {
                            layoutLoading.visibility = View.GONE
                            layoutContent.visibility = View.GONE
                            tvError.visibility       = View.VISIBLE
                            tvError.text             = "🚫 Match Abandoned"
                            tvError.setTextColor(android.graphics.Color.parseColor("#E31212"))
                            tvError.textSize = 24f
                        }
                    }
                } else {
                    requireContext().toastShort("Failed to abandon match: ${response.code()}")
                    showOnly(binding.layoutMainScoring.root)
                }
            } catch (e: Exception) {
                requireContext().toastLong("Error: ${e.message}")
                showOnly(binding.layoutMainScoring.root)
            }
        }
    }

    private fun setupWicketPanel() {
        binding.layoutWicketPanel.apply {
            btnBowled.setOnClickListener    { showOnly(binding.layoutBowled.root)        }
            btnLBW.setOnClickListener       { showOnly(binding.layoutLBW.root)           }
            btnManked.setOnClickListener    { showOnly(binding.layoutManked.root)        }
            btnOverFence.setOnClickListener { showOnly(binding.layoutOvertheFence.root)  }
            btnHitWicket.setOnClickListener { showOnly(binding.layoutHitWicket.root)     }
            btnOneHand.setOnClickListener   { showOnly(binding.layoutOneHand.root)       }
            btnCaught.setOnClickListener    { resetCaughtPanel();  showOnly(binding.layoutCaught.root)  }
            btnStumped.setOnClickListener   { resetStumpedPanel(); showOnly(binding.layoutStumped.root) }
            btnRunOut.setOnClickListener    { resetRunOutPanel();  refreshRunOutNames(); showOnly(binding.layoutRunOut.root)  }
            btnRetired.setOnClickListener   { resetRetiredPanel(); refreshRetiredNames(); showOnly(binding.layoutRetired.root) }
        }

        binding.layoutBowled.apply {
            btnConfirmBowled.setOnClickListener { openNewBatsmanDialog("bowled", currentStrikerId!!, null, 0) }
            btnCloseBowled.setOnClickListener   { showOnly(binding.layoutMainScoring.root) }
        }
        binding.layoutLBW.apply {
            btnConfirmLBW.setOnClickListener { openNewBatsmanDialog("lbw", currentStrikerId!!, null, 0) }
            btnCloseLBW.setOnClickListener   { showOnly(binding.layoutMainScoring.root) }
        }
        binding.layoutManked.apply {
            btnConfirmMankad.setOnClickListener { openNewBatsmanDialog("mankad", currentNonStrikerId!!, null, 0) }
            btnCloseManked.setOnClickListener   { showOnly(binding.layoutMainScoring.root) }
        }
        binding.layoutOvertheFence.apply {
            btnConfirmOverTheFence.setOnClickListener { openNewBatsmanDialog("overthefence", currentStrikerId!!, null, 0) }
            btnCloseOverTheFence.setOnClickListener   { showOnly(binding.layoutMainScoring.root) }
        }
        binding.layoutHitWicket.apply {
            btnConfirmHitWicket.setOnClickListener { openNewBatsmanDialog("hitwicket", currentStrikerId!!, null, 0) }
            btnCloseHitWicket.setOnClickListener   { showOnly(binding.layoutMainScoring.root) }
        }
        binding.layoutOneHand.apply {
            btnConfirmOneHand.setOnClickListener { openNewBatsmanDialog("onehandonebounce", currentStrikerId!!, null, 0) }
            btnCloseOneHand.setOnClickListener   { showOnly(binding.layoutMainScoring.root) }
        }
//        binding.layoutCaught.apply {
//            btnSelectFielderCaught.setOnClickListener {
//                openFielderSelectionDialog { selectedFielder ->
//                    wicketFielderId = selectedFielder.id
//                    tvSelectedFielderCaught.text = "Fielder: ${selectedFielder.name}"
//                    tvSelectedFielderCaught.setTextColor(0xFF4CAF50.toInt())
//                    btnConfirmCaught.isEnabled = true
//                }
//            }
//            btnConfirmCaught.setOnClickListener {
//                openNewBatsmanDialog("caught", currentStrikerId!!, wicketFielderId, 0)
//            }
//            btnCloseCaught.setOnClickListener { resetCaughtPanel(); showOnly(binding.layoutMainScoring.root) }
//        }
        binding.layoutCaught.apply {
            btnSelectFielderCaught.setOnClickListener {
                openFielderSelectionDialog { selectedFielder ->
                    wicketFielderId = selectedFielder.id
                    tvSelectedFielderCaught.text = "Fielder: ${selectedFielder.name}"
                    tvSelectedFielderCaught.setTextColor(0xFF4CAF50.toInt())

                    if (isDoubleWicket) {
                        // ← Fielder select hote hi seedha out — confirm button skip
                        sendWicketEvent("caught", currentStrikerId!!, null, selectedFielder.id, 0)
                        resetCaughtPanel()
                        showOnly(binding.layoutMainScoring.root)
                    } else {
                        btnConfirmCaught.isEnabled = true
                    }
                }
            }
            btnConfirmCaught.setOnClickListener {
                openNewBatsmanDialog("caught", currentStrikerId!!, wicketFielderId, 0)
            }
            btnCloseCaught.setOnClickListener {
                resetCaughtPanel()
                showOnly(binding.layoutMainScoring.root)
            }
        }
//        binding.layoutStumped.apply {
//            btnSelectFielderStumped.setOnClickListener {
//                openFielderSelectionDialog { selectedFielder ->
//                    wicketFielderId = selectedFielder.id
//                    tvSelectedFielderStumped.text = "Keeper: ${selectedFielder.name}"
//                    tvSelectedFielderStumped.setTextColor(0xFF4CAF50.toInt())
//                    btnConfirmStumped.isEnabled = true
//                }
//            }
//            btnConfirmStumped.setOnClickListener {
//                openNewBatsmanDialog("stumped", currentStrikerId!!, wicketFielderId, 0)
//            }
//            btnCloseStumped.setOnClickListener { resetStumpedPanel(); showOnly(binding.layoutMainScoring.root) }
//        }

        binding.layoutStumped.apply {
            btnSelectFielderStumped.setOnClickListener {
                openFielderSelectionDialog { selectedFielder ->
                    wicketFielderId = selectedFielder.id
                    tvSelectedFielderStumped.text = "Keeper: ${selectedFielder.name}"
                    tvSelectedFielderStumped.setTextColor(0xFF4CAF50.toInt())

                    if (isDoubleWicket) {
                        // ← Fielder select hote hi seedha out
                        sendWicketEvent("stumped", currentStrikerId!!, null, selectedFielder.id, 0)
                        resetStumpedPanel()
                        showOnly(binding.layoutMainScoring.root)
                    } else {
                        btnConfirmStumped.isEnabled = true
                    }
                }
            }
            btnConfirmStumped.setOnClickListener {
                openNewBatsmanDialog("stumped", currentStrikerId!!, wicketFielderId, 0)
            }
            btnCloseStumped.setOnClickListener {
                resetStumpedPanel()
                showOnly(binding.layoutMainScoring.root)
            }
        }

        setupRunOutPanel()
        setupRetiredPanel()
    }

    private fun setupRunOutPanel() {
        binding.layoutRunOut.apply {
            btnRunOutBatsman1.setOnClickListener {
                wicketOutPlayerId = currentStrikerId
                tvRunOutSelected.text = "Out: ${btnRunOutBatsman1.text}"
                tvRunOutSelected.setTextColor(0xFFFF9800.toInt())
                btnRunOutBatsman1.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF5555.toInt())
                btnRunOutBatsman2.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt())
                updateRunOutDoneButton()
            }
            btnRunOutBatsman2.setOnClickListener {
                wicketOutPlayerId = currentNonStrikerId
                tvRunOutSelected.text = "Out: ${btnRunOutBatsman2.text}"
                tvRunOutSelected.setTextColor(0xFFFF9800.toInt())
                btnRunOutBatsman2.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF5555.toInt())
                btnRunOutBatsman1.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt())
                updateRunOutDoneButton()
            }
            btnSelectFielderRunOut.setOnClickListener {
                openFielderSelectionDialog { selectedFielder ->
                    wicketFielderId = selectedFielder.id
                    tvRunOutFielder.text = "Fielder: ${selectedFielder.name}"
                    tvRunOutFielder.setTextColor(0xFF4CAF50.toInt())
                    updateRunOutDoneButton()
                }
            }
            wicketRunOutRuns = 0
            btnRunOut0.setOnClickListener { selectRunOutRuns(0) }
            btnRunOut1.setOnClickListener { selectRunOutRuns(1) }
            btnRunOut2.setOnClickListener { selectRunOutRuns(2) }
            btnRunOut3.setOnClickListener { selectRunOutRuns(3) }
            btnConfirmRunOut.setOnClickListener {
                val outId = wicketOutPlayerId ?: return@setOnClickListener
                if (isNoBallRunOut) {
                    openNewBatsmanDialogForNoBallRunOut(outId, wicketFielderId)
                } else {
                    openNewBatsmanDialog("runout", outId, wicketFielderId, wicketRunOutRuns)
                }
            }

            btnCloseRunOut.setOnClickListener {
                if (isNoBallRunOut) {
                    isNoBallRunOut = false
                    noBallRunOutRuns = 0
                    resetRunOutPanel()
                    showOnly(binding.layoutNoBallPanel.root)
                } else {
                    resetRunOutPanel()
                    showOnly(binding.layoutMainScoring.root)
                }
            }
        }
    }

    private fun refreshRunOutNames() {
        binding.layoutRunOut.apply {
            btnRunOutBatsman1.text = binding.tvBatsman1Name.text.toString().replace("🏏 ", "").ifEmpty { "Striker" }
            btnRunOutBatsman2.text = binding.tvBatsman2Name.text.toString().ifEmpty { "Non-Striker" }
        }
    }

    private fun selectRunOutRuns(runs: Int) {
        wicketRunOutRuns = runs
        binding.layoutRunOut.tvRunOutRuns.text = "Runs: $runs"
        val buttons = listOf(
            binding.layoutRunOut.btnRunOut0, binding.layoutRunOut.btnRunOut1,
            binding.layoutRunOut.btnRunOut2, binding.layoutRunOut.btnRunOut3
        )
        buttons.forEachIndexed { index, btn ->
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (index == runs) 0xFF666666.toInt() else 0xFF444444.toInt()
            )
        }
    }

//    private fun updateRunOutDoneButton() {
//        binding.layoutRunOut.btnConfirmRunOut.isEnabled =
//            wicketOutPlayerId != null && wicketFielderId != null
//    }

    private fun updateRunOutDoneButton() {
        val bothSelected = wicketOutPlayerId != null && wicketFielderId != null

        if (bothSelected && isDoubleWicket) {
            // ← Dono select hote hi seedha out — confirm button ki zaroorat nahi
            val outId = wicketOutPlayerId ?: return
            sendWicketEvent("runout", outId, null, wicketFielderId, wicketRunOutRuns)
            resetRunOutPanel()
            showOnly(binding.layoutMainScoring.root)
        } else {
            binding.layoutRunOut.btnConfirmRunOut.isEnabled = bothSelected
        }
    }

    private fun setupRetiredPanel() {
        binding.layoutRetired.apply {
            btnRetiredBatsman1.setOnClickListener {
                wicketOutPlayerId = currentStrikerId
                tvRetiredSelected.text = "Retiring: ${btnRetiredBatsman1.text}"
                tvRetiredSelected.setTextColor(0xFFFF9800.toInt())
                btnRetiredBatsman1.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF5555.toInt())
                btnRetiredBatsman2.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt())
                btnConfirmRetired.isEnabled = true
            }
            btnRetiredBatsman2.setOnClickListener {
                wicketOutPlayerId = currentNonStrikerId
                tvRetiredSelected.text = "Retiring: ${btnRetiredBatsman2.text}"
                tvRetiredSelected.setTextColor(0xFFFF9800.toInt())
                btnRetiredBatsman2.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF5555.toInt())
                btnRetiredBatsman1.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt())
                btnConfirmRetired.isEnabled = true
            }
            btnConfirmRetired.setOnClickListener {
                val outId = wicketOutPlayerId ?: return@setOnClickListener
                openNewBatsmanDialog("retired", outId, null, 0)
            }
            btnCloseRetired.setOnClickListener { resetRetiredPanel(); showOnly(binding.layoutMainScoring.root) }
        }
    }

    private fun refreshRetiredNames() {
        binding.layoutRetired.apply {
            btnRetiredBatsman1.text = binding.tvBatsman1Name.text.toString().replace("🏏 ", "").ifEmpty { "Striker" }
            btnRetiredBatsman2.text = binding.tvBatsman2Name.text.toString().ifEmpty { "Non-Striker" }
        }
    }

    private fun openFielderSelectionDialog(onFielderSelected: (TeamPlayerDto) -> Unit) {
        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_player_selection, null)
        val rvPlayers  = dialogView.findViewById<RecyclerView>(R.id.rvPlayersList)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmSelection)
        val btnClose   = dialogView.findViewById<Button>(R.id.btnClosePlayer)
        rvPlayers.layoutManager = LinearLayoutManager(context)
        btnClose.setOnClickListener { dialog.dismiss() }

        // ✅ FIX: pehle availableBowlers use karo (same as openPlayerSelectionDialog)
        val preloadedFielders = availableBowlers.ifEmpty { null }

        if (preloadedFielders != null) {
            val adapter = PlayerSelectionAdapter(preloadedFielders) { }
            rvPlayers.adapter = adapter
            btnConfirm.setOnClickListener {
                val selected = adapter.getSelectedPlayer()
                if (selected != null) { onFielderSelected(selected); dialog.dismiss() }
                else requireContext().toastShort("Please select a player")
            }
            dialog.setView(dialogView)
            dialog.show()
        } else {
            // Fallback: API se lo
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getPlayersByTeam(bowlingTeamId)
                    }
                    if (response.isSuccessful) {
                        val players = response.body() ?: emptyList()
                        val adapter = PlayerSelectionAdapter(players) { }
                        rvPlayers.adapter = adapter
                        btnConfirm.setOnClickListener {
                            val selected = adapter.getSelectedPlayer()
                            if (selected != null) { onFielderSelected(selected); dialog.dismiss() }
                            else requireContext().toastShort("Please select a player")
                        }
                    }
                } catch (e: Exception) {
                    requireContext().toastLong("Error loading players")
                    dialog.dismiss()
                }
            }
            dialog.setView(dialogView)
            dialog.show()
        }
    }

    private fun openNewBatsmanDialog(
        dismissalType: String,
        outPlayerId: Long,
        fielderId: Long?,
        runsOnBall: Int
    ) {
        if (isDoubleWicket) {
            sendWicketEvent(dismissalType, outPlayerId, null, fielderId, runsOnBall)
            showOnly(binding.layoutMainScoring.root)
            return
        }


        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_player_selection, null)
        val rvPlayers  = dialogView.findViewById<RecyclerView>(R.id.rvPlayersList)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmSelection)
        val btnClose   = dialogView.findViewById<Button>(R.id.btnClosePlayer)
        rvPlayers.layoutManager = LinearLayoutManager(context)
        btnClose.setOnClickListener {
            dialog.dismiss()
            showOnly(binding.layoutMainScoring.root)
        }

        val battersToShow = availableBatters.ifEmpty { null }

        if (battersToShow != null) {
            val adapter = PlayerSelectionAdapter(battersToShow) { }
            rvPlayers.adapter = adapter
            btnConfirm.setOnClickListener {
                val selected = adapter.getSelectedPlayer()
                if (selected != null) {
                    sendWicketEvent(dismissalType, outPlayerId, selected.id, fielderId, runsOnBall)
                    dialog.dismiss()
                    showOnly(binding.layoutMainScoring.root)
                } else requireContext().toastShort("Please select new batsman")
            }
            dialog.setView(dialogView)
            dialog.show()
        } else {
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getPlayersByTeam(battingTeamId)
                    }
                    if (response.isSuccessful) {
                        val players = response.body() ?: emptyList()
                        val adapter = PlayerSelectionAdapter(players) { }
                        rvPlayers.adapter = adapter
                        btnConfirm.setOnClickListener {
                            val selected = adapter.getSelectedPlayer()
                            if (selected != null) {
                                sendWicketEvent(dismissalType, outPlayerId, selected.id!!, fielderId, runsOnBall)
                                dialog.dismiss()
                                showOnly(binding.layoutMainScoring.root)
                            } else requireContext().toastShort("Please select new batsman")
                        }
                    }
                } catch (e: Exception) {
                    requireContext().toastLong("Error loading players")
                    dialog.dismiss()
                }
            }
            dialog.setView(dialogView)
            dialog.show()
        }
    }

    private fun showNoBallRunOutRunsDialog() {
        val editText = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Runs scored off No Ball"
            setText("0")
            setPadding(32, 16, 32, 16)
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("NB + Run Out")
            .setMessage("Is no ball par kitne runs bane? (fielder ka run out alag se select hoga)")
            .setView(editText)
            .setPositiveButton("Proceed to Run Out") { _, _ ->
                noBallRunOutRuns = editText.text.toString().toIntOrNull() ?: 0
                isNoBallRunOut   = true
                resetRunOutPanel()
                refreshRunOutNames()
                showOnly(binding.layoutRunOut.root)
            }
            .setNegativeButton("Cancel") { d, _ ->
                d.dismiss()
                showOnly(binding.layoutMainScoring.root)
            }
            .show()
    }

    private fun openNewBatsmanDialogForNoBallRunOut(outPlayerId: Long, fielderId: Long?) {
        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_player_selection, null)
        val rvPlayers  = dialogView.findViewById<RecyclerView>(R.id.rvPlayersList)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmSelection)
        val btnClose   = dialogView.findViewById<Button>(R.id.btnClosePlayer)
        rvPlayers.layoutManager = LinearLayoutManager(context)

        btnClose.setOnClickListener {
            dialog.dismiss()
            isNoBallRunOut   = false
            noBallRunOutRuns = 0
        }

        val confirmAction = fun(_: android.view.View, adapter: com.example.fypproject.Adapter.PlayerSelectionAdapter) {
            val selected = adapter.getSelectedPlayer()
            if (!isDoubleWicket && selected == null) {
                requireContext().toastShort("Please select new batsman")
                return
            }
            sendNoBallRunOutEvent(outPlayerId, selected?.id, fielderId, noBallRunOutRuns)
            isNoBallRunOut   = false
            noBallRunOutRuns = 0
            dialog.dismiss()
            showOnly(binding.layoutMainScoring.root)
        }

        val battersToShow = availableBatters.ifEmpty { null }

        if (battersToShow != null) {
            val adapter = com.example.fypproject.Adapter.PlayerSelectionAdapter(battersToShow) { }
            rvPlayers.adapter = adapter
            btnConfirm.setOnClickListener { confirmAction(it, adapter) }
            dialog.setView(dialogView)
            dialog.show()
        } else {
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getPlayersByTeam(battingTeamId)
                    }
                    if (response.isSuccessful) {
                        val players = response.body() ?: emptyList()
                        val adapter = com.example.fypproject.Adapter.PlayerSelectionAdapter(players) { }
                        rvPlayers.adapter = adapter
                        btnConfirm.setOnClickListener { confirmAction(it, adapter) }
                    }
                } catch (e: Exception) {
                    requireContext().toastLong("Error loading players")
                    dialog.dismiss()
                }
            }
            dialog.setView(dialogView)
            dialog.show()
        }
    }

    private fun sendNoBallRunOutEvent(
        outPlayerId: Long,
        newBatsmanId: Long?,
        fielderId: Long?,
        extraRuns: Int
    ) {
        if (isBallPending) return
        isBallPending = true
        setScoringPanelEnabled(false)

        val base = lastReceivedScore ?: ScoreDTO()
        JsonConverter.sendScore(base.cleanForSend().copy(
            matchId        = matchResponse?.id,
            teamId         = battingTeamId,
            inningsId      = currentInningsId(),
            batsmanId      = currentStrikerId,
            nonStrikerId   = currentNonStrikerId,
            bowlerId       = currentBowlerId,
            outPlayerId    = outPlayerId,
            newPlayerId    = newBatsmanId,
            fielderId      = fielderId,
            overs          = currentOvers,
            balls          = currentBalls,
            runsOnThisBall = 0,                  // always 0 for NB run out
            event          = extraRuns.toString(), // NB runs (e.g. "2")
            eventType      = "noball_runout",
            dismissalType  = "runout",
            status         = "LIVE",
            isLegal        = false,               // no ball is illegal delivery
            firstInnings   = isFirstInnings,
            undo           = false,
            comment        = ""
        ))

        // Local ID update — only if new batsman selected
        if (newBatsmanId != null) {
            if (outPlayerId == currentStrikerId) {
                currentStrikerId = newBatsmanId
                row1PlayerId     = newBatsmanId
            } else if (outPlayerId == currentNonStrikerId) {
                currentNonStrikerId = newBatsmanId
                row2PlayerId        = newBatsmanId
            }
        }

        wicketFielderId   = null
        wicketOutPlayerId = null
        wicketRunOutRuns  = 0
    }

    private fun sendWicketEvent(
        dismissalType: String,
        outPlayerId: Long,
        newBatsmanId: Long?,   // ← Long? (nullable) — double wicket ke liye
        fielderId: Long?,
        runsOnBall: Int
    ) {
        if (currentStrikerId == null || currentNonStrikerId == null || currentBowlerId == null) {
            requireContext().toastShort("Players not selected!")
            return
        }
        if (isBallPending) return
        isBallPending = true
        setScoringPanelEnabled(false)
        JsonConverter.sendScore(ScoreDTO().apply {
            this.matchId        = matchResponse?.id
            this.teamId         = battingTeamId
            this.inningsId      = currentInningsId()
            this.batsmanId      = currentStrikerId
            this.nonStrikerId   = currentNonStrikerId
            this.bowlerId       = currentBowlerId
            this.outPlayerId    = outPlayerId
            this.newPlayerId    = newBatsmanId   // null allowed in double wicket
            this.fielderId      = fielderId
            this.overs          = currentOvers
            this.balls          = currentBalls
            this.runsOnThisBall = runsOnBall
            this.event          = runsOnBall.toString()
            this.eventType      = "wicket"
            this.dismissalType  = dismissalType
            this.status         = "LIVE"
            this.isLegal        = true
            this.firstInnings   = isFirstInnings
        })

        // Update local striker/non-striker only if new batsman selected
        if (newBatsmanId != null) {
            if (outPlayerId == currentStrikerId) {
                currentStrikerId = newBatsmanId
                row1PlayerId     = newBatsmanId
            } else if (outPlayerId == currentNonStrikerId) {
                currentNonStrikerId = newBatsmanId
                row2PlayerId        = newBatsmanId
            }
        }

        wicketFielderId   = null
        wicketOutPlayerId = null
        wicketRunOutRuns  = 0
    }

    private fun resetCaughtPanel() {
        wicketFielderId = null
        binding.layoutCaught.tvSelectedFielderCaught.text = "No fielder selected"
        binding.layoutCaught.tvSelectedFielderCaught.setTextColor(0xFFAAAAAA.toInt())
        binding.layoutCaught.btnConfirmCaught.isEnabled = false
    }

    private fun resetStumpedPanel() {
        wicketFielderId = null
        binding.layoutStumped.tvSelectedFielderStumped.text = "No keeper selected"
        binding.layoutStumped.tvSelectedFielderStumped.setTextColor(0xFFAAAAAA.toInt())
        binding.layoutStumped.btnConfirmStumped.isEnabled = false
    }

    private fun resetRunOutPanel() {
        wicketOutPlayerId = null
        wicketFielderId   = null
        wicketRunOutRuns  = 0
        binding.layoutRunOut.apply {
            tvRunOutSelected.text = "No batsman selected"
            tvRunOutSelected.setTextColor(0xFFAAAAAA.toInt())
            tvRunOutFielder.text = "No fielder selected"
            tvRunOutFielder.setTextColor(0xFFAAAAAA.toInt())
            tvRunOutRuns.text = "Runs: 0"
            btnConfirmRunOut.isEnabled = false
            btnRunOutBatsman1.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt())
            btnRunOutBatsman2.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt())
        }
    }

    private fun resetRetiredPanel() {
        wicketOutPlayerId = null
        binding.layoutRetired.apply {
            tvRetiredSelected.text = "No batsman selected"
            tvRetiredSelected.setTextColor(0xFFAAAAAA.toInt())
            btnConfirmRetired.isEnabled = false
            btnRetiredBatsman1.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt())
            btnRetiredBatsman2.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt())
        }
    }

    private fun setupAdminSelectionFlow() {
        binding.layoutSelectPlayer.apply {
            btnSelectBatsman.setOnClickListener {
                if (!b1Selected)      openPlayerSelectionDialog("batsman1", battingTeamId)
                else if (!b2Selected) openPlayerSelectionDialog("batsman2", battingTeamId)
                else requireContext().toastShort("Both batsmen selected")
            }
            btnSelectBowler.setOnClickListener {
                openPlayerSelectionDialog("bowler", bowlingTeamId)
            }
        }
        binding.layoutSelectBowler.apply {
            btnSelectBowler.setOnClickListener {
                openPlayerSelectionDialog("bowler", bowlingTeamId)
            }
        }
    }

    private fun openPlayerSelectionDialog(selectionType: String, teamId: Long) {
        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_player_selection, null)
        val rvPlayers  = dialogView.findViewById<RecyclerView>(R.id.rvPlayersList)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmSelection)
        val btnClose   = dialogView.findViewById<Button>(R.id.btnClosePlayer)
        rvPlayers.layoutManager = LinearLayoutManager(context)
        btnClose.setOnClickListener {
            dialog.dismiss()
            showOnly(binding.layoutMainScoring.root)
        }

        // ✅ Bowler: use availableBowlers from WS (same as JS)
        val preloadedList = when (selectionType) {
            "bowler"   -> availableBowlers.ifEmpty { null }
            "batsman1" -> availableBatters.ifEmpty { null }
            "batsman2" -> availableBatters
                .filter { it.id != currentStrikerId }
                .ifEmpty { null }
            else       -> null
        }

        if (preloadedList != null) {
            val adapter = PlayerSelectionAdapter(preloadedList) { }
            rvPlayers.adapter = adapter
            btnConfirm.setOnClickListener {
                val selected = adapter.getSelectedPlayer()
                if (selected != null) { handleSelection(selectionType, selected); dialog.dismiss() }
                else requireContext().toastShort("Please select a player")
            }
            dialog.setView(dialogView)
            dialog.show()
        } else {
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getPlayersByTeam(teamId)
                    }
                    if (response.isSuccessful) {
                        var players = response.body() ?: emptyList()
                        if (selectionType == "batsman2") {
                            players = players.filter { it.id != currentStrikerId }
                        }
                        val adapter = PlayerSelectionAdapter(players) { }
                        rvPlayers.adapter = adapter
                        btnConfirm.setOnClickListener {
                            val selected = adapter.getSelectedPlayer()
                            if (selected != null) { handleSelection(selectionType, selected); dialog.dismiss() }
                            else requireContext().toastShort("Please select a player")
                        }
                    }
                } catch (e: Exception) {
                    requireContext().toastLong("Error loading players")
                    dialog.dismiss()
                }
            }
            dialog.setView(dialogView)
            dialog.show()
        }
    }

    private fun handleSelection(type: String, player: TeamPlayerDto) {
        when (type) {
            "batsman1" -> {
                currentStrikerId = player.id
                row1PlayerId     = player.id
                binding.tvBatsman1Name.text = "🏏 ${player.name}"
                binding.layoutSelectPlayer.btnSelectBatsman.text = "B1: ${player.name} (Select B2)"
                resetBatsman1UI()
                b1Selected = true
            }
            "batsman2" -> {
                if (player.id == currentStrikerId) {
                    requireContext().toastShort("Batsman 2 cannot be the same as Batsman 1!")
                    return
                }
                currentNonStrikerId = player.id
                row2PlayerId        = player.id
                binding.tvBatsman2Name.text = player.name
                binding.layoutSelectPlayer.btnSelectBatsman.text = "Batsmen: Selected"
                resetBatsman2UI()
                b2Selected = true
            }
            "bowler" -> {
                currentBowlerId = player.id
                binding.tvBowlerName.text = player.name
                if (binding.layoutSelectPlayer.root.visibility == View.VISIBLE)
                    binding.layoutSelectPlayer.btnSelectBowler.text = "Bowler: ${player.name}"
                else if (binding.layoutSelectBowler.root.visibility == View.VISIBLE)
                    binding.layoutSelectBowler.btnSelectBowler.text = "Bowler: ${player.name}"
                resetBowlerUI()
                bowlerSelected = true
            }
        }
        checkAutoStart()
    }

    private fun checkAutoStart() {
        if (b1Selected && b2Selected && bowlerSelected) {
            showOnly(binding.layoutMainScoring.root)
            saveSelectionState()
        }
    }

    private fun resetBatsman1UI() {
        binding.tvBatsman1R.text  = "0"
        binding.tvBatsman1B.text  = "0"
        binding.tvBatsman14s.text = "0"
        binding.tvBatsman16s.text = "0"
        binding.tvBatsman1SR.text = "0.0"
    }

    private fun resetBatsman2UI() {
        binding.tvBatsman2R.text  = "0"
        binding.tvBatsman2B.text  = "0"
        binding.tvBatsman24s.text = "0"
        binding.tvBatsman26s.text = "0"
        binding.tvBatsman2SR.text = "0.0"
    }

    private fun resetBowlerUI() {
        binding.tvBowlerO.text   = "0.0"
        binding.tvBowlerR.text   = "0"
        binding.tvBowlerW.text   = "0"
        binding.tvBowlerEco.text = "0.0"
    }

    private fun showOnly(activePanel: View) {
        binding.layoutScoringHeader.visibility =
            if (activePanel == binding.layoutMatchSummary.root || activePanel == binding.layoutVoting.root)
                View.GONE else View.VISIBLE
        listOf(
            binding.layoutSelectPlayer.root,
            binding.layoutMainScoring.root,
            binding.layoutUserHistory,
            binding.layoutByePanel.root,
            binding.layoutWidePanel.root,
            binding.layoutNoBallPanel.root,
            binding.layoutLegByePanel.root,
            binding.layoutWicketPanel.root,
            binding.layoutBowled.root,
            binding.layoutLBW.root,
            binding.layoutRetired.root,
            binding.layoutManked.root,
            binding.layoutHitWicket.root,
            binding.layoutOvertheFence.root,
            binding.layoutStumped.root,
            binding.layoutCaught.root,
            binding.layoutRunOut.root,
            binding.layoutOneHand.root,
            binding.layoutSelectBowler.root,
            binding.layoutEndInnings.root,
            binding.layoutPenalty.root,
            binding.layoutDLS.root,
            binding.layoutVoting.root,
            binding.layoutSuperOver.root,
            binding.layoutAbandon.root,
            binding.layoutMorePanel.root,
            binding.layoutInningsUndo.root,
            binding.layoutMatchSummary.root
        ).forEach { it.visibility = View.GONE }

        activePanel.visibility = View.VISIBLE
    }

    private fun ScoreDTO.cleanForSend(): ScoreDTO = this.copy(
        availableBatters = null,
        availableBowlers = null,
        cricketBalls     = null,
        batsman1Stats    = null,
        batsman2Stats    = null,
        bowlerStats      = null
    )

    companion object {
        fun newInstance(match: MatchResponse): ScoringFragment {
            return ScoringFragment().apply {
                arguments = Bundle().apply { putSerializable("match_response", match) }
            }
        }
    }
}