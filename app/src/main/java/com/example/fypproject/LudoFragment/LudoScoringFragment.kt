package com.example.fypproject.LudoFragment

import android.content.Context.MODE_PRIVATE
import android.graphics.Color
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

class LudoScoringFragment : Fragment(R.layout.ludo_scoring_fragment) {

    private var _binding: LudoScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private var lastSocketJson: JSONObject? = null

    private var pendingComment: String? = null
    private val SOCKET_KEY = "LudoScoringFragment"

    private var team1HomeRuns = 0
    private var isCompletedAndWaitingForData = false
    private var team2HomeRuns = 0
    private var matchStatus   = "LIVE"
    private var isActionPending = false

    private var selectedTeamId: Long? = null
    private var canEdit = false

    private var votingAlreadyTriggered = false
    private var selectedVotePlayerId: Long? = null
    private var selectedVotePlayerName = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null

    private val eventsList = mutableListOf<LudoEvent>()
    private lateinit var eventsAdapter: LudoEventAdapter

    private var pendingEventId: Long? = null
    private var cameraImageUri: Uri?  = null
    private var isUploading           = false
    private var canAddMedia = false

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

        val status = matchResponse?.status?.uppercase().orEmpty()
        val isCompleted = status == "COMPLETED" || status == "MATCH_COMPLETE"

        if (isCompleted) {
            binding.scoringTabContent.visibility = View.VISIBLE
            binding.eventsTabContent.visibility  = View.GONE
            loadAndShowVotingThenSummary()
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
        val prefs       = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val role        = prefs.getString("role", "")?.trim().orEmpty()
        val username    = prefs.getString("username", "")?.trim().orEmpty()
        val scorer      = matchResponse?.scorerId?.trim().orEmpty()
        val mediaScorer = matchResponse?.mediaScorerUsername?.trim().orEmpty()
        canEdit = role.equals("ADMIN", true)
                || scorer.equals(username, true)
        canAddMedia= canEdit || mediaScorer.equals(username, true)

    }

    private fun setupBottomTabs() {
        if (!canEdit) binding.tabScoring.visibility = View.GONE
        binding.tabScoring.setOnClickListener { showTab("scoring") }
        binding.tabEvents.setOnClickListener  { showTab("events")  }
    }

