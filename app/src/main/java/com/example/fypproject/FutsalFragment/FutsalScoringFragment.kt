package com.example.fypproject.FutsalFragment

import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.FutsalEventsAdapter
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Dialog.MilestoneDialog
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.FutsalEventDTO
import com.example.fypproject.ScoringDTO.Player
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.MilestoneDetector
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.FutsalScoringFragmentBinding
import kotlinx.coroutines.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.timerTask

class FutsalScoringFragment : Fragment(R.layout.futsal_scoring_fragment) {
    private var _binding: FutsalScoringFragmentBinding? = null
    private val binding get() = _binding!!

    private var prevDataSnapshot: Map<String, Any?>? = null
    private var lastSocketJson: JSONObject? = null
    private var matchResponse: MatchResponse? = null
    private var team1Players = listOf<Player>()
    private var team2Players = listOf<Player>()
    private var team1Active = listOf<Player>()
    private var team2Active = listOf<Player>()
    private var pendingComment: String? = null
    private var currentStatus  = "LIVE"
    private var currentHalf    = 1
    private var elapsedMinutes = 0
    private var canAddMedia = false
    private val SOCKET_KEY = "FutsalScoringFragment"
    private val handler        = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var halfStartTime  = 0L
    private val eventsList     = mutableListOf<FutsalEventDTO>()
    private lateinit var eventsAdapter: FutsalEventsAdapter
    private var goalTeamId:       Long?   = null
    private var foulTeamId:       Long?   = null
    private var subTeamId:        Long?   = null
    private var votingAlreadyTriggered = false

    private var pendingSummary = false
    private var selectedCardType: String? = null
    private var pendingEventId: Long? = null
    private var cameraImageUri: Uri?  = null
    private var isUploading           = false
    private var isActionPending = false
    private var canEdit = false
    private var selectedVotePlayerId:   Long?  = null
    private var selectedVotePlayerName: String = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null
    private var lastTeam1Score = 0
    private var lastTeam2Score = 0

