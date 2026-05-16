package com.example.fypproject.CricketFragment

import android.content.ContentValues
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.BatsmanAdapter
import com.example.fypproject.Adapter.BowlerAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.ScorecardResponse
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ScoreboardFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

class ScoreCardFragment : Fragment(R.layout.scoreboard_fragment) {

    private var _binding: ScoreboardFragmentBinding? = null
    private val binding get() = _binding!!

    private var matchResponse: MatchResponse? = null
    private var showingTeamA = true
    private val SOCKET_KEY = "ScoreCardFragment"

    private lateinit var batsmanAdapter: BatsmanAdapter
    private lateinit var bowlerAdapter: BowlerAdapter

    private var ScoreCardTeamA: ScorecardResponse? = null
    private var ScoreCardTeamB: ScorecardResponse? = null

    private var isExportingPdf = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ScoreboardFragmentBinding.bind(view)

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }

        binding.btnTeamA.text = matchResponse?.team1Name ?: "Team A"
        binding.btnTeamB.text = matchResponse?.team2Name ?: "Team B"

        // RecyclerViews
        binding.rvBatsmen.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBowlers.layoutManager = LinearLayoutManager(requireContext())
        batsmanAdapter = BatsmanAdapter()
        bowlerAdapter  = BowlerAdapter()
        binding.rvBatsmen.adapter = batsmanAdapter
        binding.rvBowlers.adapter = bowlerAdapter

        highlightTab(true)

        // ── Tab buttons ──────────────────────────────────────────────────────
        binding.btnTeamA.setOnClickListener {
            showingTeamA = true
            highlightTab(true)
            ScoreCardTeamA?.let { updateUI(it) } ?: run {
                showLoadingState()
                fetchScoreCard(true)
            }
        }

        binding.btnTeamB.setOnClickListener {
            showingTeamA = false
            highlightTab(false)
            ScoreCardTeamB?.let { updateUI(it) } ?: run {
                showLoadingState()
                fetchScoreCard(false)
            }
        }

        // ── PDF Export button ─────────────────────────────────────────────────
        // btnExportPdf is added to scoreboard_fragment.xml (see layout note below)
        binding.btnExportPdf.setOnClickListener {
            if (!isExportingPdf) exportScoreCardPdf()
        }

        fetchScoreCard(true)
    }

    // ── Scorecard fetch ───────────────────────────────────────────────────────

    private fun fetchScoreCard(isTeamA: Boolean) {
        val match   = matchResponse ?: return
        val matchId = match.id      ?: return
        val teamId  = if (isTeamA) match.team1Id else match.team2Id
        teamId ?: return

        if (isTeamA == showingTeamA) showLoadingState()

        lifecycleScope.launch {
            try {
                val response = api.getScoreCard(matchId, teamId)
                if (response.isSuccessful) {
                    val scorecard = response.body()
                    if (scorecard != null) {
                        if (isTeamA) ScoreCardTeamA = scorecard else ScoreCardTeamB = scorecard
                        if (isTeamA == showingTeamA) updateUI(scorecard)
                    } else {
                        if (isTeamA == showingTeamA) showEmptyState()
                    }
                } else {
                    requireContext().toastShort("HTTP Error: ${response.code()}")
                    if (isTeamA == showingTeamA) showEmptyState()
                }
            } catch (e: Exception) {
                requireContext().toastShort("Exception: ${e.message}")
                if (isTeamA == showingTeamA) showEmptyState()
            }
        }
    }

    // ── PDF Export ────────────────────────────────────────────────────────────

    private fun exportScoreCardPdf() {
        val matchId = matchResponse?.id ?: run {
            requireContext().toastShort("Match ID not found!")
            return
        }

        isExportingPdf = true
        binding.btnExportPdf.isEnabled = false
        binding.btnExportPdf.text = "Generating PDF…"

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.downloadScorecardPdf(matchId)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val savedUri = savePdfToDownloads(body, "scorecard-$matchId.pdf")
                        requireContext().toastShort("PDF saved to Downloads!")
                        // ✅ Share dialog show karo
                        savedUri?.let { showShareDialog(it) }
                    } else {
                        requireContext().toastShort("Empty PDF response")
                    }
                } else {
                    requireContext().toastShort("PDF download failed: ${response.code()}")
                }
            } catch (e: Exception) {
                requireContext().toastShort("Error: ${e.message}")
            } finally {
                isExportingPdf = false
                binding.btnExportPdf.isEnabled = true
                binding.btnExportPdf.text = "⬇ Export PDF"
            }
        }
    }

    /**
     * PDF save karo aur Uri return karo taake share ho sake
     */
    private suspend fun savePdfToDownloads(body: ResponseBody, fileName: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = requireContext().contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw Exception("Cannot create file in Downloads")

                    resolver.openOutputStream(uri)?.use { out ->
                        body.byteStream().copyTo(out)
                    }

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)

                    uri  // ✅ Uri return karo
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { out ->
                        body.byteStream().copyTo(out)
                    }
                    // Legacy ke liye FileProvider Uri
                    FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.provider",
                        file
                    )
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * WhatsApp direct + general share dono options show karo
     */
    private fun showShareDialog(pdfUri: Uri) {
        val ctx = requireContext()

        // WhatsApp installed hai?
        val whatsappInstalled = try {
            ctx.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) { false }

        val options = if (whatsappInstalled) {
            arrayOf("Share on WhatsApp", "Share via Other Apps")
        } else {
            arrayOf("Share via Other Apps")
        }

        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("Share Scorecard PDF")
            .setItems(options) { _, which ->
                when {
                    whatsappInstalled && which == 0 -> shareToWhatsApp(pdfUri)
                    else -> shareGeneral(pdfUri)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Directly WhatsApp pe share karo
     */
    private fun shareToWhatsApp(pdfUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            setPackage("com.whatsapp")           // sirf WhatsApp
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            requireContext().toastShort("WhatsApp not found!")
        }
    }

    /**
     * General share sheet (Gmail, Drive, Telegram, etc.)
     */
    private fun shareGeneral(pdfUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "Match Scorecard")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Scorecard PDF"))
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun updateUI(scorecard: ScorecardResponse) {
        hideLoadingState()
        hideEmptyState()
        showContentView()
        batsmanAdapter.updateData(scorecard.batsmanScores)
        bowlerAdapter.updateData(scorecard.bowlerScores)
        binding.tvExtras.text   = "Extras   ${scorecard.extras}"
        binding.tvTotal.text    = " Total    ${scorecard.totalRuns}"
        binding.tvOversInfo.text = "Overs    ${scorecard.overs}.${scorecard.balls}"
    }

    private fun showLoadingState() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.contentScrollView.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
        binding.progressLoading.indeterminateTintList =
            ColorStateList.valueOf(0xFFE31212.toInt())
    }

    private fun hideLoadingState() {
        binding.progressLoading.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressLoading.visibility = View.GONE
        binding.contentScrollView.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        binding.emptyStateContainer.visibility = View.GONE
    }

    private fun showContentView() {
        binding.contentScrollView.visibility = View.VISIBLE
    }

    private fun highlightTab(isTeamA: Boolean) {
        val activeColor   = 0xFFE31212.toInt()
        val inactiveColor = 0xFF333333.toInt()
        val activeText    = 0xFFFFFFFF.toInt()
        val inactiveText  = 0xFFAAAAAA.toInt()

        binding.btnTeamA.setBackgroundColor(if (isTeamA) activeColor else inactiveColor)
        binding.btnTeamA.setTextColor(if (isTeamA) activeText else inactiveText)
        binding.btnTeamB.setBackgroundColor(if (isTeamA) inactiveColor else activeColor)
        binding.btnTeamB.setTextColor(if (isTeamA) inactiveText else activeText)
    }

    // ── Socket ────────────────────────────────────────────────────────────────

    private fun registerSocketListeners() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                when (state) {
                    is SocketState.Error -> requireContext().toastShort("Socket Error: ${state.message}")
                    else -> {}
                }
            }
        }
    }

    fun onSocketUpdate() {
        if (_binding == null) return
        fetchScoreCard(showingTeamA)
    }

    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }

    override fun onResume() {
        super.onResume()
        registerSocketListeners()
        fetchScoreCard(showingTeamA)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) { registerSocketListeners(); fetchScoreCard(showingTeamA) }
        else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = ScoreCardFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}