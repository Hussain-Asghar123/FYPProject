package com.example.fypproject.CricketFragment

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
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

class FavouriteFragment : Fragment() {

    private var matchResponse: MatchResponse? = null
    private var favItems: MutableList<MediaItem> = mutableListOf()

    private lateinit var rvFav: RecyclerView
    private lateinit var progressFav: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutContent: LinearLayout
    private lateinit var tvCount: TextView
    private lateinit var favAdapter: MediaGridAdapter

    private val prefs: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("MyPrefs", android.content.Context.MODE_PRIVATE)
    }
    private val accountId: Long get() = prefs.getLong("id", -1L)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_favourites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }

        rvFav       = view.findViewById(R.id.rvFav)
        progressFav = view.findViewById(R.id.progressFav)
        layoutEmpty = view.findViewById(R.id.layoutFavEmpty)
        tvCount     = view.findViewById(R.id.tvFavCount)
        layoutContent = view.findViewById(R.id.layoutFavContent)

        favAdapter = MediaGridAdapter(
            favouriteIds      = emptySet(),
            scope             = lifecycleScope,
            onFavouriteToggle = ::unfavourite,
            onVideoClick      = { media ->
                VideoPlayerDialog.newInstance(media.url)
                    .show(childFragmentManager, "fav_video")
            },
            onImageClick      = { media ->
                MediaFullScreenDialog.newInstance(media.url)
                    .show(childFragmentManager, "fav_image_full")
            }
        )
        rvFav.layoutManager = GridLayoutManager(requireContext(), 2)
        rvFav.adapter = favAdapter

        view.findViewById<Button>(R.id.btnRetryFav)?.setOnClickListener { fetchFavourites() }

        if (accountId == -1L) {
            showLoginRequired(view)
        } else {
            fetchFavourites()
        }
    }

    // ── KEY FIX: hide/show pattern calls this, NOT onResume ──────────────────
    // Jab bhi Favourite tab pe click hoga, hidden=false ayega → refresh
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && accountId != -1L && ::rvFav.isInitialized) {
            Log.e("FavFrag", "Tab visible hua — refresh kar raha hoon")
            fetchFavourites()
        }
    }

    private fun fetchFavourites() {
        if (accountId == -1L) return

        Log.e("FavFrag", "fetchFavourites called — accountId=$accountId")
        showLoading(true)

        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getAccountFavouriteMedia(accountId)
                }

                if (!res.isSuccessful) {
                    Log.e("FavFrag", "API error ${res.code()}: ${res.errorBody()?.string()}")
                    showLoading(false)
                    showEmpty(true, "Server error ${res.code()}")
                    requireContext().toastShort("Load failed: ${res.code()}")
                    return@launch
                }

                val body = res.body()
                Log.e("FavFrag", "Response: ${body?.size} items — $body")

                favItems = (body ?: emptyList()).toMutableList()
                val allIds = favItems.map { it.id }.toSet()

                showLoading(false)
                tvCount.text = "${favItems.size} items"

                if (favItems.isEmpty()) {
                    showEmpty(true, null)
                } else {
                    showEmpty(false, null)
                    // ✅ pehle favs, phir FRESH COPY list
                    favAdapter.updateFavourites(allIds)
                    favAdapter.submitList(favItems.toList())
                }

            } catch (e: Exception) {
                Log.e("FavFrag", "Exception: ${e.message}", e)
                showLoading(false)
                showEmpty(true, "Error: ${e.message}")
                requireContext().toastShort("Error: ${e.message}")
            }
        }
    }

    private fun unfavourite(mediaId: Long) {
        if (accountId == -1L) return
        val matchId = matchResponse?.id ?: -1L

        favItems.removeAll { it.id == mediaId }
        tvCount.text = "${favItems.size} items"

        if (favItems.isEmpty()) {
            favAdapter.submitList(emptyList())
            showEmpty(true, null)
        } else {
            val allIds = favItems.map { it.id }.toSet()
            favAdapter.updateFavourites(allIds)
            favAdapter.submitList(favItems.toList())
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.toggleFavouriteMedia(
                        mapOf("accountId" to accountId, "mediaId" to mediaId, "matchId" to matchId)
                    )
                }
            } catch (e: Exception) {
                Log.e("FavFrag", "unfavourite error: ${e.message}", e)
                requireContext().toastShort("Error: ${e.message}")
                fetchFavourites()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressFav.visibility = if (show) View.VISIBLE else View.GONE
        layoutContent.visibility       = if (show) View.GONE    else View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty(show: Boolean, subtitle: String?) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        layoutContent.visibility       = if (show) View.GONE    else View.VISIBLE
        progressFav.visibility = View.GONE

        if (show) {
            val title = if (subtitle != null) "Something went wrong" else "No Favourites Yet"
            val sub   = subtitle ?: "Tap the ♥ on any media to save it here"
            view?.findViewById<TextView>(R.id.tvFavEmptyTitle)?.text    = title
            view?.findViewById<TextView>(R.id.tvFavEmptySubtitle)?.text = sub
        }
    }

    private fun showLoginRequired(view: View) {
        progressFav.visibility = View.GONE
        rvFav.visibility       = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.tvFavEmptyTitle)?.text    = "Login Required"
        view.findViewById<TextView>(R.id.tvFavEmptySubtitle)?.text = "Please login to view favourites"
    }

    companion object {
        fun newInstance(match: MatchResponse) = FavouriteFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}