package com.example.fypproject.ChessFragment

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
import com.example.fypproject.Adapter.ChessEventAdapter
import com.example.fypproject.Adapter.TugOfWarEventAdapter
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.ChessEvent
import com.example.fypproject.ScoringDTO.Player
import com.example.fypproject.ScoringDTO.TugOfWarEvent
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ChessScoringFragmentBinding
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Timer
import java.util.TimerTask

class ChessScoringFragment : Fragment(R.layout.chess_scoring_fragment) {

    private var _binding: ChessScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    private var team1Moves = 0
    private var team2Moves = 0
    private var team1Checks = 0
    private var team2Checks = 0
    private var totalMoves = 0
    private var matchStatus = "LIVE"
    private var resultType: String? = null
    private var isDraw = false
    private var currentTurnTeamId: Long? = null
    private var currentTurnTeamName: String? = null
    private var matchStartTimeMs = 0L
    private var isActionPending = false
    private var timerEverStarted = false

    private var movePanelTeamId: Long? = null

    private var team1Players = listOf<Player>()
    private var team2Players = listOf<Player>()
    private var canEdit = false

    private var votingAlreadyTriggered = false
    private var selectedVotePlayerId: Long? = null
    private var selectedVotePlayerName = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null

    private val eventsList = mutableListOf<ChessEvent>()
    private lateinit var eventsAdapter: ChessEventAdapter

    private var timerTask: TimerTask? = null
    private val timer = Timer()

