package com.example.fypproject.VolleyBallFragment

import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.VolleyBallEventsAdapter
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.Player
import com.example.fypproject.ScoringDTO.VolleyballEvent
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.VolleyballScoringFragmentBinding
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Timer
import java.util.TimerTask

class VolleyBallScoringFragment : Fragment(R.layout.volleyball_scoring_fragment) {

    private var _binding: VolleyballScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    private var pendingEventId: Long? = null
    private var cameraImageUri: Uri?  = null
    private var isUploading           = false
    private val SOCKET_KEY = "VolleyBallScoringFragment"

    private var isActionPending = false

    private var team1Points   = 0
    private var team2Points   = 0
    private var team1Sets     = 0
    private var team2Sets     = 0
    private var currentSet    = 1
    private var team1Timeouts = 0
    private var team2Timeouts = 0
    private var setsToWin     = 3
    private var matchStatus   = "LIVE"
    private var setStartTimeMs = 0L
    private var lastTeam1Points = 0
    private var lastTeam2Points = 0

    private var timerEverStarted = false

    private var team1Players = listOf<Player>()
    private var team2Players = listOf<Player>()
    private var canEdit      = false

    private var isCompletedAndWaitingForData = false

    private var votingAlreadyTriggered = false
    private var selectedVotePlayerId: Long? = null
    private var selectedVotePlayerName = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null

    private val eventsList = mutableListOf<VolleyballEvent>()
    private lateinit var eventsAdapter: VolleyBallEventsAdapter

