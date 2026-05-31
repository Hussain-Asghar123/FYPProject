package com.example.fypproject.HockeyFragment

import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.HockeyEventsAdapter
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Dialog.MilestoneDialog
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.HockeyEventDTO
import com.example.fypproject.ScoringDTO.Player
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.MilestoneDetector
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.FragmentHockeyScoringBinding
import kotlinx.coroutines.*
import org.json.JSONObject

fun Fragment.toast(msg: String) {
    requireContext().toastShort(msg)
}

class HockeyScoringFragment : Fragment(R.layout.fragment_hockey_scoring) {
    private var _binding: FragmentHockeyScoringBinding? = null
    private val binding get() = _binding!!

    private var prevDataSnapshot: Map<String, Any?>? = null
    private var lastSocketJson: JSONObject? = null
    private var matchResponse: MatchResponse? = null
    private var team1Players = listOf<Player>()
    private var team2Players = listOf<Player>()
    private var team1Active = listOf<Player>()
    private var team2Active = listOf<Player>()
    private var currentStatus  = "LIVE"
    private var currentPeriod  = 1
    private var elapsedMinutes = 0
    private var canAddMedia = false
    private val SOCKET_KEY = "HockeyScoringFragment"
    private val handler        = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var periodStartTime  = 0L
    private val eventsList     = mutableListOf<HockeyEventDTO>()
    private lateinit var eventsAdapter: HockeyEventsAdapter
    private var goalTeamId:       Long?   = null
    private var foulTeamId:       Long?   = null
    private var subTeamId:        Long?   = null
    private var votingAlreadyTriggered = false

    private var selectedCardType: String? = null
    private var isActionPending = false
    private var canEdit = false

    private var lastTeam1Score = 0
    private var lastTeam2Score = 0

