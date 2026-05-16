package com.example.fypproject.CricketFragment

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.fypproject.Adapter.MediaGridAdapter
import com.example.fypproject.ScoringDTO.MediaItem
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.Utils.toastShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class BallMediaBottomSheet : BottomSheetDialogFragment() {

    // ── Args ──────────────────────────────────────────────────────────────────
    private var ballId: Long    = -1L
    private var matchId: Long   = -1L
    private var accountId: Long = -1L

    // ── State ─────────────────────────────────────────────────────────────────
    private var mediaList: List<MediaItem> = emptyList()
    private var favIds: MutableSet<Long>   = mutableSetOf()
    private var pendingUri: Uri?           = null
    private var cameraUri: Uri?            = null
    private var isUploading                = false
    private var currentStep                = STEP_GALLERY

    // ── View refs ─────────────────────────────────────────────────────────────
    private lateinit var rvMedia: RecyclerView
    private lateinit var progressMedia: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var stepGallery: View
    private lateinit var stepSource: View
    private lateinit var stepComment: View
    private lateinit var etComment: EditText
    private lateinit var tvFileName: TextView
    private lateinit var btnUpload: Button
    private lateinit var tvMediaCount: TextView
    private lateinit var mediaAdapter: MediaGridAdapter

    // ── Launchers ─────────────────────────────────────────────────────────────

    // Photo camera
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) { pendingUri = cameraUri; goTo(STEP_COMMENT) }
        }

    // ✅ Video camera — ab properly call hoga
    private val videoLauncher =
        registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            if (success) { pendingUri = cameraUri; goTo(STEP_COMMENT) }
        }

    // ✅ Gallery — image + video dono
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) { pendingUri = uri; goTo(STEP_COMMENT) }
        }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_ball_media, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ballId    = arguments?.getLong(ARG_BALL_ID,    -1L) ?: -1L
        matchId   = arguments?.getLong(ARG_MATCH_ID,   -1L) ?: -1L
        accountId = arguments?.getLong(ARG_ACCOUNT_ID, -1L) ?: -1L

        bindViews(view)
        setupAdapter()
        goTo(STEP_GALLERY)
        fetchBallMedia()
    }

    private fun bindViews(v: View) {
        rvMedia       = v.findViewById(R.id.rvBallMedia)
        progressMedia = v.findViewById(R.id.progressBallMedia)
        layoutEmpty   = v.findViewById(R.id.layoutBallMediaEmpty)
        stepGallery   = v.findViewById(R.id.stepGallery)
        stepSource    = v.findViewById(R.id.stepSource)
        stepComment   = v.findViewById(R.id.stepComment)
        etComment     = v.findViewById(R.id.etBallComment)
        tvFileName    = v.findViewById(R.id.tvBallFileName)
        btnUpload     = v.findViewById(R.id.btnBallUpload)
        tvMediaCount  = v.findViewById(R.id.tvBallMediaCount)

        // Gallery step
        v.findViewById<Button>(R.id.btnGoToUpload)
            .setOnClickListener { goTo(STEP_SOURCE) }

        // Source step
        v.findViewById<View>(R.id.btnBallCamera)
            .setOnClickListener { openPhotoCamera() }

        // ✅ Video camera button
        v.findViewById<View>(R.id.btnBallVideoCamera)
            .setOnClickListener { openVideoCamera() }

        // ✅ Gallery ab image/* + video/* dono support karta hai
        v.findViewById<View>(R.id.btnBallGallery)
            .setOnClickListener { galleryLauncher.launch("*/*") }

        v.findViewById<TextView>(R.id.btnBackToGallery)
            .setOnClickListener { goTo(STEP_GALLERY) }

        // Comment step
        btnUpload.setOnClickListener { doUpload() }
        v.findViewById<TextView>(R.id.btnBackToSource)
            .setOnClickListener { goTo(STEP_SOURCE) }
    }

    private fun setupAdapter() {
        mediaAdapter = MediaGridAdapter(
            favouriteIds      = emptySet(),
            scope             = lifecycleScope,
            onFavouriteToggle = ::toggleFavourite,
            onVideoClick      = { media ->
                VideoPlayerDialog.newInstance(media.url)
                    .show(childFragmentManager, "video_ball")
            }
        )
        rvMedia.layoutManager = GridLayoutManager(requireContext(), 2)
        rvMedia.adapter = mediaAdapter
    }

    // ── Step navigation ───────────────────────────────────────────────────────
    private fun goTo(step: String) {
        currentStep            = step
        stepGallery.visibility = if (step == STEP_GALLERY) View.VISIBLE else View.GONE
        stepSource.visibility  = if (step == STEP_SOURCE)  View.VISIBLE else View.GONE
        stepComment.visibility = if (step == STEP_COMMENT) View.VISIBLE else View.GONE

        if (step == STEP_COMMENT) {
            tvFileName.text = pendingUri?.lastPathSegment ?: "Selected file"
            etComment.text?.clear()
        }
    }

    // ── Fetch existing media ──────────────────────────────────────────────────
    private fun fetchBallMedia() {
        if (ballId == -1L) return
        progressMedia.visibility = View.VISIBLE
        rvMedia.visibility       = View.GONE
        layoutEmpty.visibility   = View.GONE

        lifecycleScope.launch {
            try {
                val mediaRes = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getMediaByBallId(ballId)
                }
                val favRes = if (accountId != -1L && matchId != -1L) {
                    withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getMatchFavouriteMediaIds(matchId, accountId)
                    }
                } else null

                mediaList = mediaRes.body() ?: emptyList()
                favIds    = (favRes?.body() ?: emptyList()).toMutableSet()

                progressMedia.visibility = View.GONE
                refreshMediaUI()
            } catch (e: Exception) {
                progressMedia.visibility = View.GONE
                layoutEmpty.visibility   = View.VISIBLE
                requireContext().toastShort("Error loading media: ${e.message}")
            }
        }
    }

    private fun refreshMediaUI() {
        tvMediaCount.text = "${mediaList.size} items"
        if (mediaList.isEmpty()) {
            rvMedia.visibility     = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
            rvMedia.visibility     = View.VISIBLE
            mediaAdapter.submitList(mediaList)
            mediaAdapter.updateFavourites(favIds.toSet())
        }
    }

    // ── Favourite toggle ──────────────────────────────────────────────────────
    private fun toggleFavourite(mediaId: Long) {
        if (accountId == -1L) { requireContext().toastShort("Login required"); return }
        if (matchId == -1L) return

        if (favIds.contains(mediaId)) favIds.remove(mediaId) else favIds.add(mediaId)
        mediaAdapter.updateFavourites(favIds.toSet())

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.toggleFavouriteMedia(
                        mapOf("accountId" to accountId,
                            "mediaId"   to mediaId,
                            "matchId"   to matchId)
                    )
                }
            } catch (e: Exception) {
                if (favIds.contains(mediaId)) favIds.remove(mediaId) else favIds.add(mediaId)
                mediaAdapter.updateFavourites(favIds.toSet())
                requireContext().toastShort("Error: ${e.message}")
            }
        }
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    // ✅ Photo camera
    private fun openPhotoCamera() {
        val file = File(
            requireContext().cacheDir,
            "ball_${ballId}_${System.currentTimeMillis()}.jpg"
        )
        cameraUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", file
        )
        cameraLauncher.launch(cameraUri!!)
    }

    // ✅ Video camera — videoLauncher ab yahan call ho raha hai
    private fun openVideoCamera() {
        val file = File(
            requireContext().cacheDir,
            "ball_${ballId}_${System.currentTimeMillis()}.mp4"  // .mp4 extension
        )
        cameraUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", file
        )
        videoLauncher.launch(cameraUri!!)
    }

    // ── Upload ────────────────────────────────────────────────────────────────
    private fun doUpload() {
        val uri = pendingUri ?: return
        if (ballId == -1L || matchId == -1L || isUploading) return

        isUploading         = true
        btnUpload.isEnabled = false
        btnUpload.text      = "Uploading…"

        val comment = etComment.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        // ✅ MIME type uri se automatically detect hoga
        val mimeType  = requireContext().contentResolver.getType(uri) ?: "image/*"
        val isVideo   = mimeType.startsWith("video")
        val extension = if (isVideo) ".mp4" else ".jpg"

        lifecycleScope.launch {
            try {
                val input = requireContext().contentResolver.openInputStream(uri)
                val temp  = File(
                    requireContext().cacheDir,
                    "upload_${System.currentTimeMillis()}$extension"  // ✅ dynamic extension
                )
                temp.outputStream().use { input?.copyTo(it) }

                val filePart = MultipartBody.Part.createFormData(
                    "file", temp.name,
                    temp.asRequestBody(mimeType.toMediaTypeOrNull())  // ✅ dynamic MIME
                )
                val matchPart   = matchId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val ballPart    = ballId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val commentPart = comment?.toRequestBody("text/plain".toMediaTypeOrNull())

                val resp = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.createMedia(matchPart, ballPart, filePart, commentPart)
                }

                if (resp.isSuccessful) {
                    requireContext().toastShort("Upload successful!")
                    pendingUri = null
                    goTo(STEP_GALLERY)
                    fetchBallMedia()
                } else {
                    requireContext().toastShort("Upload failed: ${resp.code()}")
                }
            } catch (e: Exception) {
                requireContext().toastShort("Error: ${e.message}")
            } finally {
                isUploading         = false
                btnUpload.isEnabled = true
                btnUpload.text      = "Upload"
            }
        }
    }

    // ── Companion ─────────────────────────────────────────────────────────────
    companion object {
        const val TAG          = "BallMediaBottomSheet"
        const val STEP_GALLERY = "gallery"
        const val STEP_SOURCE  = "source"
        const val STEP_COMMENT = "comment"
        private const val ARG_BALL_ID    = "ball_id"
        private const val ARG_MATCH_ID   = "match_id"
        private const val ARG_ACCOUNT_ID = "account_id"

        fun newInstance(ballId: Long, matchId: Long, accountId: Long) =
            BallMediaBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BALL_ID,    ballId)
                    putLong(ARG_MATCH_ID,   matchId)
                    putLong(ARG_ACCOUNT_ID, accountId)
                }
            }
    }
}