    private var timerTask: TimerTask? = null
    private val timer = Timer()

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { uploadMediaFile(it) } }
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraImageUri?.let { uploadMediaFile(it) } }

    private val pointTypes = listOf(
        "POINT"         to " Rally Point",
        "ACE"           to " Service Ace",
        "BLOCK"         to " Block",
        "ATTACK_ERROR"  to " Attack Error",
        "SERVICE_ERROR" to " Service Error"
    )

    private var selectedPointType: String? = null
    private var selectedPointTeamId: Long? = null
    private var subTeamId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = VolleyballScoringFragmentBinding.bind(view)

        getBundleData()
        computeCanEdit()
        setupEventsRecycler()
        setupBottomTabs()
        setupSocketConnection()
        fetchPlayers()
        showTab("scoring")

        val status = matchResponse?.status?.uppercase().orEmpty()
        if (status == "COMPLETED" || status == "MATCH_COMPLETE") {
            votingAlreadyTriggered = true
            showPanel("loading")
            fetchCompletedMatchDataThenVote()
        } else {
            showPanel("scoring")
        }
    }

    override fun onResume() {
        super.onResume()
        setupSocketListeners()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) setupSocketListeners()
        else unregisterSocketListeners()
    }


    override fun onPause() {
        super.onPause()
        unregisterSocketListeners()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        timerTask?.cancel()
        unregisterSocketListeners()
        _binding = null
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

    private fun computeCanEdit() {
        val prefs    = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val role     = prefs.getString("role", "")?.trim().orEmpty()
        val username = prefs.getString("username", "")?.trim().orEmpty()
        val scorer   = matchResponse?.scorerId?.trim().orEmpty()
        canEdit = role.equals("ADMIN", true) || scorer.equals(username, true)
    }

    private fun setupBottomTabs() {
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
        eventsAdapter = VolleyBallEventsAdapter(eventsList) { event ->
            showMediaDialog(event.id)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter       = eventsAdapter
    }

    private fun showPanel(panel: String) {
        binding.layoutScoring.root.visibility  = View.GONE
        binding.layoutPoint.root.visibility    = View.GONE
        binding.timeout.root.visibility        = View.GONE
        binding.subsitute.root.visibility      = View.GONE
        binding.layoutVoting.root.visibility   = View.GONE
        binding.summary.root.visibility        = View.GONE
        binding.layoutProgressBar.visibility   = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> { binding.layoutScoring.root.visibility = View.VISIBLE; setupScoringPanel() }
            "point"   -> { binding.layoutPoint.root.visibility   = View.VISIBLE; setupPointPanel()   }
            "timeout" -> { binding.timeout.root.visibility       = View.VISIBLE; setupTimeoutPanel() }
            "sub"     -> { binding.subsitute.root.visibility     = View.VISIBLE; setupSubPanel()     }
            "voting"  ->   binding.layoutVoting.root.visibility  = View.VISIBLE
            "summary" ->   binding.summary.root.visibility       = View.VISIBLE
            "loading" ->   binding.layoutProgressBar.visibility  = View.VISIBLE
        }
    }

    private fun setScoringButtonsEnabled(enabled: Boolean) {
        if (_binding == null) return
        val s     = binding.layoutScoring
        val alpha = if (enabled) 1f else 0.45f
        listOf(s.btnPoint, s.btnTimeOut, s.btnSubstitution, s.btnEndSet, s.btnUndo).forEach {
            it.isEnabled = enabled
            it.alpha     = alpha
        }
    }

    private fun setupScoringPanel() {
        updateScoreUI()
        updateSetCircles()

        if (!canEdit) {
            binding.layoutScoring.btnPoint.visibility        = View.GONE
            binding.layoutScoring.btnTimeOut.visibility      = View.GONE
            binding.layoutScoring.btnEndSet.visibility       = View.GONE
            binding.layoutScoring.btnSubstitution.visibility = View.GONE
            binding.layoutScoring.btnUndo.visibility         = View.GONE
            return
        }

        binding.layoutScoring.btnPoint.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            selectedPointType   = null
            selectedPointTeamId = null
            showPanel("point")
        }

        binding.layoutScoring.btnTimeOut.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            showPanel("timeout")
        }

        binding.layoutScoring.btnSubstitution.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            showPanel("sub")
        }

        binding.layoutScoring.btnEndSet.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "END_SET"))
        }

        binding.layoutScoring.btnUndo.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "UNDO"))
        }

        setScoringButtonsEnabled(!isActionPending)
    }

    private fun setupPointPanel() {
        val p = binding.layoutPoint
        p.tvClose.setOnClickListener { showPanel("scoring") }

        selectedPointType   = null
        selectedPointTeamId = null

        val typeNames = listOf("Select Point Type") + pointTypes.map { it.second }
        p.goalTypeSpinner.setup(typeNames) { pos ->
            selectedPointType = if (pos > 0) pointTypes[pos - 1].first else null
        }

        val teamNames = listOf(
            "Select Team",
            matchResponse?.team1Name ?: "Team A",
            matchResponse?.team2Name ?: "Team B"
        )
        val teamIds = listOf<Long?>(null, matchResponse?.team1Id, matchResponse?.team2Id)
        p.spinnerTeam.setup(teamNames) { pos ->
            selectedPointTeamId = teamIds[pos]
            val players = if (selectedPointTeamId == matchResponse?.team1Id)
                team1Players else team2Players
            refreshPlayerSpinner(players)
        }

        refreshPlayerSpinner(emptyList())

        p.btnSave.setOnClickListener {
            if (isActionPending) return@setOnClickListener

            val type   = selectedPointType
            val teamId = selectedPointTeamId
            if (type == null)   { toast("Select Point Type"); return@setOnClickListener }
            if (teamId == null) { toast("Select Team");       return@setOnClickListener }

            isActionPending = true

            val json = JSONObject()
                .put("eventType", type)
                .put("teamId", teamId)

            val playerId = getSelectedPlayerId()
            if (playerId != null) json.put("playerId", playerId)

            sendEvent(json)
            showPanel("scoring")
        }
    }

    private fun refreshPlayerSpinner(players: List<Player>) {
        binding.layoutPoint.spinnerPlayer.setupWithPlayers(
            "Select Player (Optional)", players
        )
    }

    private fun getSelectedPlayerId(): Int? {
        val spinner = binding.layoutPoint.spinnerPlayer
        @Suppress("UNCHECKED_CAST")
        val ids = spinner.tag as? List<Int?> ?: return null
        val pos = spinner.selectedItemPosition
        return if (pos in ids.indices) ids[pos] else null
    }

    private fun setupTimeoutPanel() {
        val t = binding.timeout

        t.btnTeamA.text      = matchResponse?.team1Name ?: "Team A"
        t.btnTeamB.text      = matchResponse?.team2Name ?: "Team B"
        t.btnTeamA.isEnabled = team1Timeouts < 2
        t.btnTeamB.isEnabled = team2Timeouts < 2
        t.btnTeamA.alpha     = if (team1Timeouts < 2) 1f else 0.4f
        t.btnTeamB.alpha     = if (team2Timeouts < 2) 1f else 0.4f

        t.tvClose.setOnClickListener { showPanel("scoring") }

        t.btnTeamA.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending     = true
            t.btnTeamA.isEnabled = false
            t.btnTeamB.isEnabled = false
            sendEvent(JSONObject()
                .put("eventType", "TIMEOUT")
                .put("teamId", matchResponse?.team1Id))
            showPanel("scoring")
        }

        t.btnTeamB.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending     = true
            t.btnTeamA.isEnabled = false
            t.btnTeamB.isEnabled = false
            sendEvent(JSONObject()
                .put("eventType", "TIMEOUT")
                .put("teamId", matchResponse?.team2Id))
            showPanel("scoring")
        }
    }

    private fun setupSubPanel() {
        val s = binding.subsitute
        subTeamId = null
        s.tvClose.setOnClickListener { showPanel("scoring") }

        val teamNames = listOf(
            "Select Team",
            matchResponse?.team1Name ?: "Team A",
            matchResponse?.team2Name ?: "Team B"
        )
        val teamIds = listOf<Long?>(null, matchResponse?.team1Id, matchResponse?.team2Id)

        s.spinnerTeam.setup(teamNames) { pos ->
            subTeamId = teamIds[pos]
            val players = if (subTeamId == matchResponse?.team1Id) team1Players else team2Players
            s.spinnerPlaying.setupWithPlayers("Select Player OUT", players)
            s.spinnerBenched.setupWithPlayers("Select Player IN",  players)
        }

        s.spinnerPlaying.setupEmpty("Select Player OUT")
        s.spinnerBenched.setupEmpty("Select Player IN")

        s.btnSubstitution.setOnClickListener {
            if (isActionPending) return@setOnClickListener

            val teamId = subTeamId
            val outId  = s.spinnerPlaying.selectedId()
            val inId   = s.spinnerBenched.selectedId()

            if (teamId == null) { toast("Select Team");       return@setOnClickListener }
            if (outId  == null) { toast("Select Out Player"); return@setOnClickListener }
            if (inId   == null) { toast("Select In Player");  return@setOnClickListener }
            if (outId  == inId) { toast("In and Out player should be different"); return@setOnClickListener }

            isActionPending = true

            sendEvent(JSONObject()
                .put("eventType",   "SUBSTITUTION")
                .put("teamId",      teamId)
                .put("outPlayerId", outId)
                .put("inPlayerId",  inId))
            showPanel("scoring")
        }
    }

    private fun updateScoreUI() {
        if (_binding == null) return
        binding.score.text = "$team1Points - $team2Points"

        val maxSets     = setsToWin * 2 - 1
        val totalPlayed = team1Sets + team2Sets
        val setLabel    = if (totalPlayed == maxSets - 1) "Tiebreak" else "Set $currentSet"
        binding.tvSetValue.text = setLabel
    }

    private fun updateSetCircles() {
        if (_binding == null) return
        drawCircles(binding.layoutTeamASetIndicators, team1Sets,
            android.graphics.Color.parseColor("#3B82F6"))
        drawCircles(binding.layoutTeamBSetIndicators, team2Sets,
            android.graphics.Color.parseColor("#F43F5E"))
    }

    private fun drawCircles(container: LinearLayout?, setsWon: Int, filledColor: Int) {
        container ?: return
        container.removeAllViews()
        val size   = (16 * resources.displayMetrics.density).toInt()
        val margin = (6  * resources.displayMetrics.density).toInt()
        val empty  = android.graphics.Color.parseColor("#E5E7EB")
        val border = android.graphics.Color.parseColor("#D1D5DB")

        for (i in 0 until setsToWin) {
            val circle = View(requireContext())
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(margin / 2, 0, margin / 2, 0)
            circle.layoutParams = lp

            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            if (i < setsWon) drawable.setColor(filledColor)
            else             { drawable.setColor(empty); drawable.setStroke(2, border) }
            circle.background = drawable
            container.addView(circle)
        }
    }

    private fun startSetTimer(startTime: Long) {
        timerTask?.cancel()
        setStartTimeMs = startTime
        timerEverStarted = true

        activity?.runOnUiThread {
            if (_binding != null) {
                binding.tvSetTimer.visibility = View.VISIBLE
            }
        }

        timerTask = object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (_binding == null || setStartTimeMs == 0L) return@runOnUiThread
                    val elapsed = (System.currentTimeMillis() - setStartTimeMs) / 1000
                    val mins = String.format("%02d", elapsed / 60)
                    val secs = String.format("%02d", elapsed % 60)
                    binding.tvSetTimer.text = "$mins:$secs"
                }
            }
        }
        timer.scheduleAtFixedRate(timerTask, 0, 1000)
    }

    private fun setupSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected -> toast("Connected")
                    is SocketState.Error -> {
                        toast("Socket Error")
                        isActionPending = false
                        if (_binding != null && binding.layoutScoring.root.visibility == View.VISIBLE)
                            setScoringButtonsEnabled(true)
                    }
                    is SocketState.Disconnected -> {
                        isActionPending = false
                        if (_binding != null && binding.layoutScoring.root.visibility == View.VISIBLE)
                            setScoringButtonsEnabled(true)
                    }
                }
            }
        }
        WebSocketManager.addMessageListener(SOCKET_KEY) { jsonString ->
            android.util.Log.d("VB_SOCKET", "Raw: $jsonString")
            activity?.runOnUiThread {
                try {
                    handleServerUpdate(JSONObject(jsonString))
                } catch (e: Exception) {
                    android.util.Log.e("VB_SOCKET", "Parse error: ${e.message}")
                    isActionPending = false
                    if (_binding != null) setScoringButtonsEnabled(true)
                }
            }
        }
    }

    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }

    private fun handleServerUpdate(obj: JSONObject) {
        if (_binding == null) return

        isActionPending = false

        if (isCompletedAndWaitingForData) {
            isCompletedAndWaitingForData = false
        }

        team1Points   = obj.optInt("team1Score",    team1Points)
        team2Points   = obj.optInt("team2Score",    team2Points)
        if (team1Points > 0 || team2Points > 0) {
            lastTeam1Points = team1Points
            lastTeam2Points = team2Points
        }
        team1Sets     = obj.optInt("team1Sets",     team1Sets)
        team2Sets     = obj.optInt("team2Sets",     team2Sets)
        currentSet    = obj.optInt("currentSet",    currentSet)
        setsToWin     = obj.optInt("setsToWin",     setsToWin)
        team1Timeouts = obj.optInt("team1Timeouts", team1Timeouts)
        team2Timeouts = obj.optInt("team2Timeouts", team2Timeouts)

        val rawStatus = obj.optString("status", "")
        if (rawStatus.isNotEmpty() && rawStatus != "null") matchStatus = rawStatus

        val eventsArray = obj.optJSONArray("volleyballEvents")
        if (eventsArray != null) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                parseVolleyballEvent(eventsArray.getJSONObject(i))
            }
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }
        if (obj.has("setStartTime") && !obj.isNull("setStartTime")) {
            val start = obj.getLong("setStartTime")
            if (start > 0) {
                if (start != setStartTimeMs || !timerEverStarted) {
                    startSetTimer(start)
                }
            }
        }
        if (obj.optString("comment") == "UNDO") toast("Undo successful")

        updateScoreUI()
        updateSetCircles()

        if (binding.layoutScoring.root.visibility == View.VISIBLE) {
            setScoringButtonsEnabled(true)
        }

        val status = matchStatus.uppercase()

        if ((status == "COMPLETED" || status == "MATCH_COMPLETE") &&
            votingAlreadyTriggered &&
            binding.layoutProgressBar.visibility == View.VISIBLE) {
            loadAndShowVotingThenSummary()
            return
        }

        if ((status == "COMPLETED" || status == "MATCH_COMPLETE") && !votingAlreadyTriggered) {
            votingAlreadyTriggered = true
            timerTask?.cancel()
            loadAndShowVotingThenSummary()
        }
    }

    private fun parseVolleyballEvent(obj: JSONObject) {
        val eventType = obj.optString("eventType", "").ifEmpty { return }

        if (eventType in listOf("END_SET", "START_SET")) return

        val eventId  = obj.optLong("id", System.currentTimeMillis())
        val teamId   = obj.optLong("teamId", -1L)
        val teamName = when (teamId) {
            matchResponse?.team1Id -> matchResponse?.team1Name ?: "Team A"
            matchResponse?.team2Id -> matchResponse?.team2Name ?: "Team B"
            else -> "Unknown"
        }

        fun JSONObject.safeString(key: String) =
            if (isNull(key)) "" else optString(key, "").let { if (it == "null") "" else it }

        val playerName = obj.safeString("playerName")
            .ifEmpty { obj.safeString("scorerName") }
            .ifEmpty { "" }

        val eventTimeSeconds = obj.optInt("eventTimeSeconds", 0)
            .takeIf { it > 0 } ?: (obj.optInt("minute", 0) * 60)

        val setNum = obj.optInt("setNumber", currentSet)

        eventsList.add(0, VolleyballEvent(
            id               = eventId,
            eventType        = eventType,
            eventTimeSeconds = eventTimeSeconds,
            setNumber        = setNum,
            playerName       = playerName,
            teamName         = teamName
        ))
    }

    private fun setupSocketConnection() {
        setupSocketListeners()
    }

    private fun sendEvent(json: JSONObject) {
        json.put("matchId", matchResponse?.id ?: 0)
        WebSocketManager.send(json.toString())
    }

     private fun fetchCompletedMatchDataThenVote() {
        if (_binding == null || !isAdded) return

        isCompletedAndWaitingForData = true

        lifecycleScope.launch {
            delay(5000)
            if (_binding != null && isCompletedAndWaitingForData) {
                isCompletedAndWaitingForData = false
                loadAndShowVotingThenSummary()
            }
        }
    }

    private fun showVolleyBallSummary() {
        if (_binding == null || !isAdded) return
        showPanel("summary")

        val s      = binding.summary
        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"

        s.tvTeam1Name.text  = t1Name
        s.tvTeam2Name.text  = t2Name

        s.tvTeam1Score.text = team1Sets.toString()
        s.tvTeam2Score.text = team2Sets.toString()

        s.tvMatchResult.text = when {
            team1Sets > team2Sets -> " $t1Name Wins!"
            team2Sets > team1Sets -> " $t2Name Wins!"
            else                  -> " Match Draw!"
        }

        s.tvTeam1Timeouts.text = team1Timeouts.toString()
        s.tvTeam2Timeouts.text = team2Timeouts.toString()
    }

    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        val matchId   = matchResponse?.id ?: run { showVolleyBallSummary(); return }
        val accountId = getAccountId()
        if (hasAlreadyVoted(matchId)) { showVolleyBallSummary(); return }

        binding.layoutProgressBar.visibility = View.VISIBLE
        val v = binding.layoutVoting
        v.tvVoteTeam1Name.text                  = matchResponse?.team1Name ?: "Team 1"
        v.tvVoteTeam2Name.text                  = matchResponse?.team2Name ?: "Team 2"
        selectedVotePlayerId                    = null
        v.btnSubmitVote.isEnabled               = false
        v.layoutSelectedPlayerBanner.visibility = View.GONE
        v.etVoteFeedback.text?.clear()

        lifecycleScope.launch {
            try {
                val t1 = matchResponse?.team1Id ?: return@launch
                val t2 = matchResponse?.team2Id ?: return@launch
                val resp1 = withContext(Dispatchers.IO) { RetrofitInstance.api.getPlayersByTeam(t1) }
                val resp2 = withContext(Dispatchers.IO) { RetrofitInstance.api.getPlayersByTeam(t2) }
                val players1 = if (resp1.isSuccessful) resp1.body() ?: emptyList() else emptyList()
                val players2 = if (resp2.isSuccessful) resp2.body() ?: emptyList() else emptyList()

                val onPicked: (TeamPlayerDto, VotePlayerAdapter) -> Unit = { player, fromAdapter ->
                    if (fromAdapter === voteAdapter1) voteAdapter2?.clearSelection()
                    else                              voteAdapter1?.clearSelection()
                    selectedVotePlayerId            = player.id
                    selectedVotePlayerName          = player.name ?: ""
                    v.tvSelectedVotePlayer.text             = selectedVotePlayerName
                    v.layoutSelectedPlayerBanner.visibility = View.VISIBLE
                    v.btnSubmitVote.isEnabled               = true
                }
                voteAdapter1 = VotePlayerAdapter(players1, onPicked)
                voteAdapter2 = VotePlayerAdapter(players2, onPicked)
                v.rvVoteTeam1.layoutManager = LinearLayoutManager(requireContext())
                v.rvVoteTeam1.adapter       = voteAdapter1
                v.rvVoteTeam2.layoutManager = LinearLayoutManager(requireContext())
                v.rvVoteTeam2.adapter       = voteAdapter2
            } catch (_: Exception) {
                toast("Could not load players")
            } finally {
                binding.layoutProgressBar.visibility = View.GONE
                showPanel("voting")
            }
        }

        v.btnSubmitVote.setOnClickListener {
            val playerId = selectedVotePlayerId ?: return@setOnClickListener
            submitVote(matchId, accountId, playerId)
        }
        v.btnSkipVote.setOnClickListener { showVolleyBallSummary() }
    }

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long) {
        if (accountId == -1L) { toast("Login again"); showVolleyBallSummary(); return }
        val v = binding.layoutVoting
        v.btnSubmitVote.isEnabled = false
        v.btnSubmitVote.text      = "Submitting…"
        v.btnSkipVote.isEnabled   = false
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.submitVote(matchId, accountId, playerId)
                }
                when {
                    response.isSuccessful  -> { markAsVoted(matchId); toast("Vote submitted!"); showVolleyBallSummary() }
                    response.code() == 409 -> { markAsVoted(matchId); toast("Already voted!");  showVolleyBallSummary() }
                    else -> {
                        toast("Vote failed")
                        v.btnSubmitVote.isEnabled = true
                        v.btnSubmitVote.text      = "Submit & View Summary"
                        v.btnSkipVote.isEnabled   = true
                    }
                }
            } catch (_: Exception) {
                toast("Network error")
                v.btnSubmitVote.isEnabled = true
                v.btnSubmitVote.text      = "Submit & View Summary"
                v.btnSkipVote.isEnabled   = true
            }
        }
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
            } catch (_: Exception) { toast("Failed to load players") }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun showMediaDialog(eventId: Long?) {
        pendingEventId = eventId
        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_media_source, null)
        val btnCamera  = dialogView.findViewById<View>(R.id.btnOpenCamera)
        val btnGallery = dialogView.findViewById<View>(R.id.btnOpenGallery)
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btnCancelMedia)
        val tvGallery  = dialogView.findViewById<TextView>(R.id.tvGalleryLabel)
        if (isUploading) tvGallery.text = "Uploading"
        btnCamera.setOnClickListener  { dialog.dismiss(); openCamera() }
        btnGallery.setOnClickListener { if (!isUploading) { dialog.dismiss(); galleryLauncher.launch("image/*") } }
        btnCancel.setOnClickListener  { dialog.dismiss() }
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
                val response    = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.createMedia(matchIdBody, eventIdBody, filePart)
                }
                if (response.isSuccessful) toast("Upload Successful!")
                else toast("Upload failed: ${response.code()}")
            } catch (e: Exception) {
                toast("Upload failed: ${e.message}")
            } finally {
                isUploading    = false
                pendingEventId = null
            }
        }
    }

    private fun hasAlreadyVoted(matchId: Long) =
        requireActivity().getSharedPreferences("VotePrefs", MODE_PRIVATE)
            .getBoolean("voted_match_$matchId", false)

    private fun markAsVoted(matchId: Long) =
        requireActivity().getSharedPreferences("VotePrefs", MODE_PRIVATE)
            .edit().putBoolean("voted_match_$matchId", true).apply()

    private fun getAccountId() =
        requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getLong("id", -1L)

    private fun toast(msg: String) = context?.toastShort(msg)

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

    private fun makeAdapter(items: List<String>) =
        ArrayAdapter(requireContext(), R.layout.spinner_item, items).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

    private fun List<TeamPlayerDto>.toScoringPlayers() = mapNotNull { dto ->
        val id   = dto.id?.toInt()
        val name = dto.name
        if (id == null || name.isNullOrBlank()) null else Player(id = id, name = name,status="")
    }

    companion object {
        fun newInstance(match: MatchResponse) = VolleyBallScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}