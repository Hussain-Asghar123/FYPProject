package com.example.fypproject.Activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.Adapter.MediaAdapter
import com.example.fypproject.Adapter.MediaViewerAdapter
import com.example.fypproject.DTO.MediaDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.ActivitySeasonMediaBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SeasonMediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeasonMediaBinding
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var mediaViewerAdapter: MediaViewerAdapter
    private val mediaList = mutableListOf<MediaDto>()

    private var seasonId: Long = -1L
    private var page = 0
    private val size = 6
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeasonMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        seasonId = intent.getLongExtra("seasonId", -1L)

        setupUI()
        setupRecyclerView()
        fetchSeasonMedia()
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        binding.tvTitle.text = "Season Media"
        binding.btnBack.setOnClickListener {
            if (binding.viewPager.visibility == View.VISIBLE) {
                hideMediaViewer()
            } else {
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        mediaAdapter = MediaAdapter(mediaList) { _, position ->
            showMediaViewer(position)
        }

        val layoutManager = GridLayoutManager(this, 2)
        binding.rvMedia.layoutManager = layoutManager
        binding.rvMedia.adapter = mediaAdapter

        binding.rvMedia.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val visible = layoutManager.childCount
                val total = layoutManager.itemCount
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visible + firstVisible) >= total) {
                    fetchSeasonMedia()
                }
            }
        })

        setupViewPager()
    }

    private fun fetchSeasonMedia() {
        if (isLoading) return

        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor))

        api.getMediaBySeasonId(seasonId, page, size)
            .enqueue(object : Callback<List<MediaDto>> {
                override fun onResponse(
                    call: Call<List<MediaDto>>,
                    response: Response<List<MediaDto>>
                ) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val newItems = response.body()!!

                        if (page == 0) {
                            mediaAdapter.clearItems()
                        }

                        mediaAdapter.addItems(newItems)
                        mediaViewerAdapter.notifyDataSetChanged()
                        page++
                    }
                }

                override fun onFailure(call: Call<List<MediaDto>>, t: Throwable) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@SeasonMediaActivity, "Failed to load media", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupViewPager() {
        mediaViewerAdapter = MediaViewerAdapter(mediaList)
        binding.viewPager.adapter = mediaViewerAdapter
    }

    private fun showMediaViewer(position: Int) {
        binding.viewPager.setCurrentItem(position, false)
        binding.viewPager.visibility = View.VISIBLE
        binding.rvMedia.visibility = View.GONE
        binding.tvTitle.text = "Media Viewer"
    }

    private fun hideMediaViewer() {
        binding.viewPager.visibility = View.GONE
        binding.rvMedia.visibility = View.VISIBLE
        binding.tvTitle.text = "Season Media"
    }
}