    private var isCompletedAndWaitingForData = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHockeyScoringBinding.bind(view)

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
            } else {
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
        canEdit = role.equals("ADMIN", true) || scorer.equals(username, true)
        canAddMedia = canEdit || mediaScorer.equals(username, true)
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
        eventsAdapter = HockeyEventsAdapter(eventsList) { event ->
            // no-op for now
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventsAdapter
    }

    private fun showPanel(panel: String) {
        binding.hockeyScoring.root.visibility       = View.GONE
        binding.goal.root.visibility                = View.GONE
        binding.foul.root.visibility                = View.GONE
        binding.subsitute.root.visibility           = View.GONE
        binding.layoutVoting.root.visibility        = View.GONE
        binding.layoutHockeySummary.root.visibility = View.GONE
        binding.layoutProgressBar.visibility        = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> { binding.hockeyScoring.root.visibility = View.VISIBLE; setupScoringPanel() }
            "goal"    -> { binding.goal.root.visibility = View.VISIBLE; setupGoalPanel() }
            "foul"    -> { binding.foul.root.visibility = View.VISIBLE; setupFoulPanel() }
            "sub"     -> { binding.subsitute.root.visibility = View.VISIBLE; setupSubPanel() }
            "voting"  -> binding.layoutVoting.root.visibility = View.VISIBLE
            "summary" -> binding.layoutHockeySummary.root.visibility = View.VISIBLE
            "loading" -> binding.layoutProgressBar.visibility = View.VISIBLE
        }
    }

    private fun setupScoringPanel() {
        val s = binding.hockeyScoring

        if (!canEdit) {
            s.btnGoal.visibility         = View.GONE
            s.btnFoul.visibility         = View.GONE
            s.btnSubstitution.visibility = View.GONE
            s.btnUndo.visibility         = View.GONE
            s.btnEndHalf.visibility      = View.GONE
            s.btnEditLineup.visibility   = View.GONE
            return
        }

        s.btnEditLineup.visibility = View.VISIBLE
        s.btnEditLineup.setOnClickListener { showLineupEditor() }

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
                "BREAK"              -> "START_NEXT_PERIOD"
                "EXTRA_TIME", "DRAW" -> "EXTRA_TIME"
                else                 -> "END_PERIOD"
            }
            sendEvent(JSONObject().apply { put("eventType", eventType) })
        }

        updatePeriodButtonText()
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
        val s     = binding.hockeyScoring
        val alpha = if (enabled) 1f else 0.45f
        listOf(s.btnGoal, s.btnFoul, s.btnSubstitution, s.btnUndo, s.btnEndHalf).forEach {
            it.isEnabled = enabled
            it.alpha     = alpha
        }
    }

    private fun updatePeriodButtonText() {
        if (_binding == null) return
        binding.hockeyScoring.btnEndHalf.text = when (currentStatus) {
            "BREAK"              -> "Start Next Period"
            "EXTRA_TIME", "DRAW" -> "Start Extra Time"
            else                 -> "End Period"
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
        selectedCardType = "FOUL"
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

        f.btnSimpleFoul.setOnClickListener { selectedCardType = "FOUL";   highlightCard("FOUL")   }
        f.btnGreenCard.setOnClickListener  { selectedCardType = "GREEN";  highlightCard("GREEN")  }
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
                "GREEN"  -> "GREEN_CARD"
                "YELLOW" -> "YELLOW_CARD"
                "RED"    -> "RED_CARD"
                else     -> "FOUL"
            }
            sendEvent(JSONObject().apply {
                put("eventType", evType)
                put("teamId",    teamId)
                put("playerId",  playerId)
                if (selectedCardType != "FOUL") put("cardType", selectedCardType)
            })
            showPanel("scoring")
        }
    }

    private fun refreshFoulPlayerSpinner() {
        binding.foul.spinnerPlayer.setupWithPlayers("Select Player", getOnFieldPlayers(foulTeamId))
    }

    private fun highlightCard(selected: String) {
        binding.foul.btnSimpleFoul.alpha = if (selected == "FOUL")   1f else 0.4f
        binding.foul.btnGreenCard.alpha  = if (selected == "GREEN")  1f else 0.4f
        binding.foul.btnYellowCard.alpha = if (selected == "YELLOW") 1f else 0.4f
        binding.foul.btnRedCard.alpha    = if (selected == "RED")    1f else 0.4f
    }

    private fun resetCardHighlight() {
        binding.foul.btnSimpleFoul.alpha = 1f
        binding.foul.btnGreenCard.alpha  = 1f
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
            // Hockey uses the Futsal milestone logic for basic goals/cards logic behind the scenes,
            // or we could add a specific detectHockeyMilestone. The prompt mentioned we could reuse it
            // or implement it. Let's use detectFutsalMilestone as they have identical structures.
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

        currentPeriod = obj.optInt("currentPeriod", currentPeriod)
        binding.tvPeriod.text = when (currentPeriod) {
            1    -> "1st Period"
            2    -> "2nd Period"
            3    -> "3rd Period"
            4    -> "4th Period"
            else -> "Extra Time"
        }

        currentStatus = obj.optString("status", currentStatus)

        when (currentStatus) {
            "BREAK"                       -> { stopTimer(); toast("Period Break!") }
            "EXTRA_TIME"                  -> toast("Draw! Extra Time?")
            "COMPLETED", "MATCH_COMPLETE" -> stopTimer()
        }

        if (obj.optString("comment") == "UNDO") {
            eventsList.clear()
            eventsAdapter.notifyDataSetChanged()
        }

        val eventsArray = obj.optJSONArray("hockeyEvents")
        if (eventsArray != null && eventsArray.length() > 0) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                parseAndAddEvent(eventsArray.getJSONObject(i))
            }
            binding.tvNoEvents.visibility = View.GONE
            eventsAdapter.notifyDataSetChanged()
        }

        if (obj.has("periodStartTime") && !obj.isNull("periodStartTime")) {
            val start = obj.getLong("periodStartTime")
            if (start > 0 && currentStatus != "BREAK") {
                periodStartTime = start
                startTimer(start)
            }
        }

        updatePeriodButtonText()

        val status = currentStatus.uppercase()

        if ((status == "COMPLETED" || status == "MATCH_COMPLETE") && isCompletedAndWaitingForData) {
            isCompletedAndWaitingForData = false
            loadAndShowVotingThenSummary()
            return
        }

        if (binding.layoutVoting.root.visibility == View.VISIBLE ||
            binding.layoutHockeySummary.root.visibility == View.VISIBLE) {
            return
        }

        if ((status == "COMPLETED" || status == "MATCH_COMPLETE") && !votingAlreadyTriggered) {
            votingAlreadyTriggered = true
            loadAndShowVotingThenSummary()
        }
    }

    private fun parseAndAddEvent(obj: JSONObject) {
        val eventType = obj.optString("eventType", "").ifEmpty { return }
        if (eventType in listOf("END_PERIOD", "START_NEXT_PERIOD", "EXTRA_TIME")) return

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

        val period = obj.optInt("period", currentPeriod)

        val extra = when (eventType) {
            "GOAL", "OWN_GOAL" -> obj.optString("assistPlayerName", "")
            "SUBSTITUTION"     -> obj.optString("inPlayerName", "")
            "GREEN_CARD"       -> "Green Card"
            "YELLOW_CARD"      -> "Yellow Card"
            "RED_CARD"         -> "Red Card"
            "FOUL"             -> "Foul"
            else               -> ""
        }

        val outPlayer = obj.optString("outPlayerName", "")

        eventsList.add(0, HockeyEventDTO(
            id               = eventId,
            eventType        = eventType,
            eventTimeSeconds = eventTimeSeconds,
            period           = period,
            scorerName       = playerName,
            assistPlayerName = extra,
            teamName         = teamName,
            outPlayerName    = outPlayer,
            goalType         = obj.optString("goalType", "")
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

    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        binding.scoringTabContent.visibility = View.VISIBLE
        binding.eventsTabContent.visibility  = View.GONE
        showHockeySummary()
    }

    private fun showHockeySummary() {
        if (_binding == null || !isAdded) return
        showPanel("summary")

        val s      = binding.layoutHockeySummary
        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"
        val t1Score = lastTeam1Score
        val t2Score = lastTeam2Score

        // ── Score & result ────────────────────────────────────────────
        s.tvHockeyTeam1Name.text  = t1Name
        s.tvHockeyTeam2Name.text  = t2Name
        s.tvHockeyTeam1Score.text = t1Score.toString()
        s.tvHockeyTeam2Score.text = t2Score.toString()

        val (icon, result) = when {
            t1Score > t2Score -> "🏆" to " $t1Name Wins!"
            t2Score > t1Score -> "🏆" to " $t2Name Wins!"
            else              -> "🤝" to " Match Draw!"
        }
        s.tvHockeyMatchResult.text = "$icon $result"

        // ── Team headers for goal lists ───────────────────────────────
        s.tvHockeyTeam1GoalsHeader.text = t1Name
        s.tvHockeyTeam2GoalsHeader.text = t2Name
        s.tvHockeyTeam1StatsHeader.text = t1Name
        s.tvHockeyTeam2StatsHeader.text = t2Name

        // ── Goal scorers ──────────────────────────────────────────────
        val t1Goals = eventsList.filter {
            it.eventType in listOf("GOAL", "OWN_GOAL") &&
            it.teamName.equals(t1Name, ignoreCase = true)
        }
        val t2Goals = eventsList.filter {
            it.eventType in listOf("GOAL", "OWN_GOAL") &&
            it.teamName.equals(t2Name, ignoreCase = true)
        }

        s.layoutHockeyTeam1Goals.removeAllViews()
        if (t1Goals.isEmpty()) addHockeySummaryRow(s.layoutHockeyTeam1Goals, "No goals", "")
        else t1Goals.forEach { ev ->
            val min  = ev.eventTimeSeconds / 60
            val label = if (ev.eventType == "OWN_GOAL") "${ev.scorerName ?: "?"} (OG)" else ev.scorerName ?: "?"
            addHockeySummaryRow(s.layoutHockeyTeam1Goals, label, "$min'")
        }

        s.layoutHockeyTeam2Goals.removeAllViews()
        if (t2Goals.isEmpty()) addHockeySummaryRow(s.layoutHockeyTeam2Goals, "No goals", "")
        else t2Goals.forEach { ev ->
            val min  = ev.eventTimeSeconds / 60
            val label = if (ev.eventType == "OWN_GOAL") "${ev.scorerName ?: "?"} (OG)" else ev.scorerName ?: "?"
            addHockeySummaryRow(s.layoutHockeyTeam2Goals, label, "$min'")
        }

        // ── Discipline counts ─────────────────────────────────────────
        fun count(eventType: String, teamName: String) =
            eventsList.count { it.eventType == eventType && it.teamName.equals(teamName, ignoreCase = true) }

        val t1Green  = count("GREEN_CARD",  t1Name)
        val t2Green  = count("GREEN_CARD",  t2Name)
        val t1Yellow = count("YELLOW_CARD", t1Name)
        val t2Yellow = count("YELLOW_CARD", t2Name)
        val t1Red    = count("RED_CARD",    t1Name)
        val t2Red    = count("RED_CARD",    t2Name)
        val t1Fouls  = count("FOUL",        t1Name)
        val t2Fouls  = count("FOUL",        t2Name)

        s.tvHockeyTeam1Green.text  = "$t1Green Green"
        s.tvHockeyTeam2Green.text  = "$t2Green Green"
        s.tvHockeyTeam1Yellow.text = "$t1Yellow Yellow"
        s.tvHockeyTeam2Yellow.text = "$t2Yellow Yellow"
        s.tvHockeyTeam1Red.text    = "$t1Red Red"
        s.tvHockeyTeam2Red.text    = "$t2Red Red"
        s.tvHockeyTeam1Fouls.text  = "$t1Fouls Fouls"
        s.tvHockeyTeam2Fouls.text  = "$t2Fouls Fouls"
    }

    private fun addHockeySummaryRow(container: android.widget.LinearLayout, name: String, stat: String) {
        val row = layoutInflater.inflate(R.layout.item_performer_row, container, false)
        row.findViewById<android.widget.TextView>(R.id.tvPlayerName).text = name
        row.findViewById<android.widget.TextView>(R.id.tvPlayerStat).text = stat
        container.addView(row)
    }

    private fun sendEvent(payload: JSONObject) {
        payload.put("matchId", matchResponse?.id ?: 0)
        WebSocketManager.send(payload.toString())
    }

    private fun getOnFieldPlayers(teamId: Long?): List<Player> = when (teamId) {
        matchResponse?.team1Id -> team1Active.ifEmpty { team1Players }
        matchResponse?.team2Id -> team2Active.ifEmpty { team2Players }
        else -> emptyList()
    }

    private fun getOpposingPlayers(teamId: Long?): List<Player> = when (teamId) {
        matchResponse?.team1Id -> team2Active.ifEmpty { team2Players }
        matchResponse?.team2Id -> team1Active.ifEmpty { team1Players }
        else -> emptyList()
    }

    private fun getBenchPlayers(teamId: Long?): List<Player> {
        val all = when (teamId) {
            matchResponse?.team1Id -> team1Players
            matchResponse?.team2Id -> team2Players
            else -> return emptyList()
        }
        val activeIds = getOnFieldPlayers(teamId).map { it.id }.toSet()
        return all.filter { it.id !in activeIds }
    }

    private fun Spinner.setupEmpty(prompt: String) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf(prompt))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        this.adapter = adapter
    }

    private fun Spinner.setup(items: List<String>, onItemSelected: (Int) -> Unit) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        this.adapter = adapter
        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) onItemSelected(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun Spinner.setupWithPlayers(prompt: String, players: List<Player>) {
        val names = listOf(prompt) + players.map { it.name }
        val ids   = listOf(null)   + players.map { it.id }
        this.tag = ids

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        this.adapter = adapter
    }

    private fun Spinner.selectedId(): Int? {
        val ids = this.tag as? List<Int?> ?: return null
        return ids.getOrNull(this.selectedItemPosition)
    }

    private fun List<com.example.fypproject.DTO.TeamPlayerDto>.toScoringPlayers() = map {
        Player(it.id?.toInt() ?: -1, it.name ?: "Unknown", "")
    }

    companion object {
        fun newInstance(matchResponse: MatchResponse) = HockeyScoringFragment().apply {
            arguments = Bundle().apply {
                putSerializable("match_response", matchResponse)
            }
        }
    }
}