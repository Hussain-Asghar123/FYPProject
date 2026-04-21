package com.example.fypproject.TableTennisFragment

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
import com.example.fypproject.Adapter.TableTennisEventAdapter
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.Player
import com.example.fypproject.ScoringDTO.TableTennisEvent
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.TabletennisScoringFragmentBinding
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Timer
import java.util.TimerTask

class TableTennisScoringFragment : Fragment(R.layout.tabletennis_scoring_fragment) {

    private var _binding: TabletennisScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    private var pendingEventId: Long? = null
    private var cameraImageUri: Uri?  = null
    private var isUploading           = false
    private val SOCKET_KEY = "TableTennisScoringFragment"

    private var team1Points    = 0
    private var team2Points    = 0
    private var team1Games     = 0
    private var team2Games     = 0
    private var currentGame    = 1
    private var gamesToWin     = 4
    private var pointsPerGame = 11
    private var matchStatus    = "LIVE"
    private var gameStartTimeMs = 0L
    private var isActionPending = false
    private var timerEverStarted = false

    private var team1Players = listOf<Player>()
    private var team2Players = listOf<Player>()
    private var canEdit      = false

    private var votingAlreadyTriggered = false
    private var selectedVotePlayerId: Long? = null
    private var selectedVotePlayerName = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null

    private val eventsList = mutableListOf<TableTennisEvent>()
    private lateinit var eventsAdapter: TableTennisEventAdapter

    private var timerTask: TimerTask? = null
    private val timer = Timer()

    private val scoreTypes = listOf(
        "POINT"       to "🏓 Rally Point",
        "SMASH"       to "💥 Smash",
        "SERVICE_ACE" to "🎯 Service Ace",
        "EDGE_BALL"   to "🎱 Edge Ball"
    )

    private val faultTypes = listOf(
        "NET_FAULT"     to "🔴 Net Fault",
        "OUT"           to "⚡ Ball Out",
        "SERVICE_FAULT" to "🟠 Service Fault"
    )

