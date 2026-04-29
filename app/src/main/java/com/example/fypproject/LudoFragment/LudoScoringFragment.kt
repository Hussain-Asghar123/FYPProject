package com.example.fypproject.LudoFragment

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
import com.example.fypproject.Adapter.LudoEventAdapter
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.LudoEvent
import com.example.fypproject.ScoringDTO.Player
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.LudoScoringFragmentBinding
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Timer
import java.util.TimerTask

class LudoScoringFragment : Fragment(R.layout.ludo_scoring_fragment) {

    private var _binding: LudoScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    private val SOCKET_KEY = "LudoScoringFragment"

    private var team1HomeRuns   = 0
    private var team2HomeRuns   = 0
    private var team1Captures   = 0
    private var team2Captures   = 0
    private var matchStatus     = "LIVE"
    private var matchStartTimeMs = 0L
    private var isActionPending  = false
    private var timerEverStarted = false

    private var activePanel: String? = null

    private var team1Players = listOf<Player>()
    private var team2Players = listOf<Player>()
    private var canEdit      = false

    private var votingAlreadyTriggered = false
    private var selectedVotePlayerId: Long? = null
    private var selectedVotePlayerName = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null

    private val eventsList = mutableListOf<LudoEvent>()
    private lateinit var eventsAdapter: LudoEventAdapter

    private var timerTask: TimerTask? = null
    private val timer = Timer()

    private var selectedTeamId: Long?  = null
    private var selectedPlayerId: Int? = null

