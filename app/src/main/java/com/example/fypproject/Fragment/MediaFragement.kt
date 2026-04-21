package com.example.fypproject.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.Adapter.MediaAdapter
import com.example.fypproject.Adapter.MediaViewerAdapter
import com.example.fypproject.DTO.MediaDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.databinding.FragementMediaBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MediaFragment : Fragment() {

    private lateinit var binding: FragementMediaBinding
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var mediaViewerAdapter: MediaViewerAdapter
    private val mediaList = mutableListOf<MediaDto>()
    private var tournamentId: Long = -1L
    private var page = 0
    private val size = 6
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragementMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tournamentId = arguments?.getLong("tournamentId", -1L) ?: -1L

        setupRecyclerView()
        setupViewPager()
        fetchTournamentMedia()
        setupBackPress()
    }

    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPager.visibility == View.VISIBLE) {
                    hideMediaViewer()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupRecyclerView() {
        mediaAdapter = MediaAdapter(mediaList) { _, position ->
            showMediaViewer(position)
        }

        val layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMedia.layoutManager = layoutManager
        binding.rvMedia.adapter = mediaAdapter

        binding.rvMedia.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val visible = layoutManager.childCount
                val total = layoutManager.itemCount
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visible + firstVisible) >= total) {
                    fetchTournamentMedia()
                }
            }
        })
    }

    private fun fetchTournamentMedia() {
        if (isLoading) return

        isLoading = true
        binding.progressBar.visibility = View.VISIBLE

        api.getMediaByTournamentId(tournamentId, page, size)
            .enqueue(object : Callback<List<MediaDto>> {
                override fun onResponse(call: Call<List<MediaDto>>, response: Response<List<MediaDto>>) {
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
                        checkEmptyState()
                    } else if (page == 0) {
                        checkEmptyState()
                    }
                }

                override fun onFailure(call: Call<List<MediaDto>>, t: Throwable) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), t.message, Toast.LENGTH_SHORT).show()
                    if (page == 0) {
                        checkEmptyState()
                    }
                }
            })
    }

    private fun checkEmptyState() {
        val isEmpty = mediaList.isEmpty()
        binding.rvMedia.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setupViewPager() {
        mediaViewerAdapter = MediaViewerAdapter(mediaList)
        binding.viewPager.adapter = mediaViewerAdapter
    }

    private fun showMediaViewer(position: Int) {
        binding.viewPager.setCurrentItem(position, false)
        binding.viewPager.visibility = View.VISIBLE
        binding.rvMedia.visibility = View.GONE
    }

    private fun hideMediaViewer() {
        binding.viewPager.visibility = View.GONE
        binding.rvMedia.visibility = View.VISIBLE
    }

    companion object {
        fun newInstance(tournamentId: Long): MediaFragment {
            return MediaFragment().apply {
                arguments = Bundle().apply {
                    putLong("tournamentId", tournamentId)
                }
            }
        }
    }
}