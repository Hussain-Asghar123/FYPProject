package com.example.fypproject.Adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fypproject.ScoringDTO.MediaItem
import com.example.fypproject.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaGridAdapter(
    private var favouriteIds: Set<Long>,
    private val scope: CoroutineScope,
    private val onFavouriteToggle: (Long) -> Unit,
    private val onVideoClick: (MediaItem) -> Unit = {},
    private val onImageClick: (MediaItem) -> Unit = {}
) : ListAdapter<MediaItem, MediaGridAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(o: MediaItem, n: MediaItem) = o.id == n.id
            override fun areContentsTheSame(o: MediaItem, n: MediaItem) = o == n
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivMedia: ImageView  = view.findViewById(R.id.ivMedia)
        val ivPlay: ImageView   = view.findViewById(R.id.ivPlayIcon)
        val btnFav: ImageButton = view.findViewById(R.id.btnFavourite)
        val tvComment: TextView = view.findViewById(R.id.tvComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_grid, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item    = getItem(position)
        val isVideo = item.fileType?.startsWith("video") == true

        loadMedia(holder, item)

        holder.ivPlay.visibility = if (isVideo) View.VISIBLE else View.GONE

        holder.ivMedia.setOnClickListener {
            if (isVideo) onVideoClick(item) else onImageClick(item)
        }

        val isFav = favouriteIds.contains(item.id)
        holder.btnFav.setImageResource(
            if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        holder.btnFav.setColorFilter(
            if (isFav) 0xFFE31212.toInt() else 0xFFAAAAAA.toInt()
        )
        holder.btnFav.setOnClickListener { onFavouriteToggle(item.id) }

        if (!item.comment.isNullOrBlank()) {
            holder.tvComment.visibility = View.VISIBLE
            holder.tvComment.text = "\"${item.comment}\""
        } else {
            holder.tvComment.visibility = View.GONE
        }
    }

    private fun loadMedia(holder: VH, item: MediaItem) {
        val url = item.url

        // ── Pehle placeholder set karo ────────────────────────────────────────
        holder.ivMedia.setImageResource(android.R.drawable.ic_menu_gallery)

        when {
            // ── Base64 ────────────────────────────────────────────────────────
            url != null && url.contains("base64,") -> {

                // String tag use karo — Long boxing issue avoid hoga
                val tagKey = "media_${item.id}"
                holder.ivMedia.tag = tagKey

                scope.launch(Dispatchers.IO) {
                    try {
                        Log.d("MediaAdapter", "Decoding base64 for id=${item.id}")

                        val base64Part = url.substringAfter("base64,").trim()
                        val bytes      = Base64.decode(base64Part, Base64.DEFAULT)
                        val bitmap     = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        Log.d("MediaAdapter", "Decoded bitmap=${bitmap != null} for id=${item.id}")

                        withContext(Dispatchers.Main) {
                            // View recycle check
                            if (holder.ivMedia.tag == tagKey) {
                                if (bitmap != null) {
                                    holder.ivMedia.setImageBitmap(bitmap)
                                    Log.d("MediaAdapter", "Bitmap set for id=${item.id}")
                                } else {
                                    holder.ivMedia.setImageResource(
                                        android.R.drawable.ic_menu_report_image
                                    )
                                }
                            } else {
                                Log.d("MediaAdapter", "Tag mismatch — view recycled, skip id=${item.id}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MediaAdapter", "Error decoding id=${item.id}: ${e.message}")
                        withContext(Dispatchers.Main) {
                            if (holder.ivMedia.tag == tagKey) {
                                holder.ivMedia.setImageResource(
                                    android.R.drawable.ic_menu_report_image
                                )
                            }
                        }
                    }
                }
            }

            // ── Normal HTTP URL ───────────────────────────────────────────────
            !url.isNullOrBlank() -> {
                holder.ivMedia.tag = null
                Glide.with(holder.itemView.context)
                    .asBitmap()
                    .load(url)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivMedia)
            }

            else -> {
                holder.ivMedia.tag = null
                holder.ivMedia.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }
    }

    fun updateFavourites(newFavs: Set<Long>) {
        favouriteIds = newFavs
        notifyDataSetChanged()
    }
}