    private var selectedPointType: String?  = null
    private var selectedPointTeamId: Long?  = null
    private var selectedFoulType: String?   = null
    private var selectedFoulTeamId: Long?   = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { uploadMediaFile(it) } }
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraImageUri?.let { uploadMediaFile(it) } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = TabletennisScoringFragmentBinding.bind(view)

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
            loadAndShowVotingThenSummary()
        } else {
            showPanel("scoring")
        }
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
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }
    }

    private fun setupEventsRecycler() {
        eventsAdapter = TableTennisEventAdapter(eventsList) { event ->
            showMediaDialog(event.id)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter       = eventsAdapter
    }
    private fun showPanel(panel: String) {
        binding.futsalScoring.root.visibility = View.GONE
        binding.goal.root.visibility = View.GONE
        binding.foul.root.visibility = View.GONE
        binding.layoutVoting.root.visibility = View.GONE
        binding.layoutFutsalSummary.root.visibility = View.GONE
        binding.layoutProgressBar.visibility = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> {
                binding.futsalScoring.root.visibility = View.VISIBLE; setupScoringPanel()
            }

            "point" -> {
                binding.goal.root.visibility = View.VISIBLE; setupPointPanel()
            }

            "foul" -> {
                binding.foul.root.visibility = View.VISIBLE; setupFoulPanel()
            }

            "voting" -> binding.layoutVoting.root.visibility = View.VISIBLE
            "summary" -> binding.layoutFutsalSummary.root.visibility = View.VISIBLE
            "loading" -> binding.layoutProgressBar.visibility = View.VISIBLE
        }
    }

    private fun setupScoringPanel() {
        updateScoreUI()
        updateGameCircles()

        if (!canEdit) {
            binding.futsalScoring.btnGoal.visibility    = View.GONE
            binding.futsalScoring.btnFoul.visibility    = View.GONE
            binding.futsalScoring.btnEndGame.visibility = View.GONE
            binding.futsalScoring.btnUndo.visibility    = View.GONE
            return
        }

        binding.futsalScoring.btnGoal.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            selectedPointType   = null
            selectedPointTeamId = null
            showPanel("point")
        }

        binding.futsalScoring.btnFoul.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            selectedFoulType   = null
            selectedFoulTeamId = null
            showPanel("foul")
        }

        binding.futsalScoring.btnEndGame.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "END_GAME"))
        }

        binding.futsalScoring.btnUndo.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("undo", true))
        }

        setScoringButtonsEnabled(!isActionPending)
    }

    private fun setScoringButtonsEnabled(enabled: Boolean) {
        if (_binding == null) return
        val alpha = if (enabled) 1f else 0.45f
        listOf(
            binding.futsalScoring.btnGoal,
            binding.futsalScoring.btnFoul,
            binding.futsalScoring.btnEndGame,
            binding.futsalScoring.btnUndo
        ).forEach {
            it.isEnabled = enabled
            it.alpha     = alpha
        }
    }

    private fun setupPointPanel() {
        val p = binding.goal
        p.tvClose.setOnClickListener { showPanel("scoring") }
        selectedPointType   = null
        selectedPointTeamId = null

        val typeNames = listOf("Select Point Type") + scoreTypes.map { it.second }
        p.goalTypeSpinner.setup(typeNames) { pos ->
            selectedPointType = if (pos > 0) scoreTypes[pos - 1].first else null
        }

        val teamNames = listOf("Select Team",
            matchResponse?.team1Name ?: "Team A",
            matchResponse?.team2Name ?: "Team B")
        val teamIds = listOf<Long?>(null, matchResponse?.team1Id, matchResponse?.team2Id)
        p.spinnerTeam.setup(teamNames) { pos ->
            selectedPointTeamId = teamIds[pos]
            val players = if (selectedPointTeamId == matchResponse?.team1Id)
                team1Players else team2Players
            p.spinnerPlayer.setupWithPlayers("Select Player (Optional)", players)
        }
        p.spinnerPlayer.setupEmpty("Select Player (Optional)")

        p.btnSave.setOnClickListener {
            val type   = selectedPointType
            val teamId = selectedPointTeamId
            if (type == null)    { toast("Select Point Type"); return@setOnClickListener }
            if (teamId == null)  { toast("Select Team");       return@setOnClickListener }
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            val json = JSONObject().put("eventType", type).put("teamId", teamId)
            val playerId = p.spinnerPlayer.selectedId()
            if (playerId != null) json.put("playerId", playerId)
            sendEvent(json)
            showPanel("scoring")
        }
    }

    private fun setupFoulPanel() {
        val f = binding.foul
        f.tvClose.setOnClickListener { showPanel("scoring") }
        selectedFoulType   = null
        selectedFoulTeamId = null

        val faultNames = listOf("Select Fault Type") + faultTypes.map { it.second }
        f.goalTypeSpinner.setup(faultNames) { pos ->
            selectedFoulType = if (pos > 0) faultTypes[pos - 1].first else null
        }

        val teamNames = listOf("Select Team",
            matchResponse?.team1Name ?: "Team A",
            matchResponse?.team2Name ?: "Team B")
        val teamIds = listOf<Long?>(null, matchResponse?.team1Id, matchResponse?.team2Id)
        f.spinnerTeam.setup(teamNames) { pos ->
            selectedFoulTeamId = teamIds[pos]
            val players = if (selectedFoulTeamId == matchResponse?.team1Id)
                team1Players else team2Players
            f.spinnerPlayer.setupWithPlayers("Select Player (Optional)", players)
        }
        f.spinnerPlayer.setupEmpty("Select Player (Optional)")

        f.btnSave.setOnClickListener {
            val type   = selectedFoulType
            val teamId = selectedFoulTeamId
            if (type == null)    { toast("Select Fault Type"); return@setOnClickListener }
            if (teamId == null)  { toast("Select Team");       return@setOnClickListener }
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            val json = JSONObject().put("eventType", type).put("teamId", teamId)
            val playerId = f.spinnerPlayer.selectedId()
            if (playerId != null) json.put("playerId", playerId)
            sendEvent(json)
            showPanel("scoring")
        }
    }

    private fun updateScoreUI() {
        if (_binding == null) return
        binding.score.text = "$team1Points - $team2Points"

        val maxGames   = gamesToWin * 2 - 1
        val totalGames = team1Games + team2Games
        val gameLabel  = if (totalGames == maxGames - 1) "Decider" else "Game $currentGame"
        binding.tvPeriod.text = gameLabel

        val isDeuce = team1Points >= pointsPerGame - 1 && team2Points >= pointsPerGame - 1
        binding.tvDeuce.visibility = if (isDeuce) View.VISIBLE else View.GONE
    }

    private fun updateGameCircles() {
        if (_binding == null) return
        drawCircles(binding.layoutTeamASetIndicators,
            team1Games, android.graphics.Color.parseColor("#3B82F6"))
        drawCircles(binding.layoutTeamBSetIndicators,
            team2Games, android.graphics.Color.parseColor("#F43F5E"))
    }

    private fun drawCircles(container: LinearLayout?, gamesWon: Int, filledColor: Int) {
        container ?: return
        container.removeAllViews()
        val size   = (16 * resources.displayMetrics.density).toInt()
        val margin = (6  * resources.displayMetrics.density).toInt()
        val empty  = android.graphics.Color.parseColor("#E5E7EB")
        val border = android.graphics.Color.parseColor("#D1D5DB")
        for (i in 0 until gamesToWin) {
            val circle = View(requireContext())
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(margin / 2, 0, margin / 2, 0)
            circle.layoutParams = lp
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            if (i < gamesWon) drawable.setColor(filledColor)
            else { drawable.setColor(empty); drawable.setStroke(2, border) }
            circle.background = drawable
            container.addView(circle)
        }
    }

    private fun startGameTimer(startTime: Long) {
        timerTask?.cancel()
        gameStartTimeMs  = startTime
        timerEverStarted = true

        activity?.runOnUiThread {
            if (_binding != null) binding.timer.visibility = View.VISIBLE
        }

        timerTask = object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (_binding == null || gameStartTimeMs == 0L) return@runOnUiThread
                    val elapsed = (System.currentTimeMillis() - gameStartTimeMs) / 1000
                    binding.timer.text = String.format("%02d:%02d", elapsed / 60, elapsed % 60)
                }
            }
        }
        timer.scheduleAtFixedRate(timerTask, 0, 1000)
    }

    private fun setupSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected -> { /* silent */ }
                    is SocketState.Error -> {
                        toast("Socket Error")
                        isActionPending = false
                        if (_binding != null &&
                            binding.futsalScoring.root.visibility == View.VISIBLE)
                            setScoringButtonsEnabled(true)
                    }
                    is SocketState.Disconnected -> {
                        isActionPending = false
                        if (_binding != null &&
                            binding.futsalScoring.root.visibility == View.VISIBLE)
                            setScoringButtonsEnabled(true)
                    }
                }
            }
        }
        WebSocketManager.addMessageListener(SOCKET_KEY) { jsonString ->
            android.util.Log.d("TT_SOCKET", "Raw: $jsonString")
            activity?.runOnUiThread {
                try {
                    handleServerUpdate(JSONObject(jsonString))
                } catch (e: Exception) {
                    android.util.Log.e("TT_SOCKET", "Parse error: ${e.message}")
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

    private fun setupSocketConnection() {
        setupSocketListeners()
    }

    private fun handleServerUpdate(obj: JSONObject) {
        if (_binding == null) return

        isActionPending = false

        team1Points = obj.optInt("team1Points", team1Points)
        team2Points = obj.optInt("team2Points", team2Points)
        team1Games  = obj.optInt("team1Games",  team1Games)
        team2Games  = obj.optInt("team2Games",  team2Games)
        pointsPerGame = obj.optInt("pointsPerGame", pointsPerGame)
        currentGame = obj.optInt("currentGame", currentGame)
        gamesToWin  = obj.optInt("gamesToWin",  gamesToWin)

        val rawStatus = obj.optString("status", "")
        if (rawStatus.isNotEmpty() && rawStatus != "null") matchStatus = rawStatus

        val eventsArray = obj.optJSONArray("tableTennisEvents")
        if (eventsArray != null) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                parseTableTennisEvent(eventsArray.getJSONObject(i))
            }
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }

        if (obj.has("gameStartTime") && !obj.isNull("gameStartTime")) {
            val start = obj.getLong("gameStartTime")
            if (start > 0 && (start != gameStartTimeMs || !timerEverStarted)) {
                startGameTimer(start)
            }
        } else if (!timerEverStarted && matchStatus.uppercase() == "LIVE") {
            startGameTimer(System.currentTimeMillis())
        }

        if (obj.optString("comment") == "UNDO") toast("↩ Undo successful")

        updateScoreUI()
        updateGameCircles()

        if (binding.futsalScoring.root.visibility == View.VISIBLE) {
            setScoringButtonsEnabled(true)
        }

        val status = matchStatus.uppercase()
        if ((status == "COMPLETED" || status == "MATCH_COMPLETE") && !votingAlreadyTriggered) {
            votingAlreadyTriggered = true
            timerTask?.cancel()
            loadAndShowVotingThenSummary()
        }
    }

    private fun parseTableTennisEvent(obj: JSONObject) {
        val eventType = obj.optString("eventType", "").ifEmpty { return }
        if (eventType == "END_GAME") return

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

        val eventTimeSeconds = obj.optInt("eventTimeSeconds", 0)
            .takeIf { it > 0 } ?: (obj.optInt("minute", 0) * 60)

        val gameNum       = obj.optInt("gameNumber", currentGame)
        val scoreSnapshot = obj.safeString("scoreSnapshot")

        eventsList.add(0, TableTennisEvent(
            id               = eventId,
            eventType        = eventType,
            eventTimeSeconds = eventTimeSeconds,
            gameNumber       = gameNum,
            playerName       = playerName,
            teamName         = teamName,
            scoreSnapshot    = scoreSnapshot
        ))
    }

    private fun sendEvent(json: JSONObject) {
        json.put("matchId", matchResponse?.id ?: 0)
        android.util.Log.d("WS_DEBUG", "isConnected: ${WebSocketManager.isConnected()}")
        android.util.Log.d("WS_DEBUG", "Sending: ${json}")
        WebSocketManager.send(json.toString())
    }

    private fun showTableTennisSummary() {
        if (_binding == null || !isAdded) return
        showPanel("summary")
        val s      = binding.layoutFutsalSummary
        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"
        s.tvTeam1Name.text  = t1Name
        s.tvTeam2Name.text  = t2Name
        s.tvTeam1Score.text = team1Games.toString()
        s.tvTeam2Score.text = team2Games.toString()
        s.tvMatchResult.text = when {
            team1Games > team2Games -> "🏆 $t1Name Wins!"
            team2Games > team1Games -> "🏆 $t2Name Wins!"
            else                    -> "🤝 Match Draw!"
        }
    }

    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        val matchId   = matchResponse?.id ?: run { showTableTennisSummary(); return }
        val accountId = getAccountId()
        if (hasAlreadyVoted(matchId)) { showTableTennisSummary(); return }

        binding.layoutProgressBar.visibility = View.VISIBLE
        val v = binding.layoutVoting
        v.tvVoteTeam1Name.text              = matchResponse?.team1Name ?: "Team 1"
        v.tvVoteTeam2Name.text              = matchResponse?.team2Name ?: "Team 2"
        selectedVotePlayerId                = null
        v.btnSubmitVote.isEnabled           = false
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
                    selectedVotePlayerId   = player.id
                    selectedVotePlayerName = player.name ?: ""
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
        v.btnSkipVote.setOnClickListener { showTableTennisSummary() }
    }

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long) {
        if (accountId == -1L) { toast("Login again"); showTableTennisSummary(); return }
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
                    response.isSuccessful   -> { markAsVoted(matchId); toast("Vote submitted!"); showTableTennisSummary() }
                    response.code() == 409  -> { markAsVoted(matchId); toast("Already voted!"); showTableTennisSummary() }
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
        if (id == null || name.isNullOrBlank()) null
        else Player(id = id, name = name, status = "")
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

    companion object {
        fun newInstance(match: MatchResponse) = TableTennisScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}