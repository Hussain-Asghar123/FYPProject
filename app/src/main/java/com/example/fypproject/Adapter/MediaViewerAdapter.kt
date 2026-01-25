package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fypproject.DTO.MediaDto
import com.example.fypproject.databinding.ItemMediaViewerBinding

class MediaViewerAdapter(
    private val mediaList: List<MediaDto>
) : RecyclerView.Adapter<MediaViewerAdapter.MediaViewerViewHolder>() {

    inner class MediaViewerViewHolder(private val binding: ItemMediaViewerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(media: MediaDto) {
            binding.imageView.visibility = View.GONE
            binding.videoView.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            
            when (media.fileType.lowercase()) {
                "image", "jpg", "jpeg", "png", "gif" -> {
                    binding.imageView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.VISIBLE
                    
                    Glide.with(binding.imageView.context)
                        .load(media.url)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(binding.imageView)
                    
                    binding.progressBar.visibility = View.GONE
                }
                "video", "mp4", "avi", "mov" -> {
                    binding.videoView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.VISIBLE
                    
                    try {
                        binding.videoView.setVideoPath(media.url)
                        binding.videoView.setOnPreparedListener { 
                            binding.progressBar.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                    }
                }
                else -> {
                    binding.imageView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.VISIBLE
                    
                    Glide.with(binding.imageView.context)
                        .load(media.url)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(binding.imageView)
                    
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewerViewHolder {
        val binding = ItemMediaViewerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewerViewHolder, position: Int) {
        holder.bind(mediaList[position])
    }

    override fun getItemCount(): Int = mediaList.size
}