    private val SOCKET_KEY = "ChessScoringFragment"
    private var pendingEventId: Long? = null
    private var cameraImageUri: Uri? = null
    private var isUploading = false

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { uploadMediaFile(it) } }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraImageUri?.let { uploadMediaFile(it) } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ChessScoringFragmentBinding.bind(view)

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
        val prefs = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val role = prefs.getString("role", "")?.trim().orEmpty()
        val username = prefs.getString("username", "")?.trim().orEmpty()
        val scorer = matchResponse?.scorerId?.trim().orEmpty()
        canEdit = role.equals("ADMIN", true) || scorer.equals(username, true)
    }

    private fun setupBottomTabs() {
        binding.tabScoring.setOnClickListener { showTab("scoring") }
        binding.tabMoves.setOnClickListener { showTab("moves") }
    }

    private fun uploadMediaFile(uri: Uri) {
        val matchId = matchResponse?.id ?: return
        val eventId = pendingEventId ?: return
        isUploading = true

        // Show progress overlay
        showUploadProgress(true)
        toast("Uploading...")

        lifecycleScope.launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val tempFile = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                tempFile.outputStream().use { out -> inputStream?.copyTo(out) }
                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val matchIdBody = matchId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val eventIdBody = eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.createMedia(matchIdBody, eventIdBody, filePart)
                }
                if (response.isSuccessful) toast("✅ Upload Successful!")
                else toast("❌ Upload failed: ${response.code()}")
            } catch (e: Exception) {
                toast("❌ Upload failed: ${e.message}")
            } finally {
                // Hide progress overlay
                showUploadProgress(false)
                isUploading = false
                pendingEventId = null
            }
        }
    }

    private fun showUploadProgress(show: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showMediaDialog(eventId: Long?) {
        pendingEventId = eventId
        val dialog = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_media_source, null)
        val btnCamera = dialogView.findViewById<View>(R.id.btnOpenCamera)
        val btnGallery = dialogView.findViewById<View>(R.id.btnOpenGallery)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelMedia)
        val tvGallery = dialogView.findViewById<TextView>(R.id.tvGalleryLabel)
        if (isUploading) tvGallery.text = "Uploading"
        btnCamera.setOnClickListener { dialog.dismiss(); openCamera() }
        btnGallery.setOnClickListener {
            if (!isUploading) {
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
            requireContext(), "${requireContext().packageName}.provider", imageFile
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun showTab(tab: String) {
        val isScoring = tab == "scoring"
        binding.scoringTabContent.visibility = if (isScoring) View.VISIBLE else View.GONE
        binding.eventsTabContent.visibility = if (isScoring) View.GONE else View.VISIBLE
        binding.tabScoring.isSelected = isScoring
        binding.tabMoves.isSelected = !isScoring
        binding.tabScoring.setTextColor(
            if (isScoring) android.graphics.Color.parseColor("#1E293B")
            else android.graphics.Color.parseColor("#888888")
        )
        binding.tabMoves.setTextColor(
            if (!isScoring) android.graphics.Color.parseColor("#1E293B")
            else android.graphics.Color.parseColor("#888888")
        )
        if (!isScoring) {
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }
    }
    private fun setupEventsRecycler() {
        eventsAdapter = ChessEventAdapter(eventsList){event ->
            showMediaDialog(event.id)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventsAdapter
    }

    private fun showPanel(panel: String) {
        binding.layoutScoring.root.visibility = View.GONE
        binding.layoutMove.root.visibility = View.GONE
        binding.layoutCheck.root.visibility = View.GONE
        binding.layoutTerminal.root.visibility = View.GONE
        binding.layoutVoting.root.visibility = View.GONE
        binding.layoutSummary.root.visibility = View.GONE
        binding.layoutProgressBar.visibility = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> {
                binding.layoutScoring.root.visibility = View.VISIBLE; setupScoringPanel()
            }

            "move" -> {
                binding.layoutMove.root.visibility = View.VISIBLE; setupMovePanel()
            }

            "check" -> {
                binding.layoutCheck.root.visibility = View.VISIBLE; setupCheckPanel()
            }

            "terminal" -> {
                binding.layoutTerminal.root.visibility = View.VISIBLE; setupTerminalPanel()
            }

            "voting" -> binding.layoutVoting.root.visibility = View.VISIBLE
            "summary" -> binding.layoutSummary.root.visibility = View.VISIBLE
            "loading" -> binding.layoutProgressBar.visibility = View.VISIBLE
        }
    }

    private fun setupScoringPanel() {
        updateScoreUI()

        if (!canEdit) {
            binding.layoutScoring.btnMoveTeam1.visibility = View.GONE
            binding.layoutScoring.btnMoveTeam2.visibility = View.GONE
            binding.layoutScoring.btnCheck.visibility = View.GONE
            binding.layoutScoring.btnEndGame.visibility = View.GONE
            binding.layoutScoring.btnUndo.visibility = View.GONE
            return
        }

        binding.layoutScoring.btnMoveTeam1.text =
            "⬜ ${matchResponse?.team1Name ?: "Team A"}"
        binding.layoutScoring.btnMoveTeam2.text =
            "⬛ ${matchResponse?.team2Name ?: "Team B"}"

        binding.layoutScoring.btnMoveTeam1.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            movePanelTeamId = matchResponse?.team1Id
            showPanel("move")
        }
        binding.layoutScoring.btnMoveTeam2.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            movePanelTeamId = matchResponse?.team2Id
            showPanel("move")
        }

        binding.layoutScoring.btnCheck.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            showPanel("check")
        }

        binding.layoutScoring.btnEndGame.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            showPanel("terminal")
        }
        binding.layoutScoring.btnUndo.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("undo", true))
        }

        setScoringButtonsEnabled(!isActionPending)
    }

    private fun setupMovePanel() {
        val m = binding.layoutMove
        m.tvClose.setOnClickListener { showPanel("scoring") }

        val teamName = if (movePanelTeamId == matchResponse?.team1Id)
            "⬜ ${matchResponse?.team1Name ?: "Team A"}"
        else
            "⬛ ${matchResponse?.team2Name ?: "Team B"}"

        m.tvTitle.text = "♟️ Record Move — $teamName"

        val players = if (movePanelTeamId == matchResponse?.team1Id)
            team1Players else team2Players
        m.spinnerPlayer.setupWithPlayers("Select Player (Optional)", players)

        m.btnConfirmMove.setOnClickListener {
            val teamId = movePanelTeamId ?: return@setOnClickListener
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)

            val json = JSONObject().put("eventType", "MOVE").put("teamId", teamId)
            val notation = m.etMoveNotation.text?.toString()?.trim()
            if (!notation.isNullOrEmpty()) json.put("moveNotation", notation)
            val playerId = m.spinnerPlayer.selectedId()
            if (playerId != null) json.put("playerId", playerId)

            sendEvent(json)
            m.etMoveNotation.text?.clear()
            showPanel("scoring")
        }
    }

    private fun setupCheckPanel() {
        val c = binding.layoutCheck
        c.tvClose.setOnClickListener { showPanel("scoring") }

        c.btnCheckTeam1.text =
            "⬜ ${matchResponse?.team1Name ?: "Team A"} delivered check"
        c.btnCheckTeam2.text =
            "⬛ ${matchResponse?.team2Name ?: "Team B"} delivered check"

        c.btnCheckTeam1.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(
                JSONObject()
                    .put("eventType", "CHECK")
                    .put("teamId", matchResponse?.team1Id)
            )
            showPanel("scoring")
        }
        c.btnCheckTeam2.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(
                JSONObject()
                    .put("eventType", "CHECK")
                    .put("teamId", matchResponse?.team2Id)
            )
            showPanel("scoring")
        }
        c.btnCancel.setOnClickListener { showPanel("scoring") }
    }

    private fun setupTerminalPanel() {
        val t = binding.layoutTerminal
        t.tvClose.setOnClickListener { showPanel("scoring") }

        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"

        t.btnCheckmateTeam1.text = "⬜ $t1Name"
        t.btnCheckmateTeam2.text = "⬛ $t2Name"
        t.btnResignTeam1.text = "⬜ $t1Name resigns"
        t.btnResignTeam2.text = "⬛ $t2Name resigns"
        t.btnTimeoutTeam1.text = "⬜ $t1Name"
        t.btnTimeoutTeam2.text = "⬛ $t2Name"

        t.btnCheckmateTeam1.setOnClickListener {
            sendTerminal(
                JSONObject().put("eventType", "CHECKMATE").put("teamId", matchResponse?.team1Id)
            )
        }
        t.btnCheckmateTeam2.setOnClickListener {
            sendTerminal(
                JSONObject().put("eventType", "CHECKMATE").put("teamId", matchResponse?.team2Id)
            )
        }
        t.btnResignTeam1.setOnClickListener {
            sendTerminal(
                JSONObject().put("eventType", "RESIGN").put("teamId", matchResponse?.team1Id)
            )
        }
        t.btnResignTeam2.setOnClickListener {
            sendTerminal(
                JSONObject().put("eventType", "RESIGN").put("teamId", matchResponse?.team2Id)
            )
        }
        t.btnStalemate.setOnClickListener {
            sendTerminal(JSONObject().put("eventType", "STALEMATE"))
        }
        t.btnDrawAgreed.setOnClickListener {
            sendTerminal(JSONObject().put("eventType", "DRAW_AGREED"))
        }
        // ✅ Timeout
        t.btnTimeoutTeam1.setOnClickListener {
            sendTerminal(
                JSONObject().put("eventType", "TIMEOUT").put("teamId", matchResponse?.team1Id)
            )
        }
        t.btnTimeoutTeam2.setOnClickListener {
            sendTerminal(
                JSONObject().put("eventType", "TIMEOUT").put("teamId", matchResponse?.team2Id)
            )
        }

        t.btnCancel.setOnClickListener { showPanel("scoring") }
    }

    private fun sendTerminal(json: JSONObject) {
        if (isActionPending) return
        isActionPending = true
        setScoringButtonsEnabled(false)
        sendEvent(json)
        showPanel("scoring")
    }

    private fun setScoringButtonsEnabled(enabled: Boolean) {
        if (_binding == null) return
        val alpha = if (enabled) 1f else 0.45f
        listOf(
            binding.layoutScoring.btnMoveTeam1,
            binding.layoutScoring.btnMoveTeam2,
            binding.layoutScoring.btnCheck,
            binding.layoutScoring.btnEndGame,
            binding.layoutScoring.btnUndo
        ).forEach {
            it.isEnabled = enabled
            it.alpha = alpha
        }
    }

    private fun updateScoreUI() {
        if (_binding == null) return
        binding.tvTeam1Moves.text = team1Moves.toString()
        binding.tvTeam2Moves.text = team2Moves.toString()
        binding.tvTeam1Checks.text = "⚔️ $team1Checks checks"
        binding.tvTeam2Checks.text = "⚔️ $team2Checks checks"
        binding.tvTotalMoves.text = "Total moves: $totalMoves"

        val turnName = currentTurnTeamName ?: matchResponse?.team1Name ?: "Team A"
        val isT1 = currentTurnTeamId == matchResponse?.team1Id
        binding.tvCurrentTurn.text = "${if (isT1) "⬜" else "⬛"} ${turnName}'s turn"

        binding.layoutScoring.tvTurnHint.text =
            "${if (isT1) "⬜" else "⬛"} $turnName to move"
    }

    private fun startMatchTimer(startTime: Long) {
        timerTask?.cancel()
        matchStartTimeMs = startTime
        timerEverStarted = true

        activity?.runOnUiThread {
            if (_binding != null) binding.tvTimer.visibility = View.VISIBLE
        }

        timerTask = object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (_binding == null || matchStartTimeMs == 0L) return@runOnUiThread
                    val elapsed = (System.currentTimeMillis() - matchStartTimeMs) / 1000
                    val h = elapsed / 3600
                    val m = (elapsed % 3600) / 60
                    val s = elapsed % 60
                    binding.tvTimer.text = if (h > 0)
                        String.format("%02d:%02d:%02d", h, m, s)
                    else
                        String.format("%02d:%02d", m, s)
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
                            binding.layoutScoring.root.visibility == View.VISIBLE)
                            setScoringButtonsEnabled(true)
                    }
                    is SocketState.Disconnected -> {
                        isActionPending = false
                        if (_binding != null &&
                            binding.layoutScoring.root.visibility == View.VISIBLE)
                            setScoringButtonsEnabled(true)
                    }
                }
            }
        }
        WebSocketManager.addMessageListener(SOCKET_KEY) { jsonString ->
            activity?.runOnUiThread {
                try {
                    handleServerUpdate(JSONObject(jsonString))
                } catch (e: Exception) {
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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) setupSocketListeners()
        else unregisterSocketListeners()
    }
    private fun setupSocketConnection() {
        setupSocketListeners()
    }
    private fun handleServerUpdate(obj: JSONObject) {
        if (_binding == null) return

        isActionPending = false

        team1Moves  = obj.optInt("team1Moves",  team1Moves)
        team2Moves  = obj.optInt("team2Moves",  team2Moves)
        team1Checks = obj.optInt("team1Checks", team1Checks)
        team2Checks = obj.optInt("team2Checks", team2Checks)
        totalMoves  = obj.optInt("totalMoves",  totalMoves)
        isDraw      = obj.optBoolean("isDraw",   isDraw)

        val rawStatus = obj.optString("status", "")
        if (rawStatus.isNotEmpty() && rawStatus != "null") matchStatus = rawStatus

        val rawResult = obj.optString("resultType", "")
        if (rawResult.isNotEmpty() && rawResult != "null") resultType = rawResult

        val rawTurnId = obj.optLong("currentTurnTeamId", -1L)
        if (rawTurnId != -1L) currentTurnTeamId = rawTurnId

        val rawTurnName = obj.optString("currentTurnTeamName", "")
        if (rawTurnName.isNotEmpty() && rawTurnName != "null")
            currentTurnTeamName = rawTurnName

        val eventsArray = obj.optJSONArray("chessEvents")
        if (eventsArray != null) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                val ev = eventsArray.getJSONObject(i)
                val eventType = ev.optString("eventType", "").ifEmpty { continue }

                fun JSONObject.safeString(key: String) =
                    if (isNull(key)) "" else optString(key, "")
                        .let { if (it == "null") "" else it }

                eventsList.add(0, ChessEvent(
                    id               = ev.optLong("id", System.currentTimeMillis()),
                    eventType        = eventType,
                    teamName         = ev.safeString("teamName").ifEmpty { null },
                    playerName       = ev.safeString("playerName").ifEmpty { null },
                    moveNotation     = ev.safeString("moveNotation").ifEmpty { null },
                    moveNumber       = ev.optInt("moveNumber", 0).takeIf { it > 0 },
                    eventTimeSeconds = ev.optInt("eventTimeSeconds", 0).takeIf { it > 0 }
                ))
            }
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }

        if (obj.has("matchStartTime") && !obj.isNull("matchStartTime")) {
            val start = obj.getLong("matchStartTime")
            if (start > 0 && (start != matchStartTimeMs || !timerEverStarted)) {
                startMatchTimer(start)
            }
        }

        if (obj.optString("comment") == "UNDO") toast("↩ Undo successful")

        updateScoreUI()

        if (binding.layoutScoring.root.visibility == View.VISIBLE) {
            setScoringButtonsEnabled(true)
        }

        val status = matchStatus.uppercase()
        if ((status == "COMPLETED" || status == "MATCH_COMPLETE") && !votingAlreadyTriggered) {
            votingAlreadyTriggered = true
            timerTask?.cancel()
            loadAndShowVotingThenSummary()
        }
    }

    private fun sendEvent(json: JSONObject) {
        json.put("matchId", matchResponse?.id ?: 0)
        WebSocketManager.send(json.toString())
    }

    private fun showChessSummary() {
        if (_binding == null || !isAdded) return
        showPanel("summary")
        val s      = binding.layoutSummary
        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"

        s.tvTeam1Name.text   = t1Name
        s.tvTeam2Name.text   = t2Name
        s.tvTeam1Moves.text  = team1Moves.toString()
        s.tvTeam2Moves.text  = team2Moves.toString()
        s.tvTeam1Checks.text = "⚔️ $team1Checks checks"
        s.tvTeam2Checks.text = "⚔️ $team2Checks checks"
        s.tvTotalMoves.text  = "Total moves: $totalMoves"

        val resultLabel = when (resultType) {
            "CHECKMATE"   -> "Checkmate"
            "RESIGN"      -> "Resignation"
            "TIMEOUT"     -> "Timeout"
            "STALEMATE"   -> "Stalemate"
            "DRAW_AGREED" -> "Draw by Agreement"
            "END_MATCH"   -> "Match Ended"
            else          -> resultType ?: ""
        }
        s.tvResultType.text = resultLabel

        s.tvMatchResult.text = if (isDraw) "🤝 Draw!" else "♟️ Match Completed!"
    }

    // ── Voting ────────────────────────────────────────────────────
    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        val matchId   = matchResponse?.id ?: run { showChessSummary(); return }
        val accountId = getAccountId()
        if (hasAlreadyVoted(matchId)) { showChessSummary(); return }

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
        v.btnSkipVote.setOnClickListener { showChessSummary() }
    }

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long) {
        if (accountId == -1L) { toast("Login again"); showChessSummary(); return }
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
                    response.isSuccessful   -> { markAsVoted(matchId); toast("Vote submitted!"); showChessSummary() }
                    response.code() == 409  -> { markAsVoted(matchId); toast("Already voted!"); showChessSummary() }
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

    private fun Spinner.setupWithPlayers(placeholder: String, players: List<Player>) {
        adapter = ArrayAdapter(requireContext(), R.layout.spinner_item,
            listOf(placeholder) + players.map { it.name }).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        tag = listOf<Int?>(null) + players.map { it.id }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Spinner.selectedId(): Int? {
        val ids = tag as? List<Int?> ?: return null
        val pos = selectedItemPosition
        return if (pos in ids.indices) ids[pos] else null
    }

    private fun List<TeamPlayerDto>.toScoringPlayers() = mapNotNull { dto ->
        val id   = dto.id?.toInt()
        val name = dto.name
        if (id == null || name.isNullOrBlank()) null else Player(id = id, name = name, status = "")
    }

    override fun onResume() {
        super.onResume()
        setupSocketListeners()
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
        fun newInstance(match: MatchResponse) = ChessScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}