    private var pendingEventId: Long? = null
    private var cameraImageUri: Uri?  = null
    private var isUploading           = false

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { uploadMediaFile(it) } }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraImageUri?.let { uploadMediaFile(it) } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = LudoScoringFragmentBinding.bind(view)

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

    private fun setupEventsRecycler() {
        eventsAdapter = LudoEventAdapter(eventsList) { event ->
            showMediaDialog(event.id)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter       = eventsAdapter
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
                val tempFile    =
                    File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
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

    private fun showPanel(panel: String) {
        binding.layoutScoring.root.visibility  = View.GONE
        binding.layoutHomeRun.root.visibility  = View.GONE
        binding.layoutCapture.root.visibility  = View.GONE
        binding.layoutWin.root.visibility      = View.GONE
        binding.layoutVoting.root.visibility   = View.GONE
        binding.layoutSummary.root.visibility  = View.GONE
        binding.layoutProgressBar.visibility   = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> { binding.layoutScoring.root.visibility = View.VISIBLE; setupScoringPanel() }
            "homeRun" -> { binding.layoutHomeRun.root.visibility = View.VISIBLE; setupTeamPlayerPanel(binding.layoutHomeRun, "HOME_RUN") }
            "capture" -> { binding.layoutCapture.root.visibility = View.VISIBLE; setupTeamPlayerPanel(binding.layoutCapture, "CAPTURE") }
            "win"     -> { binding.layoutWin.root.visibility     = View.VISIBLE; setupWinPanel()     }
            "voting"  ->   binding.layoutVoting.root.visibility  = View.VISIBLE
            "summary" ->   binding.layoutSummary.root.visibility = View.VISIBLE
            "loading" ->   binding.layoutProgressBar.visibility  = View.VISIBLE
        }
    }

    private fun setupScoringPanel() {
        updateScoreUI()

        if (!canEdit) {
            binding.layoutScoring.btnHomeRun.visibility      = View.GONE
            binding.layoutScoring.btnCapture.visibility      = View.GONE
            binding.layoutScoring.btnDeclareWinner.visibility = View.GONE
            binding.layoutScoring.btnEndMatch.visibility     = View.GONE
            binding.layoutScoring.btnUndo.visibility         = View.GONE
            return
        }

        binding.layoutScoring.btnHomeRun.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            resetWizard()
            showPanel("homeRun")
        }

        binding.layoutScoring.btnCapture.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            resetWizard()
            showPanel("capture")
        }

        binding.layoutScoring.btnDeclareWinner.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            showPanel("win")
        }

        binding.layoutScoring.btnEndMatch.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "END_MATCH"))
        }

        binding.layoutScoring.btnUndo.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("undo", true))
        }

        setScoringButtonsEnabled(!isActionPending)
    }

    private fun setupTeamPlayerPanel(
        panelBinding: com.example.fypproject.databinding.LayoutLudoTeamPlayerBinding,
        eventType: String
    ) {
        resetWizard()
        val title = when (eventType) {
            "HOME_RUN" -> "🏠 Record Home Run"
            "CAPTURE"  -> "⚔️ Record Capture"
            else       -> "Record Event"
        }
        panelBinding.tvTitle.text = title

        panelBinding.tvClose.setOnClickListener { showPanel("scoring") }

        panelBinding.layoutStep1.visibility = View.VISIBLE
        panelBinding.layoutStep2.visibility = View.GONE

        val teamNames = listOf("Select Team",
            matchResponse?.team1Name ?: "Team A",
            matchResponse?.team2Name ?: "Team B")
        val teamIds = listOf<Long?>(null, matchResponse?.team1Id, matchResponse?.team2Id)

        panelBinding.spinnerTeam.setup(teamNames) { pos ->
            selectedTeamId = teamIds[pos]
            if (selectedTeamId != null) {
                val players = if (selectedTeamId == matchResponse?.team1Id)
                    team1Players else team2Players
                panelBinding.spinnerPlayer.setupWithPlayers("Select Player (Optional)", players)
                panelBinding.layoutStep2.visibility = View.VISIBLE
            }
        }

        panelBinding.btnConfirm.setOnClickListener {
            val teamId = selectedTeamId
            if (teamId == null) { toast("Select Team"); return@setOnClickListener }
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            val json = JSONObject().put("eventType", eventType).put("teamId", teamId)
            val pid = panelBinding.spinnerPlayer.selectedId()
            if (pid != null) json.put("playerId", pid)
            sendEvent(json)
            showPanel("scoring")
        }

        panelBinding.btnSkipPlayer.setOnClickListener {
            val teamId = selectedTeamId
            if (teamId == null) { toast("Select Team"); return@setOnClickListener }
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", eventType).put("teamId", teamId))
            showPanel("scoring")
        }

        panelBinding.btnBack.setOnClickListener {
            panelBinding.layoutStep2.visibility = View.GONE
            selectedTeamId = null
        }
    }

    private fun setupWinPanel() {
        val w = binding.layoutWin
        w.tvClose.setOnClickListener { showPanel("scoring") }
        w.btnTeam1Win.text = "🔵 ${matchResponse?.team1Name ?: "Team A"}"
        w.btnTeam2Win.text = "🔴 ${matchResponse?.team2Name ?: "Team B"}"

        w.btnTeam1Win.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "WIN").put("teamId", matchResponse?.team1Id))
            showPanel("scoring")
        }
        w.btnTeam2Win.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "WIN").put("teamId", matchResponse?.team2Id))
            showPanel("scoring")
        }
        w.btnCancel.setOnClickListener { showPanel("scoring") }
    }

    private fun setScoringButtonsEnabled(enabled: Boolean) {
        if (_binding == null) return
        val alpha = if (enabled) 1f else 0.45f
        listOf(
            binding.layoutScoring.btnHomeRun,
            binding.layoutScoring.btnCapture,
            binding.layoutScoring.btnDeclareWinner,
            binding.layoutScoring.btnEndMatch,
            binding.layoutScoring.btnUndo
        ).forEach {
            it.isEnabled = enabled
            it.alpha     = alpha
        }
    }

    private fun resetWizard() {
        selectedTeamId  = null
        selectedPlayerId = null
    }

    private fun updateScoreUI() {
        if (_binding == null) return
        binding.tvTeam1HomeRuns.text = team1HomeRuns.toString()
        binding.tvTeam2HomeRuns.text = team2HomeRuns.toString()
        binding.tvTeam1Captures.text = "⚔️ $team1Captures captures"
        binding.tvTeam2Captures.text = "⚔️ $team2Captures captures"
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
                    binding.tvTimer.text = "${String.format("%02d:%02d", elapsed / 60, elapsed % 60)}"
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
            android.util.Log.d("LUDO_SOCKET", "Raw: $jsonString")
            activity?.runOnUiThread {
                try {
                    handleServerUpdate(JSONObject(jsonString))
                } catch (e: Exception) {
                    android.util.Log.e("LUDO_SOCKET", "Parse error: ${e.message}")
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

    // 3. onHiddenChanged ADD karo:
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

        team1HomeRuns = obj.optInt("team1HomeRuns", team1HomeRuns)
        team2HomeRuns = obj.optInt("team2HomeRuns", team2HomeRuns)
        team1Captures = obj.optInt("team1Captures", team1Captures)
        team2Captures = obj.optInt("team2Captures", team2Captures)

        val rawStatus = obj.optString("status", "")
        if (rawStatus.isNotEmpty() && rawStatus != "null") matchStatus = rawStatus

        val eventsArray = obj.optJSONArray("ludoEvents")
        if (eventsArray != null) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                val ev = eventsArray.getJSONObject(i)
                val eventType = ev.optString("eventType", "").ifEmpty { continue }

                fun JSONObject.safeString(key: String) =
                    if (isNull(key)) "" else optString(key, "")
                        .let { if (it == "null") "" else it }

                eventsList.add(0, LudoEvent(
                    id               = ev.optLong("id", System.currentTimeMillis()),
                    eventType        = eventType,
                    teamName         = ev.safeString("teamName").ifEmpty { null },
                    playerName       = ev.safeString("playerName").ifEmpty { null },
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

    private fun showLudoSummary() {
        if (_binding == null || !isAdded) return
        showPanel("summary")
        val s      = binding.layoutSummary
        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"
        s.tvTeam1Name.text     = t1Name
        s.tvTeam2Name.text     = t2Name
        s.tvTeam1HomeRuns.text = team1HomeRuns.toString()
        s.tvTeam2HomeRuns.text = team2HomeRuns.toString()
        s.tvTeam1Captures.text = "⚔️ $team1Captures captures"
        s.tvTeam2Captures.text = "⚔️ $team2Captures captures"
        s.tvMatchResult.text   = when {
            team1HomeRuns > team2HomeRuns -> "🏆 $t1Name Wins!"
            team2HomeRuns > team1HomeRuns -> "🏆 $t2Name Wins!"
            else -> "🤝 Match Draw!"
        }
    }

    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        val matchId   = matchResponse?.id ?: run { showLudoSummary(); return }
        val accountId = getAccountId()
        if (hasAlreadyVoted(matchId)) { showLudoSummary(); return }

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
        v.btnSkipVote.setOnClickListener { showLudoSummary() }
    }

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long, feedback: String? = null) {
        if (accountId == -1L) {
            toast("Account not found. Please login again.")
            showLudoSummary()   // ← apne fragment ka summary function yahan likh do
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
                        showLudoSummary()
                    }
                    response.code() == 409 -> {
                        markAsVoted(matchId)
                        toast("Already voted!")
                        showLudoSummary()
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

    private fun Spinner.setup(items: List<String>, onSelect: (Int) -> Unit) {
        adapter = makeAdapter(items)
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = onSelect(pos)
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
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
        fun newInstance(match: MatchResponse) = LudoScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}