package com.example.fypproject.Activity

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.Adapter.MediaGridAdapter
import com.example.fypproject.CricketFragment.MediaFullScreenDialog
import com.example.fypproject.CricketFragment.VideoPlayerDialog
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.MediaItem
import com.example.fypproject.Utils.toastShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyFavouriteMediaActivity : AppCompatActivity() {

    private lateinit var rvMedia: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var layoutContent: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var searchView: SearchView

    private lateinit var adapter: MediaGridAdapter
    private var allItems: MutableList<MediaItem> = mutableListOf()

    private val accountId: Long by lazy {
        getSharedPreferences("MyPrefs", MODE_PRIVATE).getLong("id", -1L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_favourite_media)

        // Back button
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "My Favourite Media"
        }

        rvMedia        = findViewById(R.id.rvFavouriteMedia)
        progressBar    = findViewById(R.id.progressFavMedia)
        layoutEmpty    = findViewById(R.id.layoutFavMediaEmpty)
        layoutContent  = findViewById(R.id.layoutFavMediaContent)
        tvEmptyTitle   = findViewById(R.id.tvFavMediaEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvFavMediaEmptySubtitle)
        searchView     = findViewById(R.id.searchFavMedia)

        setupAdapter()
        setupSearch()

        if (accountId == -1L) {
            showEmpty("Login Required", "Please login to view your favourites")
        } else {
            fetchFavourites()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = MediaGridAdapter(
            favouriteIds      = emptySet(),
            scope             = lifecycleScope,
            onFavouriteToggle = ::removeFavourite,
            onVideoClick      = { media ->
                VideoPlayerDialog.newInstance(media.url)
                    .show(supportFragmentManager, "fav_video")
            },
            onImageClick      = { media ->
                MediaFullScreenDialog.newInstance(media.url)
                    .show(supportFragmentManager, "fav_image")
            }
        )
        rvMedia.layoutManager = GridLayoutManager(this, 2)
        rvMedia.adapter = adapter
    }

    // ── Search ───────────────────────────────────────────────────────────────

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                applyFilter(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilter(newText)
                return true
            }
        })
    }

    private fun applyFilter(query: String?) {
        val q = query?.trim()?.lowercase() ?: ""
        val filtered = if (q.isEmpty()) {
            allItems.toList()
        } else {
            allItems.filter { media ->
                (media.comment ?: "").lowercase().contains(q)
            }
        }
        if (filtered.isEmpty() && allItems.isNotEmpty()) {
            showEmpty("No results for \"$q\"", "Try a different caption or clear the search")
        } else if (filtered.isNotEmpty()) {
            showContent()
        }
        adapter.updateFavourites(filtered.map { it.id }.toSet())
        adapter.submitList(filtered)
    }

    // ── API ──────────────────────────────────────────────────────────────────

    private fun fetchFavourites() {
        showLoading()
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getAccountFavouriteMedia(accountId)
                }
                if (res.isSuccessful) {
                    allItems = (res.body() ?: emptyList()).toMutableList()
                    if (allItems.isEmpty()) {
                        showEmpty(
                            "No Favourites Yet",
                            "Tap the ♥ on any media to save it here"
                        )
                    } else {
                        val ids = allItems.map { it.id }.toSet()
                        adapter.updateFavourites(ids)
                        adapter.submitList(allItems.toList())
                        showContent()
                    }
                } else {
                    showEmpty("Error ${res.code()}", "Could not load favourites")
                    toastShort("Load failed: ${res.code()}")
                }
            } catch (e: Exception) {
                showEmpty("Something went wrong", e.message ?: "Unknown error")
                toastShort("Error: ${e.message}")
            }
        }
    }

    private fun removeFavourite(mediaId: Long) {
        if (accountId == -1L) return

        // Optimistic UI update
        val removedItem = allItems.find { it.id == mediaId }
        allItems.removeAll { it.id == mediaId }

        val currentQuery = searchView.query?.toString()?.trim()?.lowercase() ?: ""
        val filtered = if (currentQuery.isEmpty()) allItems.toList()
        else allItems.filter { (it.comment ?: "").lowercase().contains(currentQuery) }

        if (filtered.isEmpty()) {
            showEmpty("No Favourites Yet", "Tap the ♥ on any media to save it here")
            adapter.submitList(emptyList())
        } else {
            adapter.updateFavourites(filtered.map { it.id }.toSet())
            adapter.submitList(filtered)
        }

        // API call
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.toggleFavouriteMedia(
                        mapOf(
                            "accountId" to accountId,
                            "mediaId"   to mediaId,
                            "matchId"   to (removedItem?.matchId ?: -1L)
                        )
                    )
                }
            } catch (e: Exception) {
                toastShort("Error: ${e.message}")
                fetchFavourites() // revert on failure
            }
        }
    }

    // ── UI state helpers ─────────────────────────────────────────────────────

    private fun showLoading() {
        progressBar.visibility   = View.VISIBLE
        layoutContent.visibility = View.GONE
        layoutEmpty.visibility   = View.GONE
    }

    private fun showContent() {
        progressBar.visibility   = View.GONE
        layoutContent.visibility = View.VISIBLE
        layoutEmpty.visibility   = View.GONE
    }

    private fun showEmpty(title: String, subtitle: String) {
        progressBar.visibility   = View.GONE
        layoutContent.visibility = View.GONE
        layoutEmpty.visibility   = View.VISIBLE
        tvEmptyTitle.text    = title
        tvEmptySubtitle.text = subtitle
    }
}