package com.example.fypproject.CricketFragment

import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
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

class ScoringFragment : Fragment(R.layout.scoring_fragment) {

    private var _binding: ScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    private var innings: Int = 1
    private var inningsId: Long? = null
    private var isInningsInitialized = false
    private var isBallPending = false

    private var selectedVotePlayerId: Long?    = null
    private var selectedVotePlayerName: String = ""
    private var voteAdapter1: VotePlayerAdapter? = null
    private var voteAdapter2: VotePlayerAdapter? = null

   // private var penaltyTeamId: Long? = null
    private var lastReceivedScore: ScoreDTO? = null

    private var team1Id: Long = -1L
    private var team2Id: Long = -1L
    private var team1Name: String = ""
    private var team2Name: String = ""
    private var battingTeamId: Long = -1L
    private var bowlingTeamId: Long = -1L
    private var battingTeamName: String = ""

    private var currentStrikerId: Long? = null
    private var currentNonStrikerId: Long? = null
    private var currentBowlerId: Long? = null

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
    private var penaltyTeamId: Long? = null
    private var pendingBallId: Long? = null
    private var cameraImageUri: Uri? = null
    private var isUploading = false

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
            setupSocketConnection()
        }

        canEdit = computeCanEdit(matchResponse)

        if (canEdit) {
            showOnly(binding.layoutSelectPlayer.root)
            setupAdminSelectionFlow()
            setupScoringPanel()
            setupExtrasPanels()
            setupWicketPanel()
            setupMorePanel()
        } else {
            showOnly(binding.layoutUserHistory)
        }
    }

    override fun onResume() {
        super.onResume()
        matchResponse?.id?.toLong()?.let { WebSocketManager.connect(it) }
        if (_binding != null && canEdit && b1Selected && b2Selected && bowlerSelected) {
            showOnly(binding.layoutMainScoring.root)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        summaryPollingJob?.cancel()
        WebSocketManager.socketStateListener = null
        WebSocketManager.messageListener = null
        WebSocketManager.disconnect()
        _binding = null
    }
    private fun computeCanEdit(match: MatchResponse?): Boolean {
        val prefs = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val role = prefs.getString("role", "")?.trim().orEmpty()
        val username = prefs.getString("username", "")?.trim().orEmpty()
        val scorer = match?.scorerId?.trim().orEmpty()
        return role.equals("ADMIN", true) || scorer.equals(username, true)
    }

    private fun calculateTeams(match: MatchResponse) {
        team1Id = match.team1Id ?: -1L
        team2Id = match.team2Id ?: -1L
        team1Name = match.team1Name ?: ""
        team2Name = match.team2Name ?: ""
        isInningsInitialized = true

        if (match.decision == "BAT") {
            battingTeamId = match.tossWinnerId ?: -1L
            battingTeamName = if (battingTeamId == team1Id) team1Name else team2Name
            bowlingTeamId = if (battingTeamId == team1Id) team2Id else team1Id
        } else {
            val tossWinnerId = match.tossWinnerId ?: -1L
            battingTeamId = if (tossWinnerId == team1Id) team2Id else team1Id
            battingTeamName = if (battingTeamId == team1Id) team1Name else team2Name
            bowlingTeamId = tossWinnerId
        }
    }
    private fun setupSocketConnection() {
        matchResponse?.id?.let { _ ->
            WebSocketManager.socketStateListener = { state ->
                android.util.Log.d("SOCKET_RAW", "📥 Raw: $state")
                activity?.runOnUiThread {
                    when (state) {
                        is SocketState.Connected    -> requireContext().toastShort("Live Connected!")
                        is SocketState.Error        -> requireContext().toastShort("Socket Error: ${state.message}")
                        is SocketState.Disconnected -> {}
                    }
                }
            }
            WebSocketManager.messageListener = { jsonString ->
                val updatedScore = JsonConverter.fromJson(jsonString)
                println("📥 Received JSON: $jsonString")
                updatedScore?.let {
                    activity?.runOnUiThread { updateScoreboardUI(it) }
                }
            }
            matchResponse?.id?.toLong()?.let { WebSocketManager.connect(it) }
        }
    }
   private fun updateScoreboardUI(score: ScoreDTO) {
        if (_binding == null || !isAdded) return
       isBallPending = false
       setScoringPanelEnabled(true)
        lastReceivedScore = score
       if (binding.layoutMatchSummary.root.visibility == View.VISIBLE) {
           return
       }
        if (score.inningsId != null && score.inningsId != -1L) {

            this.inningsId=score.inningsId
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
    }
    private fun handleModalLogic(score: ScoreDTO) {
        if (!canEdit) return

        if (score.comment == "End_Innings" || score.eventType == "End_Innings") {
            showOnly(binding.layoutInningsUndo.root)
            return
        }

        if (score.status == "COMPLETED" || score.status == "MATCH_COMPLETE") {
            loadAndShowVotingThenSummary()
            return
        }

        if (!score.firstInnings && isFirstInnings) {
            switchInnings()
            return
        }

        if (score.balls == 0 && score.overs == 0 && score.wickets == 0 && score.runs == 0) {
            if (!b1Selected || !b2Selected || !bowlerSelected) {
                showOnly(binding.layoutSelectPlayer.root)
            }
            return
        } else {
            if (b1Selected && b2Selected && bowlerSelected) {
                if (binding.layoutSelectPlayer.root.visibility == View.VISIBLE ||
                    binding.layoutSelectBowler.root.visibility == View.VISIBLE
                ) {
                    showOnly(binding.layoutMainScoring.root)
                }
            }
        }

        if (score.balls == 0 && score.overs > 0) {
            if (bowlerSelected) {
                bowlerSelected = false
                currentBowlerId = null
                binding.tvBowlerName.text = "Select Bowler"
                resetBowlerUI()
                showOnly(binding.layoutSelectBowler.root)
                binding.layoutSelectBowler.btnSelectBowler.text = "Select Next Bowler"
                requireContext().toastShort("Over complete! Select next bowler.")
            }
            return
        }
        if (score.status == "SUPER_OVER") {
            resetForNewInnings()
            showOnly(binding.layoutSelectPlayer.root)
            return
        }
    }
    private fun switchInnings() {
        if (!isFirstInnings) return

        val tempTeamId = battingTeamId
        battingTeamId  = bowlingTeamId
        bowlingTeamId  = tempTeamId
        battingTeamName = if (battingTeamId == team1Id) team1Name else team2Name

        isFirstInnings = false

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
    private fun checkInningsComplete(score: ScoreDTO) {
        if (score.firstInnings != isFirstInnings) return

        val totalOvers = matchResponse?.overs ?: return
        val oversComplete = score.overs >= totalOvers && score.balls == 0 && score.overs > 0
        val wicketsComplete = score.wickets >= 10
        val targetChased = !score.firstInnings && score.target > 0 && score.runs >= score.target

        if ((oversComplete || wicketsComplete || targetChased) &&
            score.eventType != "End_Innings" &&
            score.comment != "End_Innings"
        ) {
            if (binding.layoutInningsUndo.root.visibility != View.VISIBLE) {
                showOnly(binding.layoutInningsUndo.root)
            }
        }
    }

    private fun normalizeStats(score: ScoreDTO): ScoreDTO {
        val stats1 = score.batsman1Stats
        val stats2 = score.batsman2Stats

        if (score.eventType == "wicket" ||
            stats1?.playerId == null || stats1.playerId!! <= 0 ||
            stats2?.playerId == null || stats2.playerId!! <= 0
        ) {
            if (stats1?.playerId != null && stats1.playerId!! > 0) row1PlayerId = stats1.playerId
            if (stats2?.playerId != null && stats2.playerId!! > 0) row2PlayerId = stats2.playerId
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
            return score.apply {
                val temp = this.batsman1Stats
                this.batsman1Stats = this.batsman2Stats
                this.batsman2Stats = temp
            }
        }

        if (stats1.playerId != null &&
            stats1.playerId != row1PlayerId && stats1.playerId != row2PlayerId) {
            row1PlayerId = stats1.playerId
            return score
        }

        if (stats2.playerId != null &&
            stats2.playerId != row1PlayerId && stats2.playerId != row2PlayerId) {
            row2PlayerId = stats2.playerId
            return score
        }

        return score
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
        val container  = binding.ballContainer
        val scrollView = binding.ballScrollView
        val allBalls   = score.cricketBalls ?: return

        val sortedBalls = allBalls.sortedBy { it.id ?: 0L }
        if (sortedBalls.size == displayedBalls.size && sortedBalls == displayedBalls) return

        displayedBalls.clear()
        displayedBalls.addAll(sortedBalls)
        container.removeAllViews()

        for (ball in sortedBalls) {
            val ballView = BallViewHelper.createBallView(requireContext(), ball)
            ballView.setOnClickListener {
                ball.id?.let { ballId -> showMediaDialog(ballId) }
            }

            container.addView(ballView)
        }
        scrollView.post { scrollView.fullScroll(View.FOCUS_RIGHT) }
    }

    private fun showMediaDialog(ballId: Long) {
        pendingBallId = ballId

        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_media_source, null)

        val btnCamera  = dialogView.findViewById<View>(R.id.btnOpenCamera)
        val btnGallery = dialogView.findViewById<View>(R.id.btnOpenGallery)
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btnCancelMedia)
        val tvGallery  = dialogView.findViewById<TextView>(R.id.tvGalleryLabel)

        if (isUploading) tvGallery.text = "Uploading..."

        btnCamera.setOnClickListener {
            dialog.dismiss()
            openCamera()
        }

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

                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.createMedia(matchIdBody, ballIdBody, filePart)
                }

                if (response.isSuccessful) {
                    requireContext().toastShort("Upload Successful!")
                } else {
                    requireContext().toastShort("Upload failed: ${response.code()}")
                }

            } catch (e: Exception) {
                requireContext().toastShort("Upload failed: ${e.message}")
            } finally {
                isUploading   = false
                pendingBallId = null
            }
        }
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
            btnEndInnings.setOnClickListener { showOnly(binding.layoutEndInnings.root) }
            btnDLS.setOnClickListener        { showOnly(binding.layoutDLS.root)        }
            btnSuperOver.setOnClickListener  { showOnly(binding.layoutSuperOver.root)  }
            btnAbandon.setOnClickListener    { showAbandonConfirmationDialog()         }
            btnPenalty.setOnClickListener    { resetPenaltyPanel(); showOnly(binding.layoutPenalty.root) }
        }
        binding.layoutEndInnings.btnCloseEndInnings.setOnClickListener { showOnly(binding.layoutMainScoring.root) }
        binding.layoutDLS.btnCloseDLS.setOnClickListener               { showOnly(binding.layoutMainScoring.root) }
        binding.layoutSuperOver.btnCloseSuperOver.setOnClickListener   { showOnly(binding.layoutMainScoring.root) }
        binding.layoutAbandon.btnCloseAbandon.setOnClickListener       { showOnly(binding.layoutMainScoring.root) }
        binding.layoutPenalty.btnClosePenalty.setOnClickListener       { showOnly(binding.layoutMainScoring.root) }

        setupEndInningsAction()
        setupDLSAction()
        setupSuperOverAction()
        setupPenaltyAction()
    }

    private fun setupEndInningsAction() {
        binding.layoutEndInnings.btnConfirmEndInnings.setOnClickListener {
            triggerEndInnings()
        }

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

            if (!isFirstInnings) {
                loadAndShowVotingThenSummary()
            } else {
                JsonConverter.sendScore(scoreToSend.apply {
                    this.eventType = "End_Innings"
                    this.comment   = null
                    this.undo      = false
                })
                showOnly(binding.layoutMainScoring.root)
            }
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
        val prefs = requireActivity().getSharedPreferences("VotePrefs", MODE_PRIVATE)
        return prefs.getBoolean("voted_match_$matchId", false)
    }

    private fun markAsVoted(matchId: Long) {
        requireActivity()
            .getSharedPreferences("VotePrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("voted_match_$matchId", true)
            .apply()
    }
    private fun getAccountId(): Long {
        val prefs = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return prefs.getLong("id", -1L)
    }
    private fun loadAndShowSummary() {
        if (_binding == null || !isAdded) return


        if (summaryPollingJob?.isActive == true) return

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

        summaryPollingJob = lifecycleScope.launch {
            val maxRetries   = 40
            val retryDelayMs = 20_000L

            repeat(maxRetries) { attempt ->
                if (_binding == null || !isAdded) return@launch

                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getMatchSummary(matchId)
                    }

                    if (response.isSuccessful) {
                        val data = response.body()
                        if (data != null) {
                            activity?.runOnUiThread { bindSummaryData(data) }
                            return@launch
                        }
                    }

                    if (attempt < maxRetries - 1) {
                        activity?.runOnUiThread {
                            if (_binding != null) {
                            }
                        }
                        kotlinx.coroutines.delay(retryDelayMs)
                    } else {
                        showSummaryError("Results are taking longer than expected. Please try again shortly.")
                    }

                } catch (e: Exception) {
                    if (attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay(retryDelayMs)
                    } else {
                        showSummaryError("Error loading summary: ${e.message}")
                    }
                }
            }
        }
    }
    private fun loadAndShowVotingThenSummary() {
        if (_binding == null || !isAdded) return

        val matchId   = matchResponse?.id ?: run { loadAndShowSummary(); return }
        val accountId = getAccountId()

        if (hasAlreadyVoted(matchId)) {
            loadAndShowSummary()
            return
        }

        val v = binding.layoutVoting
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

                v.rvVoteTeam1.layoutManager =
                    LinearLayoutManager(requireContext())
                v.rvVoteTeam1.adapter = voteAdapter1

                v.rvVoteTeam2.layoutManager =
                    LinearLayoutManager(requireContext())
                v.rvVoteTeam2.adapter = voteAdapter2

            } catch (e: Exception) {
                requireContext().toastShort("Could not load players: ${e.message}")
            }
        }

        v.btnSubmitVote.setOnClickListener {
            val playerId = selectedVotePlayerId ?: return@setOnClickListener
            submitVote(matchId, accountId, playerId)
        }

        v.btnSkipVote.setOnClickListener {
            loadAndShowSummary()
        }
    }

    private fun submitVote(matchId: Long, accountId: Long, playerId: Long) {
        if (accountId == -1L) {
            requireContext().toastShort("Account not found. Please login again.")
            loadAndShowSummary()
            return
        }

        val v = binding.layoutVoting
        v.btnSubmitVote.isEnabled = false
        v.btnSubmitVote.text      = "Submitting…"
        v.btnSkipVote.isEnabled   = false

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.submitVote(
                        matchId   = matchId.toLong(),
                        accountId = accountId,
                        playerId  = playerId
                    )
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
            val oversText  = binding.layoutDLS.etDLSOvers.text.toString().trim()

            if (targetText.isEmpty() || oversText.isEmpty()) {
                requireContext().toastShort("Enter both target and overs"); return@setOnClickListener
            }
            val revisedTarget = targetText.toIntOrNull()
            val revisedOvers  = oversText.toDoubleOrNull()
            if (revisedTarget == null || revisedOvers == null || revisedTarget <= 0 || revisedOvers <= 0) {
                requireContext().toastShort("Enter valid numbers"); return@setOnClickListener
            }

            JsonConverter.sendScore(ScoreDTO().apply {
                this.matchId      = matchResponse?.id
                this.inningsId    = currentInningsId()
                this.teamId       = battingTeamId
                this.eventType    = "dls"
                this.event        = "dls"
                this.target       = revisedTarget
                this.status       = "LIVE"
                this.firstInnings = isFirstInnings
            })
            binding.layoutDLS.etDLSTarget.text?.clear()
            binding.layoutDLS.etDLSOvers.text?.clear()
            showOnly(binding.layoutMainScoring.root)
        }
    }

    private fun setupSuperOverAction() {
        binding.layoutSuperOver.btnConfirmSuperOver.setOnClickListener {
            JsonConverter.sendScore(ScoreDTO().apply {
                this.matchId   = matchResponse?.id
                this.inningsId = currentInningsId()
                this.eventType = "super_over"
                this.event     = "super_over"
                this.status    = "SUPER_OVER"
            })
            showOnly(binding.layoutSelectPlayer.root)
            resetForNewInnings()
            requireContext().toastShort("Super Over started! Select players.")
        }
    }

    private fun setupPenaltyAction() {
        binding.layoutPenalty.apply {
            btnPenaltyBatting.setOnClickListener {
                penaltyTeamId = battingTeamId
                tvPenaltyTeamSelected.text = "Awarded to: $battingTeamName (Batting)"
                tvPenaltyTeamSelected.setTextColor(0xFF4CAF50.toInt())
                btnPenaltyBatting.strokeColor = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
                btnPenaltyBowling.strokeColor = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
                btnConfirmPenalty.isEnabled = true
            }
            btnPenaltyBowling.setOnClickListener {
                penaltyTeamId = bowlingTeamId
                val bowlingName = if (bowlingTeamId == team1Id) team1Name else team2Name
                tvPenaltyTeamSelected.text = "Awarded to: $bowlingName (Bowling)"
                tvPenaltyTeamSelected.setTextColor(0xFF4CAF50.toInt())
                btnPenaltyBowling.strokeColor = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
                btnPenaltyBatting.strokeColor = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
                btnConfirmPenalty.isEnabled = true
            }
            btnConfirmPenalty.setOnClickListener {
                val teamId = penaltyTeamId ?: return@setOnClickListener
                val runs = etPenaltyRuns.text.toString().trim().toIntOrNull()
                if (runs == null || runs <= 0) {
                    requireContext().toastShort("Enter valid penalty runs"); return@setOnClickListener
                }
                JsonConverter.sendScore(ScoreDTO().apply {
                    this.matchId       = matchResponse?.id
                    this.inningsId     = currentInningsId()
                    this.teamId        = teamId
                    this.eventType     = "penalty"
                    this.event         = runs.toString()
                    this.runsOnThisBall = runs
                    this.isLegal       = false
                    this.status        = "LIVE"
                    this.firstInnings  = isFirstInnings
                })
                showOnly(binding.layoutMainScoring.root)
                requireContext().toastShort("Penalty $runs runs awarded!")
            }
        }
    }

    private fun resetPenaltyPanel() {
        penaltyTeamId = null
        binding.layoutPenalty.apply {
            tvPenaltyTeamSelected.text = "No team selected"
            tvPenaltyTeamSelected.setTextColor(0xFFAAAAAA.toInt())
            etPenaltyRuns.text?.clear()
            btnConfirmPenalty.isEnabled = false
            btnPenaltyBatting.strokeColor = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            btnPenaltyBowling.strokeColor = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
        }
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
                    activity?.runOnUiThread {
                        binding.root.visibility = View.GONE
                        requireContext().toastLong("Match Abandoned Successfully!")
                        requireActivity().onBackPressed()
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
        binding.layoutCaught.apply {
            btnSelectFielderCaught.setOnClickListener {
                openFielderSelectionDialog { selectedFielder ->
                    wicketFielderId = selectedFielder.id
                    tvSelectedFielderCaught.text = "Fielder: ${selectedFielder.name}"
                    tvSelectedFielderCaught.setTextColor(0xFF4CAF50.toInt())
                    btnConfirmCaught.isEnabled = true
                }
            }
            btnConfirmCaught.setOnClickListener {
                openNewBatsmanDialog("caught", currentStrikerId!!, wicketFielderId, 0)
            }
            btnCloseCaught.setOnClickListener { resetCaughtPanel(); showOnly(binding.layoutMainScoring.root) }
        }
        binding.layoutStumped.apply {
            btnSelectFielderStumped.setOnClickListener {
                openFielderSelectionDialog { selectedFielder ->
                    wicketFielderId = selectedFielder.id
                    tvSelectedFielderStumped.text = "Keeper: ${selectedFielder.name}"
                    tvSelectedFielderStumped.setTextColor(0xFF4CAF50.toInt())
                    btnConfirmStumped.isEnabled = true
                }
            }
            btnConfirmStumped.setOnClickListener {
                openNewBatsmanDialog("stumped", currentStrikerId!!, wicketFielderId, 0)
            }
            btnCloseStumped.setOnClickListener { resetStumpedPanel(); showOnly(binding.layoutMainScoring.root) }
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
                openNewBatsmanDialog("runout", outId, wicketFielderId, wicketRunOutRuns)
            }
            btnCloseRunOut.setOnClickListener { resetRunOutPanel(); showOnly(binding.layoutMainScoring.root) }
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

    private fun updateRunOutDoneButton() {
        binding.layoutRunOut.btnConfirmRunOut.isEnabled =
            wicketOutPlayerId != null && wicketFielderId != null
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

    private fun openNewBatsmanDialog(
        dismissalType: String,
        outPlayerId: Long,
        fielderId: Long?,
        runsOnBall: Int
    ) {
        val dialog     = android.app.AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_player_selection, null)
        val rvPlayers  = dialogView.findViewById<RecyclerView>(R.id.rvPlayersList)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmSelection)
        val btnClose   = dialogView.findViewById<Button>(R.id.btnClosePlayer)
        rvPlayers.layoutManager = LinearLayoutManager(context)
        btnClose.setOnClickListener { dialog.dismiss() }

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

    private fun sendWicketEvent(
        dismissalType: String,
        outPlayerId: Long,
        newBatsmanId: Long,
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
            this.newPlayerId    = newBatsmanId
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

        if (outPlayerId == currentStrikerId) {
            currentStrikerId = newBatsmanId
            row1PlayerId     = newBatsmanId
        } else if (outPlayerId == currentNonStrikerId) {
            currentNonStrikerId = newBatsmanId
            row2PlayerId        = newBatsmanId
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
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getPlayersByTeam(teamId)
                }
                if (response.isSuccessful) {
                    val players = response.body() ?: emptyList()
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

    companion object {
        fun newInstance(match: MatchResponse): ScoringFragment {
            return ScoringFragment().apply {
                arguments = Bundle().apply { putSerializable("match_response", match) }
            }
        }
    }
}