    private var isCompletedAndWaitingForData = false
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { uploadMediaFile(it) } }
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraImageUri?.let { uploadMediaFile(it) } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FutsalScoringFragmentBinding.bind(view)

        getBundleData()
        computeCanEdit()
        setupBottomTabs()
        setupEventsRecycler()
        registerSocketListeners()
        fetchPlayers()

        val status = matchResponse?.status?.uppercase().orEmpty()
        val isCompleted = status == "COMPLETED" || status == "MATCH_COMPLETE"

        if (isCompleted) {
            binding.scoringTabContent.visibility = View.VISIBLE
            binding.eventsTabContent.visibility  = View.GONE
            votingAlreadyTriggered = true
            isCompletedAndWaitingForData = true
            showPanel("loading")

            lifecycleScope.launch {
                delay(10_000)
                if (_binding != null && isAdded && isCompletedAndWaitingForData) {
                    isCompletedAndWaitingForData = false
                    loadAndShowVotingThenSummary()
                }
            }
        } else {
            if (canEdit) {
                showTab("scoring")
                showPanel("scoring")
            }
            else if (canAddMedia) {
                showTab("events")
            }
            else {
                showTab("events")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerSocketListeners()
        lastSocketJson?.let { handleServerUpdate(it) }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            registerSocketListeners()
            lastSocketJson?.let { handleServerUpdate(it) }
        }
        else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        timerRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }

    private fun computeCanEdit() {
        val prefs       = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val role        = prefs.getString("role", "")?.trim().orEmpty()
        val username    = prefs.getString("username", "")?.trim().orEmpty()
        val scorer      = matchResponse?.scorerId?.trim().orEmpty()
        val mediaScorer = matchResponse?.mediaScorerUsername?.trim().orEmpty()
        canEdit = role.equals("ADMIN", true)
                || scorer.equals(username, true)
        canAddMedia= canEdit || mediaScorer.equals(username, true)

    }

    private fun getBundleData() {
        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }
        binding.teamA.text = matchResponse?.team1Name ?: "Team A"
        binding.teamB.text = matchResponse?.team2Name ?: "Team B"
    }

    private fun setupBottomTabs() {
        if (!canEdit) binding.tabScoring.visibility = View.GONE
        binding.tabScoring.setOnClickListener { showTab("scoring") }
        binding.tabEvents.setOnClickListener  { showTab("events")  }
    }

    private fun showTab(tab: String) {
        val isScoring = tab == "scoring"
        binding.scoringTabContent.visibility = if (isScoring) View.VISIBLE else View.GONE
        binding.eventsTabContent.visibility  = if (isScoring) View.GONE    else View.VISIBLE
        binding.tabScoring.isSelected        = isScoring
        binding.tabEvents.isSelected         = !isScoring
        binding.tabScoring.setTextColor(
            if (isScoring) android.graphics.Color.parseColor("#E31212")
            else           android.graphics.Color.parseColor("#888888")
        )
        binding.tabEvents.setTextColor(
            if (!isScoring) android.graphics.Color.parseColor("#E31212")
            else            android.graphics.Color.parseColor("#888888")
        )
        if (!isScoring) {
            binding.tvNoEvents.visibility = if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }
    }

    private fun setupEventsRecycler() {
        eventsAdapter = FutsalEventsAdapter(eventsList) { event ->
            if (canAddMedia) showMediaDialog(event.id)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventsAdapter
    }

    private fun showPanel(panel: String) {
        binding.futsalScoring.root.visibility       = View.GONE
        binding.goal.root.visibility                = View.GONE
        binding.foul.root.visibility                = View.GONE
        binding.subsitute.root.visibility           = View.GONE
        binding.layoutVoting.root.visibility        = View.GONE
        binding.layoutFutsalSummary.root.visibility = View.GONE
        binding.layoutProgressBar.visibility        = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> { binding.futsalScoring.root.visibility = View.VISIBLE; setupScoringPanel() }
            "goal"    -> { binding.goal.root.visibility = View.VISIBLE; setupGoalPanel() }
            "foul"    -> { binding.foul.root.visibility = View.VISIBLE; setupFoulPanel() }
            "sub"     -> { binding.subsitute.root.visibility = View.VISIBLE; setupSubPanel() }
            "voting"  -> binding.layoutVoting.root.visibility = View.VISIBLE
            "summary" -> binding.layoutFutsalSummary.root.visibility = View.VISIBLE
            "loading" -> binding.layoutProgressBar.visibility = View.VISIBLE
        }
    }

    private fun setupScoringPanel() {
        val s = binding.futsalScoring

        if (!canEdit) {
            s.btnGoal.visibility         = View.GONE
            s.btnFoul.visibility         = View.GONE
            s.btnSubstitution.visibility = View.GONE
            s.btnUndo.visibility         = View.GONE
            s.btnEndHalf.visibility      = View.GONE
            return
        }
        if (canEdit) {
            s.btnEditLineup.visibility = View.VISIBLE
            s.btnEditLineup.setOnClickListener { showLineupEditor() }
        } else {
            s.btnEditLineup.visibility = View.GONE
        }

        s.btnGoal.setOnClickListener         { showPanel("goal") }
        s.btnFoul.setOnClickListener         { showPanel("foul") }
        s.btnSubstitution.setOnClickListener { showPanel("sub")  }

        s.btnUndo.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().apply { put("undo", true) })
        }

        s.btnEndHalf.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            val eventType = when (currentStatus) {
                "HALF_TIME"          -> "START_SECOND_HALF"
                "EXTRA_TIME", "DRAW" -> "EXTRA_TIME"
                else                 -> "END_HALF"
            }
            sendEvent(JSONObject().apply { put("eventType", eventType) })
        }

        updateEndHalfButtonText()
        setScoringButtonsEnabled(true)
    }

    private fun showLineupEditor() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view   = layoutInflater.inflate(R.layout.bottom_sheet_lineup_editor, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvLineupTeam1Label).text = matchResponse?.team1Name ?: "Team 1"
        view.findViewById<TextView>(R.id.tvLineupTeam2Label).text = matchResponse?.team2Name ?: "Team 2"
        view.findViewById<TextView>(R.id.tvLineupClose).setOnClickListener { dialog.dismiss() }

        val col1 = view.findViewById<android.widget.LinearLayout>(R.id.llLineupTeam1)
        val col2 = view.findViewById<android.widget.LinearLayout>(R.id.llLineupTeam2)

        buildLineupColumn(col1, team1Players, team1Active.toMutableList()) { updated ->
            team1Active = updated
        }
        buildLineupColumn(col2, team2Players, team2Active.toMutableList()) { updated ->
            team2Active = updated
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnSaveLineup
        ).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun buildLineupColumn(
        col: android.widget.LinearLayout,
        allPlayers: List<Player>,
        onField: MutableList<Player>,
        onUpdate: (List<Player>) -> Unit
    ) {
        col.removeAllViews()
        val dp = resources.displayMetrics.density

        allPlayers.forEach { player ->
            val isOnField = onField.any { it.id == player.id }

            val btn = android.widget.TextView(requireContext()).apply {
                text = player.name
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding((10*dp).toInt(), (10*dp).toInt(), (10*dp).toInt(), (10*dp).toInt())
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (6*dp).toInt() }

                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(
                        if (isOnField) android.graphics.Color.parseColor("#059669")
                        else android.graphics.Color.parseColor("#F1F5F9")
                    )
                    cornerRadius = 8 * dp
                }
                setTextColor(
                    if (isOnField) android.graphics.Color.WHITE
                    else android.graphics.Color.parseColor("#1e293b")
                )
            }

            btn.setOnClickListener {
                val currentlyOn = onField.any { it.id == player.id }
                if (currentlyOn) onField.removeAll { it.id == player.id }
                else onField.add(player)
                onUpdate(onField.toList())
                buildLineupColumn(col, allPlayers, onField, onUpdate)
            }

            col.addView(btn)
        }
    }

    private fun setScoringButtonsEnabled(enabled: Boolean) {
        if (_binding == null) return
        val s     = binding.futsalScoring
        val alpha = if (enabled) 1f else 0.45f
        listOf(s.btnGoal, s.btnFoul, s.btnSubstitution, s.btnUndo, s.btnEndHalf).forEach {
            it.isEnabled = enabled
            it.alpha     = alpha
        }
    }

    private fun updateEndHalfButtonText() {
        if (_binding == null) return
        binding.futsalScoring.btnEndHalf.text = when (currentStatus) {
            "HALF_TIME"          -> "Start 2nd Half"
            "EXTRA_TIME", "DRAW" -> "Start Extra Time"
            else                 -> "End Half"
        }
    }

    private fun setupGoalPanel() {
        val g = binding.goal
        goalTeamId = null
        g.tvClose.setOnClickListener { showPanel("scoring") }

        val teamNames = listOf("Select Team",
            matchResponse?.team1Name ?: "Team A",
            matchResponse?.team2Name ?: "Team B")
        val teamIds = listOf<Long?>(null, matchResponse?.team1Id, matchResponse?.team2Id)

        g.spinnerTeam.setup(teamNames) { pos ->
            goalTeamId = teamIds[pos]
            refreshGoalPlayerSpinners()
        }

        val goalTypes = listOf("Select Goal Type", "NORMAL", "PENALTY", "FREE_KICK", "OWN_GOAL")
        g.goalTypeSpinner.setup(goalTypes) { refreshGoalPlayerSpinners() }

        g.spinnerPlayer.setupEmpty("Select Scorer")
        g.spinnerAssist.setupEmpty("Select Assist (Optional)")

        g.btnSave.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            val teamId   = goalTeamId
            val gtPos    = g.goalTypeSpinner.selectedItemPosition
            val goalType = if (gtPos > 0) goalTypes[gtPos] else null
            val playerId = g.spinnerPlayer.selectedId()
            val assistId = g.spinnerAssist.selectedId()

            if (teamId   == null) { toast("Select Team");      return@setOnClickListener }
            if (goalType == null) { toast("Select Goal Type"); return@setOnClickListener }
            if (playerId == null) { toast("Select Player");    return@setOnClickListener }

            isActionPending = true
            setScoringButtonsEnabled(false)
            val evType = if (goalType == "OWN_GOAL") "OWN_GOAL" else "GOAL"
            sendEvent(JSONObject().apply {
                put("eventType", evType)
                put("goalType",  goalType)
                put("teamId",    teamId)
                put("playerId",  playerId)
                if (assistId != null) put("assistPlayerId", assistId)
            })
            showPanel("scoring")
        }
    }

    private fun refreshGoalPlayerSpinners() {
        val g         = binding.goal
        val goalTypes = listOf("Select Goal Type", "NORMAL", "PENALTY", "FREE_KICK", "OWN_GOAL")
        val goalType  = goalTypes.getOrNull(g.goalTypeSpinner.selectedItemPosition)
        val players   = if (goalType == "OWN_GOAL") getOpposingPlayers(goalTeamId)
        else getOnFieldPlayers(goalTeamId)

        g.spinnerPlayer.setupWithPlayers("Select Scorer", players)

        g.spinnerPlayer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                @Suppress("UNCHECKED_CAST")
                val scorerId = (g.spinnerPlayer.tag as? List<Int?>)?.getOrNull(pos)
                val assistPlayers = if (scorerId != null)
                    players.filter { it.id != scorerId }
                else
                    players
                g.spinnerAssist.setupWithPlayers("Select Assist (Optional)", assistPlayers)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        g.spinnerAssist.setupWithPlayers("Select Assist (Optional)", players)
    }

    private fun setupFoulPanel() {
        val f = binding.foul
        foulTeamId       = null
        selectedCardType = null
        resetCardHighlight()

        f.tvClose.setOnClickListener { showPanel("scoring") }

        val teamNames = listOf("Select Team",
            matchResponse?.team1Name ?: "Team A",
            matchResponse?.team2Name ?: "Team B")
        val teamIds = listOf<Long?>(null, matchResponse?.team1Id, matchResponse?.team2Id)

        f.spinnerTeam.setup(teamNames) { pos ->
            foulTeamId = teamIds[pos]
            refreshFoulPlayerSpinner()
        }

        f.btnSimpleFoul.setOnClickListener { selectedCardType = null;     highlightCard("FOUL")   }
        f.btnYellowCard.setOnClickListener { selectedCardType = "YELLOW"; highlightCard("YELLOW") }
        f.btnRedCard.setOnClickListener    { selectedCardType = "RED";    highlightCard("RED")    }

        f.spinnerPlayer.setupEmpty("Select Player")

        f.btnSave.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            val teamId   = foulTeamId
            val playerId = f.spinnerPlayer.selectedId()

            if (teamId   == null) { toast("Select Team");   return@setOnClickListener }
            if (playerId == null) { toast("Select Player"); return@setOnClickListener }

            isActionPending = true
            setScoringButtonsEnabled(false)
            val evType = when (selectedCardType) {
                "YELLOW" -> "YELLOW_CARD"
                "RED"    -> "RED_CARD"
                else     -> "FOUL"
            }
            sendEvent(JSONObject().apply {
                put("eventType", evType)
                put("teamId",    teamId)
                put("playerId",  playerId)
                if (selectedCardType != null) put("cardType", selectedCardType)
            })
            showPanel("scoring")
        }
    }

    private fun refreshFoulPlayerSpinner() {
        binding.foul.spinnerPlayer.setupWithPlayers("Select Player", getOnFieldPlayers(foulTeamId))
    }

    private fun highlightCard(selected: String) {
        binding.foul.btnSimpleFoul.alpha = if (selected == "FOUL")   1f else 0.4f
        binding.foul.btnYellowCard.alpha = if (selected == "YELLOW") 1f else 0.4f
        binding.foul.btnRedCard.alpha    = if (selected == "RED")    1f else 0.4f
    }

    private fun resetCardHighlight() {
        binding.foul.btnSimpleFoul.alpha = 1f
        binding.foul.btnYellowCard.alpha = 1f
        binding.foul.btnRedCard.alpha    = 1f
    }

    private fun setupSubPanel() {
        val s = binding.subsitute
        subTeamId = null

        s.tvClose.setOnClickListener { showPanel("scoring") }

        val teamNames = listOf("Select Team",
            matchResponse?.team1Name ?: "Team A",
            matchResponse?.team2Name ?: "Team B")
        val teamIds = listOf<Long?>(null, matchResponse?.team1Id, matchResponse?.team2Id)

        s.spinnerTeam.setup(teamNames) { pos ->
            subTeamId = teamIds[pos]
            refreshSubSpinners()
        }

        s.spinnerPlaying.setupEmpty("Select Player OUT")
        s.spinnerBenched.setupEmpty("Select Player IN")

        s.btnSubstitution.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            val teamId = subTeamId
            val outId  = s.spinnerPlaying.selectedId()
            val inId   = s.spinnerBenched.selectedId()

            if (teamId == null) { toast("Select Team");                          return@setOnClickListener }
            if (outId  == null) { toast("Select Out Player");                    return@setOnClickListener }
            if (inId   == null) { toast("Select In Player");                     return@setOnClickListener }
            if (outId  == inId) { toast("In and Out player should be different"); return@setOnClickListener }

            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().apply {
                put("eventType",   "SUBSTITUTION")
                put("teamId",      teamId)
                put("outPlayerId", outId)
                put("inPlayerId",  inId)
            })
            showPanel("scoring")
        }
    }

    private fun refreshSubSpinners() {
        val s = binding.subsitute
        s.spinnerPlaying.setupWithPlayers("Select Player Out", getOnFieldPlayers(subTeamId))
        s.spinnerBenched.setupWithPlayers("Select Player In",  getBenchPlayers(subTeamId))
    }

    private fun registerSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected    -> toast("Connected")
                    is SocketState.Error        -> {
                        toast("Socket Error")
                        isActionPending = false
                        if (_binding != null) setScoringButtonsEnabled(true)
                    }
                    is SocketState.Disconnected -> {
                        isActionPending = false
                        if (_binding != null) setScoringButtonsEnabled(true)
                    }
                }
            }
        }
        WebSocketManager.addMessageListener(SOCKET_KEY) { json ->
            activity?.runOnUiThread {
                try { handleServerUpdate(JSONObject(json)) }
                catch (e: Exception) {
                    e.printStackTrace()
                    isActionPending = false
                    if (_binding != null) setScoringButtonsEnabled(true)
                }
            }
        }
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }

    private fun handleServerUpdate(obj: JSONObject) {
        if (_binding == null) return
        val jsonMap = try {
            obj.keys().asSequence().associateWith { key -> obj.opt(key) }
        } catch (e: Exception) { null }

        if (jsonMap != null) {
            val milestone = MilestoneDetector.detectFutsalMilestone(jsonMap, prevDataSnapshot)
            prevDataSnapshot = jsonMap
            milestone?.let {
                activity?.runOnUiThread {
                    MilestoneDialog.show(childFragmentManager, it)
                }
            }
        }
        lastSocketJson = obj

        isActionPending = false
        setScoringButtonsEnabled(true)

        val t1 = obj.optInt("team1Score")
        val t2 = obj.optInt("team2Score")
        lastTeam1Score = t1
        lastTeam2Score = t2
        binding.score.text = "$t1 - $t2"

        val onField1 = obj.optJSONArray("team1OnField")
        val onField2 = obj.optJSONArray("team2OnField")
        if (onField1 != null && onField1.length() > 0)
            team1Active = (0 until onField1.length()).map { onField1.getJSONObject(it) }
                .mapNotNull { p ->
                    val id   = p.optInt("id", -1).takeIf { it != -1 }
                    val name = p.optString("name", "").takeIf { it.isNotBlank() }
                    if (id != null && name != null) Player(id, name, "") else null
                }
        if (onField2 != null && onField2.length() > 0)
            team2Active = (0 until onField2.length()).map { onField2.getJSONObject(it) }
                .mapNotNull { p ->
                    val id   = p.optInt("id", -1).takeIf { it != -1 }
                    val name = p.optString("name", "").takeIf { it.isNotBlank() }
                    if (id != null && name != null) Player(id, name, "") else null
                }

        currentHalf = obj.optInt("currentHalf", currentHalf)
        binding.tvPeriod.text = when (currentHalf) {
            1    -> "1st Half"
            2    -> "2nd Half"
            else -> "Extra Time"
        }

        currentStatus = obj.optString("status", currentStatus)

        when (currentStatus) {
            "HALF_TIME"                    -> { stopTimer(); toast("Half Time!") }
            "EXTRA_TIME"                   -> toast("Draw! Extra Time?")
            "COMPLETED", "MATCH_COMPLETE" -> stopTimer()
        }

        if (obj.optString("comment") == "UNDO") {
            eventsList.clear()
            eventsAdapter.notifyDataSetChanged()
        }

        val eventsArray = obj.optJSONArray("futsalEvents")
        if (eventsArray != null && eventsArray.length() > 0) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                parseAndAddEvent(eventsArray.getJSONObject(i))
            }
            binding.tvNoEvents.visibility = View.GONE
            eventsAdapter.notifyDataSetChanged()
        }

        if (obj.has("halfStartTime") && !obj.isNull("halfStartTime")) {
            val start = obj.getLong("halfStartTime")
            if (start > 0 && currentStatus != "HALF_TIME") {
                halfStartTime = start
                startTimer(start)
            }
        }

        updateEndHalfButtonText()

        val status = currentStatus.uppercase()

        if ((status == "COMPLETED" || status == "MATCH_COMPLETE") && isCompletedAndWaitingForData) {
            isCompletedAndWaitingForData = false
            loadAndShowVotingThenSummary()
            return
        }

        if (binding.layoutVoting.root.visibility == View.VISIBLE ||
            binding.layoutFutsalSummary.root.visibility == View.VISIBLE) {
            return
        }

        if ((status == "COMPLETED" || status == "MATCH_COMPLETE") && !votingAlreadyTriggered) {
            votingAlreadyTriggered = true
            loadAndShowVotingThenSummary()
        }
    }

    private fun parseAndAddEvent(obj: JSONObject) {
        val eventType = obj.optString("eventType", "").ifEmpty { return }
        if (eventType in listOf("END_HALF", "START_SECOND_HALF", "EXTRA_TIME")) return

        val eventId  = obj.optLong("id", System.currentTimeMillis())
        val teamId   = obj.optLong("teamId", -1L)
        val teamName = when (teamId) {
            matchResponse?.team1Id -> matchResponse?.team1Name ?: "Team A"
            matchResponse?.team2Id -> matchResponse?.team2Name ?: "Team B"
            else -> "Unknown"
        }

        val playerName = obj.optString("scorerName", "")
            .ifEmpty { obj.optString("playerName", "") }
            .ifEmpty { "Unknown Player" }

        val eventTimeSeconds = obj.optInt("eventTimeSeconds", 0)
            .takeIf { it > 0 } ?: (obj.optInt("minute", elapsedMinutes) * 60)

        val half = obj.optInt("half", currentHalf)

        val extra = when (eventType) {
            "GOAL", "OWN_GOAL" -> obj.optString("assistPlayerName", "")
            "SUBSTITUTION"     -> obj.optString("inPlayerName", "")
            "YELLOW_CARD"      -> "Yellow Card"
            "RED_CARD"         -> "Red Card"
            "FOUL"             -> "Foul"
            else               -> ""
        }

        val outPlayer = obj.optString("outPlayerName", "")

        eventsList.add(0, FutsalEventDTO(
            id               = eventId,
            eventType        = eventType,
            eventTimeSeconds = eventTimeSeconds,
            half             = half,
            scorerName       = playerName,
            assistPlayerName = extra,
            teamName         = teamName,
            outPlayerName    = outPlayer
        ))
    }

    private fun startTimer(startTime: Long) {
        stopTimer()
        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                elapsedMinutes = elapsed / 60
                binding.timer.text = String.format("%02d:%02d", elapsed / 60, elapsed % 60)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun fetchPlayers() {
        val t1 = matchResponse?.team1Id ?: return
        val t2 = matchResponse?.team2Id ?: return
        lifecycleScope.launch {
            try {
                val (r1, r2) = withContext(Dispatchers.IO) {
                    val d1 = async { RetrofitInstance.api.getPlayersByTeam(t1) }
                    val d2 = async { RetrofitInstance.api.getPlayersByTeam(t2) }
                    d1.await() to d2.await()
                }
                team1Players = r1.body().orEmpty().toScoringPlayers()
                team2Players = r2.body().orEmpty().toScoringPlayers()

                val playing1 = matchResponse?.team1PlayingIds?.map { it.toInt() }?.toSet()
                val playing2 = matchResponse?.team2PlayingIds?.map { it.toInt() }?.toSet()

                if (team1Active.isEmpty() && !playing1.isNullOrEmpty())
                    team1Active = team1Players.filter { it.id in playing1 }

                if (team2Active.isEmpty() && !playing2.isNullOrEmpty())
                    team2Active = team2Players.filter { it.id in playing2 }

            } catch (_: Exception) { toast("Failed to load players") }
        }
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

    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        binding.scoringTabContent.visibility = View.VISIBLE
        binding.eventsTabContent.visibility  = View.GONE

        val matchId   = matchResponse?.id ?: run { showFutsalSummary(); return }
        val accountId = getAccountId()

        if (hasAlreadyVoted(matchId)) {
            showFutsalSummary()
            return
        }

        binding.layoutProgressBar.visibility = View.VISIBLE

        val v = binding.layoutVoting
        v.tvVoteTeam1Name.text              = matchResponse?.team1Name ?: "Team 1"
        v.tvVoteTeam2Name.text              = matchResponse?.team2Name ?: "Team 2"
        selectedVotePlayerId                = null
        selectedVotePlayerName              = ""
        v.btnSubmitVote.isEnabled           = false
        v.layoutSelectedPlayerBanner.visibility = View.GONE
        v.etVoteFeedback.text?.clear()

        lifecycleScope.launch {
            try {
                val t1 = matchResponse?.team1Id ?: return@launch
                val t2 = matchResponse?.team2Id ?: return@launch
                val resp1 = withContext(Dispatchers.IO) { RetrofitInstance.api.getPlayersByTeam(t1) }
                val resp2 = withContext(Dispatchers.IO) { RetrofitInstance.api.getPlayersByTeam(t2) }
                val allPlayers1 = if (resp1.isSuccessful) resp1.body() ?: emptyList() else emptyList()
                val allPlayers2 = if (resp2.isSuccessful) resp2.body() ?: emptyList() else emptyList()

                // ── SQUAD FILTER ──────────────────────────────────────────
                val squadIds1 = matchResponse?.team1PlayingIds?.map { it }?.toSet() ?: emptySet()
                val squadIds2 = matchResponse?.team2PlayingIds?.map { it }?.toSet() ?: emptySet()

                val players1 = if (squadIds1.isNotEmpty())
                    allPlayers1.filter { it.id in squadIds1 } else allPlayers1
                val players2 = if (squadIds2.isNotEmpty())
                    allPlayers2.filter { it.id in squadIds2 } else allPlayers2

                val onPicked: (TeamPlayerDto, VotePlayerAdapter) -> Unit = { player, fromAdapter ->
                    if (fromAdapter === voteAdapter1) voteAdapter2?.clearSelection()
                    else                              voteAdapter1?.clearSelection()
                    selectedVotePlayerId   = player.id
                    selectedVotePlayerName = player.name ?: ""
                    v.tvSelectedVotePlayer.text = selectedVotePlayerName
                    v.layoutSelectedPlayerBanner.visibility = View.VISIBLE
                    v.btnSubmitVote.isEnabled = true
                }

                voteAdapter1 = VotePlayerAdapter(players1, onPicked)
                voteAdapter2 = VotePlayerAdapter(players2, onPicked)

                v.rvVoteTeam1.layoutManager = LinearLayoutManager(requireContext())
                v.rvVoteTeam1.adapter       = voteAdapter1
                v.rvVoteTeam2.layoutManager = LinearLayoutManager(requireContext())
                v.rvVoteTeam2.adapter       = voteAdapter2

            } catch (e: Exception) {
                toast("Could not load players: ${e.message}")
            } finally {
                binding.layoutProgressBar.visibility = View.GONE
                showPanel("voting")
            }
        }

        v.btnSubmitVote.setOnClickListener {
            val playerId = selectedVotePlayerId ?: return@setOnClickListener
            val feedback = v.etVoteFeedback.text?.toString()?.trim()
            submitVote(matchId, accountId, playerId, feedback)
        }
        v.btnSkipVote.setOnClickListener { showFutsalSummary() }
    }

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long, feedback: String? = null) {
        if (accountId == -1L) {
            toast("Account not found. Please login again.")
            showFutsalSummary()
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
            if (!feedback.isNullOrBlank()) put("feedback", feedback)
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { RetrofitInstance.api.submitVote(body) }
                when {
                    response.isSuccessful -> {
                        markAsVoted(matchId)
                        toast("Vote submitted!")
                        showFutsalSummary()
                    }
                    response.code() == 409 -> {
                        markAsVoted(matchId)
                        toast("Already voted!")
                        showFutsalSummary()
                    }
                    response.code() == 404 -> {
                        toast("Match or player not found.")
                        v.btnSubmitVote.isEnabled = true
                        v.btnSubmitVote.text      = "Submit & View Summary"
                        v.btnSkipVote.isEnabled   = true
                    }
                    else -> {
                        toast("Vote failed (${response.code()}). Try again.")
                        v.btnSubmitVote.isEnabled = true
                        v.btnSubmitVote.text      = "Submit & View Summary"
                        v.btnSkipVote.isEnabled   = true
                    }
                }
            } catch (e: Exception) {
                toast("Network error: ${e.message}")
                v.btnSubmitVote.isEnabled = true
                v.btnSubmitVote.text      = "Submit & View Summary"
                v.btnSkipVote.isEnabled   = true
            }
        }
    }

    private fun showFutsalSummary() {
        if (_binding == null || !isAdded) return
        showPanel("summary")

        val s       = binding.layoutFutsalSummary
        val t1Name  = matchResponse?.team1Name ?: "Team A"
        val t2Name  = matchResponse?.team2Name ?: "Team B"
        val t1Score = lastTeam1Score
        val t2Score = lastTeam2Score

        s.tvTeam1Name.text  = t1Name
        s.tvTeam2Name.text  = t2Name
        s.tvTeam1Score.text = t1Score.toString()
        s.tvTeam2Score.text = t2Score.toString()

        val (icon, result) = when {
            t1Score > t2Score -> "🏆" to " $t1Name Wins!"
            t2Score > t1Score -> "🏆" to " $t2Name Wins!"
            else              -> "🤝" to " Match Draw!"
        }
        s.tvMatchResult.text = "$icon $result"

        val team1Goals = eventsList.filter {
            it.eventType == "GOAL" && it.teamName.equals(t1Name, ignoreCase = true)
        }
        val team2Goals = eventsList.filter {
            it.eventType == "GOAL" && it.teamName.equals(t2Name, ignoreCase = true)
        }

        s.layoutTeam1Goals.removeAllViews()
        if (team1Goals.isEmpty()) addSummaryRow(s.layoutTeam1Goals, "No goals", "")
        else team1Goals.forEach { ev ->
            addSummaryRow(s.layoutTeam1Goals, ev.scorerName ?: "Unknown", "${ev.eventTimeSeconds / 60}'")
        }

        s.layoutTeam2Goals.removeAllViews()
        if (team2Goals.isEmpty()) addSummaryRow(s.layoutTeam2Goals, "No goals", "")
        else team2Goals.forEach { ev ->
            addSummaryRow(s.layoutTeam2Goals, ev.scorerName ?: "Unknown", "${ev.eventTimeSeconds / 60}'")
        }

        val t1Yellow = eventsList.count { it.eventType == "YELLOW_CARD" && it.teamName.equals(t1Name, ignoreCase = true) }
        val t2Yellow = eventsList.count { it.eventType == "YELLOW_CARD" && it.teamName.equals(t2Name, ignoreCase = true) }
        val t1Red    = eventsList.count { it.eventType == "RED_CARD"    && it.teamName.equals(t1Name, ignoreCase = true) }
        val t2Red    = eventsList.count { it.eventType == "RED_CARD"    && it.teamName.equals(t2Name, ignoreCase = true) }
        val t1Fouls  = eventsList.count { it.eventType == "FOUL"        && it.teamName.equals(t1Name, ignoreCase = true) }
        val t2Fouls  = eventsList.count { it.eventType == "FOUL"        && it.teamName.equals(t2Name, ignoreCase = true) }

        s.tvTeam1Stats.text = "🟨 $t1Yellow   🟥 $t1Red   ⚠️ $t1Fouls fouls"
        s.tvTeam2Stats.text = "🟨 $t2Yellow   🟥 $t2Red   ⚠️ $t2Fouls fouls"
    }

    private fun addSummaryRow(container: android.widget.LinearLayout, name: String, stat: String) {
        val row = layoutInflater.inflate(R.layout.item_performer_row, container, false)
        row.findViewById<TextView>(R.id.tvPlayerName).text = name
        row.findViewById<TextView>(R.id.tvPlayerStat).text = stat
        container.addView(row)
    }

    private fun showMediaDialog(eventId: Long?) {
        pendingEventId = eventId
        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_media_source, null)

        val etComment  = dialogView.findViewById<android.widget.EditText>(R.id.etMediaComment)
        val btnCamera  = dialogView.findViewById<View>(R.id.btnOpenCamera)
        val btnGallery = dialogView.findViewById<View>(R.id.btnOpenGallery)
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btnCancelMedia)
        val tvGallery  = dialogView.findViewById<TextView>(R.id.tvGalleryLabel)

        if (isUploading) tvGallery.text = "Uploading"

        btnCamera.setOnClickListener {
            pendingComment = etComment.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            dialog.dismiss()
            openCamera()
        }
        btnGallery.setOnClickListener {
            if (!isUploading) {
                pendingComment = etComment.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                dialog.dismiss()
                galleryLauncher.launch("image/*")
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun openCamera() {
        val imageFile = File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", imageFile)
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun uploadMediaFile(uri: Uri) {
        val matchId = matchResponse?.id ?: return
        val eventId = pendingEventId   ?: return
        isUploading = true
        val commentToSend = pendingComment
        toast("Uploading")

        lifecycleScope.launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val tempFile    = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                tempFile.outputStream().use { out -> inputStream?.copyTo(out) }

                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart    = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val matchIdBody = matchId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val eventIdBody = eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val commentBody = commentToSend?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.createMedia(matchIdBody, eventIdBody, filePart, commentBody)
                }

                if (response.isSuccessful) toast("Upload Successful!")
                else toast("Upload failed: ${response.code()}")

            } catch (e: Exception) {
                toast("Upload failed: ${e.message}")
            } finally {
                isUploading    = false
                pendingEventId = null
                pendingComment = null
            }
        }
    }

    private fun sendEvent(json: JSONObject) {
        json.put("matchId", matchResponse?.id ?: 0)
        WebSocketManager.send(json.toString())
    }

    private fun getActivePlayers(teamId: Long?) =
        if (teamId == matchResponse?.team1Id) team1Players else team2Players

    private fun getOpposingPlayers(teamId: Long?) =
        if (teamId == matchResponse?.team1Id) team2Players else team1Players

    private fun getOnFieldPlayers(teamId: Long?): List<Player> =
        if (teamId == matchResponse?.team1Id) team1Active else team2Active

    private fun getBenchPlayers(teamId: Long?): List<Player> {
        val onFieldIds = getOnFieldPlayers(teamId).map { it.id }.toSet()
        return getActivePlayers(teamId).filter { it.id !in onFieldIds }
    }

    private fun toast(msg: String) = requireContext().toastShort(msg)

    private fun Spinner.setup(items: List<String>, onSelect: (Int) -> Unit) {
        adapter = makeAdapter(items)
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = onSelect(pos)
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun Spinner.setupEmpty(placeholder: String) {
        adapter = makeAdapter(listOf(placeholder))
        tag     = listOf<Int?>(null)
    }

    private fun Spinner.setupWithPlayers(placeholder: String, players: List<Player>) {
        adapter = makeAdapter(listOf(placeholder) + players.map { it.name })
        tag     = listOf<Int?>(null) + players.map { it.id }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Spinner.selectedId(): Int? {
        val ids = tag as? List<Int?> ?: return null
        val pos = selectedItemPosition
        return if (pos in ids.indices) ids[pos] else null
    }

    private fun makeAdapter(items: List<String>): ArrayAdapter<String> {
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        return adapter
    }

    private fun List<TeamPlayerDto>.toScoringPlayers() = mapNotNull { dto ->
        val id   = dto.id?.toInt()
        val name = dto.name
        if (id == null || name.isNullOrBlank()) null else Player(id = id, name = name, status = "")
    }

    companion object {
        fun newInstance(match: MatchResponse) = FutsalScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}