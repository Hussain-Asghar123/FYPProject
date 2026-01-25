package com.example.fypproject.Adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.MediaDto
import com.example.fypproject.databinding.ItemMediaBinding
import com.bumptech.glide.Glide
class MediaAdapter(
    private val mediaList: MutableList<MediaDto>,
    private val onItemClick: (List<MediaDto>, Int) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(media: MediaDto, position: Int) {
            Glide.with(binding.imgMedia.context)
                .load(media.url)
                .into(binding.imgMedia)

            binding.root.setOnClickListener {
                onItemClick(mediaList, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaList[position], position)
    }

    override fun getItemCount(): Int = mediaList.size

    fun addItems(newItems: List<MediaDto>) {
        val start = mediaList.size
        mediaList.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }
    
    fun clearItems() {
        val size = mediaList.size
        mediaList.clear()
        notifyItemRangeRemoved(0, size)
    }
}