    private fun setupEventsRecycler() {
        eventsAdapter = LudoEventAdapter(eventsList) { event ->
            if (canAddMedia) showMediaDialog(event.id)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventsAdapter
    }


    private fun showPanel(panel: String) {
        binding.layoutScoring.root.visibility  = View.GONE
        binding.layoutHomeRun.root.visibility  = View.GONE
        binding.layoutVoting.root.visibility   = View.GONE
        binding.layoutSummary.root.visibility  = View.GONE
        binding.layoutProgressBar.visibility   = View.GONE

        binding.layoutScoringHeader.visibility = when (panel) {
            "voting", "summary", "loading" -> View.GONE
            else -> View.VISIBLE
        }

        when (panel) {
            "scoring" -> { binding.layoutScoring.root.visibility = View.VISIBLE; setupScoringPanel() }
            "homeRun" -> { binding.layoutHomeRun.root.visibility = View.VISIBLE; setupHomeRunPanel() }
            "voting"  ->   binding.layoutVoting.root.visibility  = View.VISIBLE
            "summary" ->   binding.layoutSummary.root.visibility = View.VISIBLE
            "loading" ->   binding.layoutProgressBar.visibility  = View.VISIBLE
        }
    }

    private fun setupScoringPanel() {
        updateScoreUI()
        if (!canEdit) {
            binding.layoutScoring.btnHomeRun.visibility = View.GONE
            binding.layoutScoring.btnUndo.visibility    = View.GONE
            return
        }
        binding.layoutScoring.btnHomeRun.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            selectedTeamId = null
            showPanel("homeRun")
        }
        binding.layoutScoring.btnUndo.setOnClickListener {
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("undo", true))
        }
        setScoringButtonsEnabled(!isActionPending)
    }

    private fun setupHomeRunPanel() {
        val p = binding.layoutHomeRun
        p.tvTitle.text = "🏠 Whose Home Run?"
        p.tvClose.setOnClickListener { showPanel("scoring") }

        val t1Name = matchResponse?.team1Name ?: "Team A"
        val t2Name = matchResponse?.team2Name ?: "Team B"
        val t1Id   = matchResponse?.team1Id
        val t2Id   = matchResponse?.team2Id

        p.btnTeam1.text = "🏠 $t1Name"
        p.btnTeam2.text = "🏠 $t2Name"
        p.btnTeam1.setBackgroundColor(Color.parseColor("#CC0000"))
        p.btnTeam2.setBackgroundColor(Color.parseColor("#CC0000"))
        p.btnConfirm.isEnabled = false
        p.btnConfirm.alpha     = 0.5f
        selectedTeamId = null

        p.btnTeam1.setOnClickListener {
            selectedTeamId = t1Id
            p.btnTeam1.setBackgroundColor(Color.parseColor("#10B981"))
            p.btnTeam2.setBackgroundColor(Color.parseColor("#CC0000"))
            p.btnConfirm.isEnabled = true
            p.btnConfirm.alpha     = 1f
        }
        p.btnTeam2.setOnClickListener {
            selectedTeamId = t2Id
            p.btnTeam2.setBackgroundColor(Color.parseColor("#10B981"))
            p.btnTeam1.setBackgroundColor(Color.parseColor("#CC0000"))
            p.btnConfirm.isEnabled = true
            p.btnConfirm.alpha     = 1f
        }
        p.btnConfirm.setOnClickListener {
            val teamId = selectedTeamId ?: return@setOnClickListener
            if (isActionPending) return@setOnClickListener
            isActionPending = true
            setScoringButtonsEnabled(false)
            sendEvent(JSONObject().put("eventType", "HOME_RUN").put("teamId", teamId))
            showPanel("scoring")
        }
    }

    private fun setScoringButtonsEnabled(enabled: Boolean) {
        if (_binding == null) return
        val alpha = if (enabled) 1f else 0.45f
        listOf(
            binding.layoutScoring.btnHomeRun,
            binding.layoutScoring.btnUndo
        ).forEach {
            it.isEnabled = enabled
            it.alpha     = alpha
        }
    }

    private fun updateScoreUI() {
        if (_binding == null) return
        binding.tvTeam1HomeRuns.text = team1HomeRuns.toString()
        binding.tvTeam2HomeRuns.text = team2HomeRuns.toString()
    }


    private fun setupSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Error, is SocketState.Disconnected -> {
                        isActionPending = false
                        if (_binding != null &&
                            binding.layoutScoring.root.visibility == View.VISIBLE)
                            setScoringButtonsEnabled(true)
                    }
                    else -> {}
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

    private fun setupSocketConnection() = setupSocketListeners()

    private fun handleServerUpdate(obj: JSONObject) {
        if (_binding == null) return
        lastSocketJson = obj
        isActionPending = false

        team1HomeRuns = obj.optInt("team1HomeRuns", team1HomeRuns)
        team2HomeRuns = obj.optInt("team2HomeRuns", team2HomeRuns)

        val rawStatus = obj.optString("status", "")
        if (rawStatus.isNotEmpty() && rawStatus != "null") matchStatus = rawStatus

        val eventsArray = obj.optJSONArray("ludoEvents")
        if (eventsArray != null) {
            eventsList.clear()
            for (i in 0 until eventsArray.length()) {
                val ev        = eventsArray.getJSONObject(i)
                val eventType = ev.optString("eventType", "").ifEmpty { continue }
                fun JSONObject.safeStr(key: String) =
                    if (isNull(key)) "" else optString(key, "").let { if (it == "null") "" else it }
                eventsList.add(0, LudoEvent(
                    id               = ev.optLong("id", System.currentTimeMillis()),
                    eventType        = eventType,
                    teamName         = ev.safeStr("teamName").ifEmpty { null },
                    playerName       = ev.safeStr("playerName").ifEmpty { null },
                    eventTimeSeconds = ev.optInt("eventTimeSeconds", 0).takeIf { it > 0 }
                ))
            }
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
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
        s.tvMatchResult.text   = when {
            team1HomeRuns > team2HomeRuns -> "🏆 $t1Name Wins!"
            team2HomeRuns > team1HomeRuns -> "🏆 $t2Name Wins!"
            else -> "🤝 Match Draw!"
        }
    }


    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return
        binding.scoringTabContent.visibility = View.VISIBLE
        binding.eventsTabContent.visibility  = View.GONE
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
                    else voteAdapter1?.clearSelection()
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

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long) {
        if (accountId == -1L) { toast("Account not found."); showLudoSummary(); return }
        val v = binding.layoutVoting
        v.btnSubmitVote.isEnabled = false
        v.btnSubmitVote.text      = "Submitting…"
        v.btnSkipVote.isEnabled   = false

        val body = buildMap<String, Any?> {
            put("matchId",   matchId)
            put("accountId", accountId)
            put("playerId",  playerId)
        }
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { RetrofitInstance.api.submitVote(body) }
                when {
                    response.isSuccessful   -> { markAsVoted(matchId); toast("Vote submitted!"); showLudoSummary() }
                    response.code() == 409  -> { markAsVoted(matchId); toast("Already voted!"); showLudoSummary() }
                    else -> {
                        toast("Vote failed (${response.code()}).")
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
        val etComment  = dialogView.findViewById<android.widget.EditText>(R.id.etMediaComment)
        val btnCamera  = dialogView.findViewById<View>(R.id.btnOpenCamera)
        val btnGallery = dialogView.findViewById<View>(R.id.btnOpenGallery)
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btnCancelMedia)

        btnCamera.setOnClickListener {
            pendingComment = etComment.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            dialog.dismiss(); openCamera()
        }
        btnGallery.setOnClickListener {
            if (!isUploading) {
                pendingComment = etComment.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                dialog.dismiss(); galleryLauncher.launch("image/*")
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
                val response    = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.createMedia(matchIdBody, eventIdBody, filePart, commentBody)
                }
                if (response.isSuccessful) toast("Upload Successful!")
                else toast("Upload failed: ${response.code()}")
            } catch (e: Exception) {
                toast("Upload failed: ${e.message}")
            } finally {
                isUploading = false; pendingEventId = null; pendingComment = null
            }
        }
    }


    private fun showTab(tab: String) {
        val isScoring = tab == "scoring"
        binding.scoringTabContent.visibility = if (isScoring) View.VISIBLE else View.GONE
        binding.eventsTabContent.visibility  = if (isScoring) View.GONE    else View.VISIBLE
        binding.tabScoring.setTextColor(
            if (isScoring) Color.parseColor("#E31212") else Color.parseColor("#888888"))
        binding.tabEvents.setTextColor(
            if (!isScoring) Color.parseColor("#E31212") else Color.parseColor("#888888"))
        if (!isScoring) {
            binding.tvNoEvents.visibility =
                if (eventsList.isEmpty()) View.VISIBLE else View.GONE
            eventsAdapter.notifyDataSetChanged()
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

    private fun getAccountId() =
        requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE).getLong("id", -1L)

    private fun toast(msg: String) = context?.toastShort(msg)


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden){
            setupSocketListeners()
            lastSocketJson?.let { handleServerUpdate(it) }
        } else unregisterSocketListeners()
    }

    override fun onResume()  {
        super.onResume();
        setupSocketListeners()
        lastSocketJson?.let { handleServerUpdate(it) }
    }
    override fun onPause()   { super.onPause();   unregisterSocketListeners() }
    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = LudoScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}