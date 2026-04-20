package com.example.fypproject.TugOfWarFragment

import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.LudoEventAdapter
import com.example.fypproject.Adapter.TugOfWarEventAdapter
import com.example.fypproject.Adapter.VotePlayerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.TugOfWarEvent
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.TugofwarScoringFragmentBinding
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class TugOfWarScoringFragment : Fragment(R.layout.tugofwar_scoring_fragment) {

    private var _binding: TugofwarScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null

    private var team1Rounds    = 0
    private var team2Rounds    = 0
    private var currentRound   = 1
    private var roundsToWin    = 3
    private var totalRounds    = 5
    private var matchStatus    = "LIVE"
    private var roundStartTimeMs = 0L
    private var isActionPending  = false
    private var timerEverStarted = false

    private var confirmTeamId: Long? = null

    private var canEdit = false

    private var votingAlreadyTriggered = false
    private var selectedVotePlayerId: Long? = null
    private var selectedVotePlayerName = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null

    private val eventsList = mutableListOf<TugOfWarEvent>()
    private lateinit var eventsAdapter: TugOfWarEventAdapter

    private var timerTask: TimerTask? = null
    private val timer = Timer()

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
        _binding = TugofwarScoringFragmentBinding.bind(view)

        getBundleData()
        computeCanEdit()
        setupEventsRecycler()
        setupBottomTabs()
        setupSocketConnection()
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
            ContextCompat.getColor(requireContext(), if (isScoring) R.color.tab_active else R.color.tab_inactive)
        )
        binding.tabEvents.setTextColor(
            ContextCompat.getColor(requireContext(), if (!isScoring) R.color.tab_active else R.color.tab_inactive)
        )
        if (!isScoring) {
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
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

    private fun setupEventsRecycler() {
        eventsAdapter = TugOfWarEventAdapter(eventsList)
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter       = eventsAdapter
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

    private fun showPanel(panel: String) {
        binding.layoutScoring.root.visibility  = View.GONE
        binding.layoutConfirm.root.visibility  = View.GONE
        binding.layoutVoting.root.visibility   = View.GONE
        binding.layoutSummary.root.visibility  = View.GONE
        binding.layoutProgressBar.visibility   = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> { binding.layoutScoring.root.visibility = View.VISIBLE; setupScoringPanel() }
            "confirm" -> { binding.layoutConfirm.root.visibility = View.VISIBLE; setupConfirmPanel() }
            "voting"  ->   binding.layoutVoting.root.visibility  = View.VISIBLE
            "summary" ->   binding.layoutSummary.root.visibility = View.VISIBLE
            "loading" ->   binding.layoutProgressBar.visibility  = View.VISIBLE
        }
    }

    private fun setupScoringPanel() {
        updateScoreUI()
        updateRopeVisualization()

        if (!canEdit) {
            binding.layoutScoring.btnTeam1Win.visibility = View.GONE
            binding.layoutScoring.btnTeam2Win.visibility = View.GONE
            binding.layoutScoring.btnUndo.visibility     = View.GONE
            binding.layoutScoring.btnEndMatch.visibility = View.GONE
            return
        }

        binding.layoutScoring.btnTeam1Win.text =
            getString(R.string.team_won_round, "🔵", matchResponse?.team1Name ?: "Team A")
        binding.layoutScoring.btnTeam2Win.text =
            getString(R.string.team_won_round, "🔴", matchResponse?.team2Name ?: "Team B")

        binding.layoutScoring.btnTeam1Win.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            confirmTeamId = matchResponse?.team1Id
            showPanel("confirm")
        }

        binding.layoutScoring.btnTeam2Win.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            confirmTeamId = matchResponse?.team2Id
            showPanel("confirm")
        }

        binding.layoutScoring.btnUndo.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("undo", true))
        }

        binding.layoutScoring.btnEndMatch.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "END_MATCH"))
        }

        setScoringButtonsEnabled(!isActionPending)
    }

    private fun setupConfirmPanel() {
        val teamName = if (confirmTeamId == matchResponse?.team1Id)
            matchResponse?.team1Name ?: "Team A"
        else
            matchResponse?.team2Name ?: "Team B"

        binding.layoutConfirm.tvConfirmText.text =
            getString(R.string.confirm_round_win, teamName)

        binding.layoutConfirm.btnConfirmYes.setOnClickListener {
            val tId = confirmTeamId ?: return@setOnClickListener
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject()
                .put("eventType", "ROUND_WIN")
                .put("winnerTeamId", tId))
            confirmTeamId = null
            showPanel("scoring")
        }

        binding.layoutConfirm.btnConfirmCancel.setOnClickListener {
            confirmTeamId = null
            showPanel("scoring")
        }
    }

    private fun setScoringButtonsEnabled(enabled: Boolean) {
        if (_binding == null) return
        val alpha = if (enabled) 1f else 0.45f
        listOf(
            binding.layoutScoring.btnTeam1Win,
            binding.layoutScoring.btnTeam2Win,
            binding.layoutScoring.btnUndo,
            binding.layoutScoring.btnEndMatch
        ).forEach {
            it.isEnabled = enabled
            it.alpha     = alpha
        }
    }

    private fun updateScoreUI() {
        if (_binding == null) return
        binding.tvRoundScore.text  = getString(R.string.round_score, team1Rounds, team2Rounds)
        binding.tvRoundLabel.text  = getString(R.string.round_label, currentRound, totalRounds)
    }

    private fun updateRopeVisualization() {
        if (_binding == null) return
        val t1Pct = ((team1Rounds.toFloat() / roundsToWin) * 50).coerceAtMost(50f)
        val t2Pct = ((team2Rounds.toFloat() / roundsToWin) * 50).coerceAtMost(50f)

        val screenWidth = resources.displayMetrics.widthPixels
        val t1Width = ((t1Pct / 100f) * screenWidth).toInt()
        val t2Width = ((t2Pct / 100f) * screenWidth).toInt()

        binding.layoutScoring.viewTeam1Rope.layoutParams =
            binding.layoutScoring.viewTeam1Rope.layoutParams.apply { width = t1Width }
        binding.layoutScoring.viewTeam2Rope.layoutParams =
            binding.layoutScoring.viewTeam2Rope.layoutParams.apply { width = t2Width }
    }

    private fun startRoundTimer(startTime: Long) {
        timerTask?.cancel()
        roundStartTimeMs = startTime
        timerEverStarted = true

        activity?.runOnUiThread {
            if (_binding != null) binding.tvTimer.visibility = View.VISIBLE
        }

        timerTask = object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (_binding == null || roundStartTimeMs == 0L) return@runOnUiThread
                    val elapsed = (System.currentTimeMillis() - roundStartTimeMs) / 1000
                    binding.tvTimer.text = String.format(Locale.US, "%02d:%02d", elapsed / 60, elapsed % 60)
                }
            }
        }
        timer.schedule(timerTask, 0, 1000)
    }

    private fun setupSocketListeners() {
        WebSocketManager.socketStateListener = { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Connected -> { /* silent */ }
                    is SocketState.Error -> {
                        toast("Socket Error")
                        isActionPending = false
                        if (_binding != null &&
                            binding.layoutScoring.root.isVisible) {
                            setScoringButtonsEnabled(true)
                        }
                    }
                    is SocketState.Disconnected -> {
                        isActionPending = false
                        if (_binding != null &&
                            binding.layoutScoring.root.isVisible) {
                            setScoringButtonsEnabled(true)
                        }
                    }
                }
            }
        }

        WebSocketManager.messageListener = { jsonString ->
            android.util.Log.d("TOW_SOCKET", "Raw: $jsonString")
            activity?.runOnUiThread {
                try {
                    handleServerUpdate(JSONObject(jsonString))
                } catch (e: Exception) {
                    android.util.Log.e("TOW_SOCKET", "Parse error: ${e.message}")
                    isActionPending = false
                    if (_binding != null) setScoringButtonsEnabled(true)
                }
            }
        }
    }

    private fun setupSocketConnection() {
        setupSocketListeners()
        matchResponse?.id?.let { WebSocketManager.connect(it) }
    }

    private fun handleServerUpdate(obj: JSONObject) {
        if (_binding == null) return

        isActionPending = false

        team1Rounds  = obj.optInt("team1Rounds",  team1Rounds)
        team2Rounds  = obj.optInt("team2Rounds",  team2Rounds)
        currentRound = obj.optInt("currentRound", currentRound)
        roundsToWin  = obj.optInt("roundsToWin",  roundsToWin)
        totalRounds  = obj.optInt("totalRounds",  totalRounds)

        val rawStatus = obj.optString("status", "")
        if (rawStatus.isNotEmpty() && rawStatus != "null") matchStatus = rawStatus

        val eventsArray = obj.optJSONArray("tugOfWarEvents")
        if (eventsArray != null) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                val ev = eventsArray.getJSONObject(i)
                val eventType = ev.optString("eventType", "")
                if (eventType.isEmpty()) continue
                eventsList.add(0, TugOfWarEvent(
                    id                  = ev.optLong("id", System.currentTimeMillis()),
                    eventType           = eventType,
                    winnerTeamName      = ev.optString("winnerTeamName", "").ifEmpty { null },
                    roundNumber         = ev.optInt("roundNumber", currentRound),
                    roundDurationSeconds = ev.optInt("roundDurationSeconds", 0)
                        .takeIf { it > 0 }
                ))
            }
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
        }

        if (obj.has("roundStartTime") && !obj.isNull("roundStartTime")) {
            val start = obj.getLong("roundStartTime")
            if (start > 0 && (start != roundStartTimeMs || !timerEverStarted)) {
                startRoundTimer(start)
            }
        } else if (!timerEverStarted && matchStatus.uppercase() == "LIVE") {
            startRoundTimer(System.currentTimeMillis())
        }

        if (obj.optString("comment") == "UNDO") toast("↩ Undo successful")

        updateScoreUI()
        updateRopeVisualization()

        if (binding.layoutScoring.root.isVisible) {
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

    private fun showTugOfWarSummary() {
        if (_binding == null || !isAdded) return
        showPanel("summary")
        val s      = binding.layoutSummary
        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"
        s.tvTeam1Name.text   = t1Name
        s.tvTeam2Name.text   = t2Name
        s.tvTeam1Rounds.text = team1Rounds.toString()
        s.tvTeam2Rounds.text = team2Rounds.toString()
        s.tvMatchResult.text = when {
            team1Rounds > team2Rounds -> "💪 $t1Name Wins!"
            team2Rounds > team1Rounds -> "💪 $t2Name Wins!"
            else                      -> "🤝 Match Draw!"
        }
    }
    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        val matchId   = matchResponse?.id ?: run { showTugOfWarSummary(); return }
        val accountId = getAccountId()
        if (hasAlreadyVoted(matchId)) { showTugOfWarSummary(); return }

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
        v.btnSkipVote.setOnClickListener { showTugOfWarSummary() }
    }

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long) {
        if (accountId == -1L) { toast("Login again"); showTugOfWarSummary(); return }
        val v = binding.layoutVoting
        v.btnSubmitVote.isEnabled = false
        v.btnSubmitVote.text      = getString(R.string.submitting)
        v.btnSkipVote.isEnabled   = false
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.submitVote(matchId, accountId, playerId)
                }
                when {
                    response.isSuccessful   -> { markAsVoted(matchId); toast("Vote submitted!"); showTugOfWarSummary() }
                    response.code() == 409  -> { markAsVoted(matchId); toast("Already voted!"); showTugOfWarSummary() }
                    else -> {
                        toast("Vote failed")
                        v.btnSubmitVote.isEnabled = true
                        v.btnSubmitVote.text      = getString(R.string.submit_vote)
                        v.btnSkipVote.isEnabled   = true
                    }
                }
            } catch (_: Exception) {
                toast("Network error")
                v.btnSubmitVote.isEnabled = true
                v.btnSubmitVote.text      = getString(R.string.submit_vote)
                v.btnSkipVote.isEnabled   = true
            }
        }
    }
    private fun hasAlreadyVoted(matchId: Long) =
        requireActivity().getSharedPreferences("VotePrefs", MODE_PRIVATE)
            .getBoolean("voted_match_$matchId", false)

    private fun markAsVoted(matchId: Long) =
        requireActivity().getSharedPreferences("VotePrefs", MODE_PRIVATE)
            .edit().apply { putBoolean("voted_match_$matchId", true) }.apply()

    private fun getAccountId() =
        requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getLong("id", -1L)

    private fun toast(msg: String) = context?.toastShort(msg)

    override fun onResume() {
        super.onResume()
        setupSocketListeners()
        if (!WebSocketManager.isConnected()) {
            matchResponse?.id?.let { WebSocketManager.connect(it) }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerTask?.cancel()
        WebSocketManager.socketStateListener = null
        WebSocketManager.messageListener     = null
        WebSocketManager.disconnect()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = TugOfWarScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}