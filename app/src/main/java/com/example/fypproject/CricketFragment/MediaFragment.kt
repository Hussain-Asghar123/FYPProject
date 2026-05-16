package com.example.fypproject.CricketFragment

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.Adapter.MediaGridAdapter
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.ScoringDTO.MediaItem
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.Utils.toastShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaFragment : Fragment() {

    private var matchResponse: MatchResponse? = null
    private var mediaItems: List<MediaItem> = emptyList()
    private var favIds: MutableSet<Long>    = mutableSetOf()

    private lateinit var rvMedia: RecyclerView
    private lateinit var progressMedia: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvCount: TextView
    private lateinit var mediaAdapter: MediaGridAdapter

    private val prefs: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("MyPrefs", android.content.Context.MODE_PRIVATE)
    }
    private val accountId: Long get() = prefs.getLong("id", -1L)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_media_standalone, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }

        rvMedia       = view.findViewById(R.id.rvMedia)
        progressMedia = view.findViewById(R.id.progressMedia)
        layoutEmpty   = view.findViewById(R.id.layoutMediaEmpty)
        tvCount       = view.findViewById(R.id.tvMediaCount)

        mediaAdapter = MediaGridAdapter(
            favouriteIds      = emptySet(),
            scope             = lifecycleScope,
            onFavouriteToggle = ::toggleFav,
            onVideoClick      = { media ->
                VideoPlayerDialog.newInstance(media.url)
                    .show(childFragmentManager, "media_video")
            },
            onImageClick      = { media ->
                MediaFullScreenDialog.newInstance(media.url)
                    .show(childFragmentManager, "media_image_full")
            }
        )
        rvMedia.layoutManager = GridLayoutManager(requireContext(), 2)
        rvMedia.adapter = mediaAdapter

        fetchMedia()
    }

    // ── Naya media tab pe aane pe reload ho ──────────────────────────────────
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && ::rvMedia.isInitialized) {
            fetchMedia()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::rvMedia.isInitialized) fetchMedia()
    }

    private fun fetchMedia() {
        val matchId = matchResponse?.id ?: return
        showLoading(true)

        lifecycleScope.launch {
            try {
                val mediaRes = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getMediaByMatchId(matchId)
                }
                val favRes = if (accountId != -1L) {
                    withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getMatchFavouriteMediaIds(matchId, accountId)
                    }
                } else null

                // ✅ always new list copy banao
                mediaItems = (mediaRes.body() ?: emptyList()).toMutableList()
                favIds     = (favRes?.body() ?: emptyList()).toMutableSet()

                showLoading(false)
                tvCount.text = "${mediaItems.size} items"

                if (mediaItems.isEmpty()) {
                    showEmpty(true)
                } else {
                    showEmpty(false)
                    // ✅ pehle favs, phir list — aur toList() se fresh copy
                    mediaAdapter.updateFavourites(favIds.toSet())
                    mediaAdapter.submitList(mediaItems.toList())
                }
            } catch (e: Exception) {
                showLoading(false)
                showEmpty(true)
                requireContext().toastShort("Error: ${e.message}")
            }
        }
    }

    private fun toggleFav(mediaId: Long) {
        if (accountId == -1L) { requireContext().toastShort("Login required"); return }
        val matchId = matchResponse?.id ?: return

        if (favIds.contains(mediaId)) favIds.remove(mediaId) else favIds.add(mediaId)
        mediaAdapter.updateFavourites(favIds.toSet())

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.toggleFavouriteMedia(
                        mapOf("accountId" to accountId, "mediaId" to mediaId, "matchId" to matchId)
                    )
                }
            } catch (e: Exception) {
                if (favIds.contains(mediaId)) favIds.remove(mediaId) else favIds.add(mediaId)
                mediaAdapter.updateFavourites(favIds.toSet())
                requireContext().toastShort("Error: ${e.message}")
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressMedia.visibility = if (show) View.VISIBLE else View.GONE
        rvMedia.visibility       = if (show) View.GONE    else View.VISIBLE
        layoutEmpty.visibility   = View.GONE
    }

    private fun showEmpty(show: Boolean) {
        layoutEmpty.visibility   = if (show) View.VISIBLE else View.GONE
        rvMedia.visibility       = if (show) View.GONE    else View.VISIBLE
        progressMedia.visibility = View.GONE
    }

    companion object {
        fun newInstance(match: MatchResponse) = MediaFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}