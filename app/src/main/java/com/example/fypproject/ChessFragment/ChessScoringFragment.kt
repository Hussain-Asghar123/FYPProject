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
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.ChessEvent
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

    // ── State ──────────────────────────────────────────────
    private var matchStatus     = "LIVE"
    private var resultType: String? = null
    private var isDraw          = false
    private var winnerTeamId: Long? = null
    private var matchStartTimeMs = 0L
    private var isActionPending  = false
    private var timerEverStarted = false

    // ── JS: pendingWinner + pendingDraw state ──────────────
    private var pendingWinnerTeamId:   Long?  = null
    private var pendingWinnerTeamName: String = ""
    private var pendingDrawMode:       Boolean = false

    // ── Players + canEdit ──────────────────────────────────
    private var canEdit = false

    // ── Voting ─────────────────────────────────────────────
    private var votingAlreadyTriggered = false
    private var selectedVotePlayerId: Long? = null
    private var selectedVotePlayerName = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null

    // ── Events ─────────────────────────────────────────────
    private val eventsList = mutableListOf<ChessEvent>()
    private lateinit var eventsAdapter: ChessEventAdapter

    // ── Timer ──────────────────────────────────────────────
    private var timerTask: TimerTask? = null
    private val timer = Timer()

    // ── Media ──────────────────────────────────────────────
    private val SOCKET_KEY = "ChessScoringFragment"
    private var pendingEventId: Long? = null
    private var pendingComment: String? = null
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

        val status = matchResponse?.status?.uppercase().orEmpty()
        val isCompleted = status == "COMPLETED" || status == "MATCH_COMPLETE"

        if (isCompleted) {
            votingAlreadyTriggered = true
            showPanel("loading")
            loadAndShowVotingThenSummary()
        } else {
            if (canEdit) {
                showTab("scoring")
                showPanel("scoring")
            } else {
                showTab("moves")
            }
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
        val role     = prefs.getString("role",     "")?.trim().orEmpty()
        val username = prefs.getString("username", "")?.trim().orEmpty()
        val scorer   = matchResponse?.scorerId?.trim().orEmpty()
        val media    = matchResponse?.mediaScorerId?.trim().orEmpty()
        canEdit = role.equals("ADMIN", true)
                || scorer.equals(username, true)
                || media.equals(username, true)
    }

    private fun setupBottomTabs() {
        if (!canEdit) binding.tabScoring.visibility = View.GONE
        binding.tabScoring.setOnClickListener { showTab("scoring") }
        binding.tabMoves.setOnClickListener   { showTab("moves")   }
    }

    private fun showTab(tab: String) {
        val isScoring = tab == "scoring"
        binding.scoringTabContent.visibility = if (isScoring) View.VISIBLE else View.GONE
        binding.eventsTabContent.visibility  = if (isScoring) View.GONE    else View.VISIBLE
        binding.tabScoring.isSelected        = isScoring
        binding.tabMoves.isSelected          = !isScoring
        binding.tabScoring.setTextColor(
            if (isScoring) android.graphics.Color.parseColor("#1E293B")
            else           android.graphics.Color.parseColor("#888888")
        )
        binding.tabMoves.setTextColor(
            if (!isScoring) android.graphics.Color.parseColor("#1E293B")
            else            android.graphics.Color.parseColor("#888888")
        )
        if (!isScoring) {
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }
    }

    private fun setupEventsRecycler() {
        eventsAdapter = ChessEventAdapter(eventsList) { event ->
            if (canEdit) showMediaDialog(event.id)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter       = eventsAdapter
    }

    private fun showPanel(panel: String) {
        binding.layoutScoring.root.visibility  = View.GONE
        binding.layoutVoting.root.visibility   = View.GONE
        binding.layoutSummary.root.visibility  = View.GONE
        binding.layoutProgressBar.visibility   = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> { binding.layoutScoring.root.visibility = View.VISIBLE; setupScoringPanel() }
            "voting"  ->   binding.layoutVoting.root.visibility  = View.VISIBLE
            "summary" ->   binding.layoutSummary.root.visibility = View.VISIBLE
            "loading" ->   binding.layoutProgressBar.visibility  = View.VISIBLE
        }
    }
    private fun setupScoringPanel() {
        updateScoreUI()

        if (!canEdit) {
            binding.layoutScoring.layoutMainButtons.visibility = View.GONE
            return
        }

        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"

        binding.layoutScoring.btnTeam1Wins.text = "$t1Name Wins"
        binding.layoutScoring.btnTeam2Wins.text = "$t2Name Wins"
        binding.layoutScoring.tvDrawTeams.text  = "🤝 $t1Name vs $t2Name"

        refreshSubPanel()

        binding.layoutScoring.btnTeam1Wins.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            pendingWinnerTeamId   = matchResponse?.team1Id
            pendingWinnerTeamName = t1Name
            binding.layoutScoring.tvWinnerLine.text = "🏆 $t1Name wins"
            binding.layoutScoring.tvLoserLine.text  = "❌ $t2Name loses"
            showScoringSubPanel("confirmWin")
        }

        binding.layoutScoring.btnTeam2Wins.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            pendingWinnerTeamId   = matchResponse?.team2Id
            pendingWinnerTeamName = t2Name
            binding.layoutScoring.tvWinnerLine.text = "🏆 $t2Name wins"
            binding.layoutScoring.tvLoserLine.text  = "❌ $t1Name loses"
            showScoringSubPanel("confirmWin")
        }

        binding.layoutScoring.btnDraw.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            pendingDrawMode     = true
            pendingWinnerTeamId = null
            showScoringSubPanel("confirmDraw")
        }

        binding.layoutScoring.btnConfirmWin.setOnClickListener {
            val teamId = pendingWinnerTeamId ?: return@setOnClickListener
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "CHECKMATE").put("teamId", teamId))
            cancelPending()
        }

        binding.layoutScoring.tvConfirmWinClose.setOnClickListener   { cancelPending() }
        binding.layoutScoring.btnConfirmWinCancel.setOnClickListener { cancelPending() }

        binding.layoutScoring.btnConfirmDraw.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "DRAW_AGREED"))
            cancelPending()
        }

        binding.layoutScoring.tvConfirmDrawClose.setOnClickListener   { cancelPending() }
        binding.layoutScoring.btnConfirmDrawCancel.setOnClickListener { cancelPending() }


        setScoringButtonsEnabled(!isActionPending)
    }

    private fun showScoringSubPanel(panel: String) {
        binding.layoutScoring.layoutMainButtons.visibility = View.GONE
        binding.layoutScoring.layoutConfirmWin.visibility  = View.GONE
        binding.layoutScoring.layoutConfirmDraw.visibility = View.GONE

        when (panel) {
            "main"        -> binding.layoutScoring.layoutMainButtons.visibility = View.VISIBLE
            "confirmWin"  -> binding.layoutScoring.layoutConfirmWin.visibility  = View.VISIBLE
            "confirmDraw" -> binding.layoutScoring.layoutConfirmDraw.visibility = View.VISIBLE
        }
    }

    private fun refreshSubPanel() {
        when {
            pendingWinnerTeamId != null -> showScoringSubPanel("confirmWin")
            pendingDrawMode             -> showScoringSubPanel("confirmDraw")
            else                        -> showScoringSubPanel("main")
        }
    }

    private fun cancelPending() {
        pendingWinnerTeamId = null
        pendingDrawMode     = false
        showScoringSubPanel("main")
    }

    private fun setScoringButtonsEnabled(enabled: Boolean) {
        if (_binding == null) return
        val alpha = if (enabled) 1f else 0.45f
        listOf(
            binding.layoutScoring.btnTeam1Wins,
            binding.layoutScoring.btnTeam2Wins,
            binding.layoutScoring.btnDraw,
            binding.layoutScoring.btnConfirmWin,
            binding.layoutScoring.btnConfirmDraw
        ).forEach { it.isEnabled = enabled; it.alpha = alpha }
    }

    private fun updateScoreUI() {
        if (_binding == null) return
        binding.teamA.text = matchResponse?.team1Name ?: "Team A"
        binding.teamB.text = matchResponse?.team2Name ?: "Team B"
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
                    is SocketState.Connected    -> { /* silent */ }
                    is SocketState.Error        -> {
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
            android.util.Log.d("CHESS_WS", jsonString)
            activity?.runOnUiThread {
                try {
                    handleServerUpdate(JSONObject(jsonString))
                } catch (e: Exception) {
                    android.util.Log.e("CHESS_WS", "Parse error: ${e.message}")
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
        matchResponse?.id?.let { WebSocketManager.connect(it) }
    }

    private fun handleServerUpdate(obj: JSONObject) {
        if (_binding == null) return

        isActionPending = false
        val rawStatus = obj.optString("status", "")
        if (rawStatus.isNotEmpty() && rawStatus != "null") matchStatus = rawStatus

        val rawResult = obj.optString("resultType", "")
        if (rawResult.isNotEmpty() && rawResult != "null") resultType = rawResult

        isDraw = obj.optBoolean("isDraw", isDraw)

        val rawWinner = obj.optLong("winnerTeamId", -1L)
        if (rawWinner != -1L) winnerTeamId = rawWinner

        val eventsArray = obj.optJSONArray("chessEvents")
        if (eventsArray != null) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                val ev        = eventsArray.getJSONObject(i)
                val eventType = ev.optString("eventType", "").ifEmpty { continue }

                fun JSONObject.safeStr(key: String) =
                    if (isNull(key)) "" else optString(key, "").let { if (it == "null") "" else it }

                eventsList.add(0, ChessEvent(
                    id               = ev.optLong("id", System.currentTimeMillis()),
                    eventType        = eventType,
                    teamName         = ev.safeStr("teamName").ifEmpty { null },
                    playerName       = ev.safeStr("playerName").ifEmpty { null },
                    moveNotation     = ev.safeStr("moveNotation").ifEmpty { null },
                    moveNumber       = ev.optInt("moveNumber", 0).takeIf { it > 0 },
                    eventTimeSeconds = ev.optInt("eventTimeSeconds", 0).takeIf { it > 0 }
                ))
            }
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }

        // Timer
        if (obj.has("matchStartTime") && !obj.isNull("matchStartTime")) {
            val start = obj.getLong("matchStartTime")
            if (start > 0 && (start != matchStartTimeMs || !timerEverStarted)) {
                startMatchTimer(start)
            }
        }

        if (obj.optString("comment") == "UNDO") toast("↩ Undo successful")

        updateScoreUI()

        if (binding.layoutScoring.root.visibility == View.VISIBLE)
            setScoringButtonsEnabled(true)

        if (binding.layoutVoting.root.visibility == View.VISIBLE ||
            binding.layoutSummary.root.visibility == View.VISIBLE) return

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

    private fun sendEvent(json: JSONObject) {
        json.put("matchId", matchResponse?.id ?: 0)
        WebSocketManager.send(json.toString())
    }

    private fun showChessSummary() {
        if (_binding == null || !isAdded) return
        showPanel("summary")
        val s = binding.layoutSummary

        s.tvMatchResult.text = if (isDraw) {
            "🤝 Draw!"
        } else {
            val winnerName = when (winnerTeamId) {
                matchResponse?.team1Id -> matchResponse?.team1Name ?: "Team A"
                matchResponse?.team2Id -> matchResponse?.team2Name ?: "Team B"
                else -> "Match Completed!"
            }
            "🏆 $winnerName Wins!"
        }

        s.tvResultType.text = when (resultType) {
            "CHECKMATE"   -> "Checkmate"
            "RESIGN"      -> "Resignation"
            "TIMEOUT"     -> "Timeout"
            "STALEMATE"   -> "Stalemate"
            "DRAW_AGREED" -> "Draw by Agreement"
            else          -> resultType ?: ""
        }
    }
    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        binding.scoringTabContent.visibility = View.VISIBLE
        binding.eventsTabContent.visibility  = View.GONE

        val matchId   = matchResponse?.id ?: run { showChessSummary(); return }
        val accountId = getAccountId()
        if (hasAlreadyVoted(matchId)) { showChessSummary(); return }

        binding.layoutProgressBar.visibility = View.VISIBLE
        val v = binding.layoutVoting
        v.tvVoteTeam1Name.text = matchResponse?.team1Name ?: "Team 1"
        v.tvVoteTeam2Name.text = matchResponse?.team2Name ?: "Team 2"
        selectedVotePlayerId   = null
        v.btnSubmitVote.isEnabled = false
        v.layoutSelectedPlayerBanner.visibility = View.GONE
        v.etVoteFeedback.text?.clear()

        lifecycleScope.launch {
            try {
                val t1 = matchResponse?.team1Id ?: return@launch
                val t2 = matchResponse?.team2Id ?: return@launch
                val (r1, r2) = withContext(Dispatchers.IO) {
                    val d1 = async { RetrofitInstance.api.getPlayersByTeam(t1) }
                    val d2 = async { RetrofitInstance.api.getPlayersByTeam(t2) }
                    d1.await() to d2.await()
                }
                val players1 = if (r1.isSuccessful) r1.body() ?: emptyList() else emptyList()
                val players2 = if (r2.isSuccessful) r2.body() ?: emptyList() else emptyList()

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
            val feedback = v.etVoteFeedback.text?.toString()?.trim().orEmpty()
            submitVote(matchId, accountId, playerId, feedback )
        }
        v.btnSkipVote.setOnClickListener { showChessSummary() }
    }

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long, feedback: String? = null) {
        if (accountId == -1L) {
            toast("Account not found. Please login again.")
            showChessSummary()
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
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.submitVote(body)
                }
                when {
                    response.isSuccessful -> {
                        markAsVoted(matchId)
                        toast("Vote submitted!")
                        showChessSummary()
                    }
                    response.code() == 409 -> {
                        markAsVoted(matchId)
                        toast("Already voted!")
                        showChessSummary()
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

    private fun showMediaDialog(eventId: Long?) {
        pendingEventId = eventId
        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_media_source, null)

        val etComment  = dialogView.findViewById<android.widget.EditText?>(R.id.etMediaComment)
        val btnCamera  = dialogView.findViewById<View>(R.id.btnOpenCamera)
        val btnGallery = dialogView.findViewById<View>(R.id.btnOpenGallery)
        val btnCancel  = dialogView.findViewById<android.widget.TextView>(R.id.btnCancelMedia)
        val tvGallery  = dialogView.findViewById<android.widget.TextView>(R.id.tvGalleryLabel)

        if (isUploading) tvGallery.text = "Uploading..."

        btnCamera.setOnClickListener {
            pendingComment = etComment?.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            dialog.dismiss(); openCamera()
        }
        btnGallery.setOnClickListener {
            if (!isUploading) {
                pendingComment = etComment?.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                dialog.dismiss(); galleryLauncher.launch("image/*")
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun openCamera() {
        val file = File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", file
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun uploadMediaFile(uri: Uri) {
        val matchId = matchResponse?.id ?: return
        val eventId = pendingEventId   ?: return
        isUploading = true
        showUploadProgress(true)
        toast("Uploading...")

        lifecycleScope.launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val tempFile    = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                tempFile.outputStream().use { out -> inputStream?.copyTo(out) }
                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart    = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val matchIdBody = matchId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val eventIdBody = eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val commentBody = pendingComment?.toRequestBody("text/plain".toMediaTypeOrNull())
                val resp = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.createMedia(matchIdBody, eventIdBody, filePart, commentBody)
                }
                if (resp.isSuccessful) toast("✅ Upload Successful!")
                else                   toast("❌ Upload failed: ${resp.code()}")
            } catch (e: Exception) { toast("❌ Upload failed: ${e.message}") }
            finally {
                showUploadProgress(false)
                isUploading    = false
                pendingEventId = null
                pendingComment = null
            }
        }
    }

    private fun showUploadProgress(show: Boolean) {
        if (_binding == null) return
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
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

    private fun getAccountId() =
        requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE).getLong("id", -1L)

    private fun toast(msg: String) = context?.toastShort(msg)

    override fun onResume() {
        super.onResume()
        setupSocketListeners()
        matchResponse?.id?.let { WebSocketManager.connect(it) }
    }

    override fun onPause() {
        super.onPause()
        WebSocketManager.disconnect()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) setupSocketListeners() else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerTask?.cancel()
        unregisterSocketListeners()
        WebSocketManager.disconnect()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = ChessScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}