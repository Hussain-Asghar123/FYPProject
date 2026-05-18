package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.Player
import com.example.fypproject.R

class PlayerAdapter(
    private var creatorPlayerId: Long = -1L,   // ← NEW: captain detection
    private var canRemove: Boolean = false,
    private val onRemove: ((Long) -> Unit)? = null
) : ListAdapter<Player, PlayerAdapter.PlayerVH>(DIFF) {

    inner class PlayerVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatar: TextView   = itemView.findViewById(R.id.tvPlayerAvatar)
        val tvName: TextView     = itemView.findViewById(R.id.tvPlayerName)
        val tvRole: TextView     = itemView.findViewById(R.id.tvPlayerRole)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemovePlayer)

        fun bind(player: Player) {
            // First letter as avatar
            tvAvatar.text = player.name.firstOrNull()?.uppercase() ?: "?"

            tvName.text = player.name

            // Captain check
            val isCaptain = player.id.toLong() == creatorPlayerId
            tvRole.text = if (isCaptain) "⭐ Captain" else "Player"

            // Remove button
            if (canRemove) {
                btnRemove.visibility = View.VISIBLE
                btnRemove.setOnClickListener {
                    onRemove?.invoke(player.id.toLong())
                }
            } else {
                btnRemove.visibility = View.GONE
            }
        }
    }

    /** Call this after TeamResponse arrives so captain badge updates */
    fun setCreatorPlayerId(id: Long) {
        creatorPlayerId = id
        notifyDataSetChanged()
    }

    fun setCanRemove(value: Boolean) {
        canRemove = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PlayerVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_player, parent, false)
        )

    override fun onBindViewHolder(holder: PlayerVH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Player>() {
            override fun areItemsTheSame(a: Player, b: Player) = a.id == b.id
            override fun areContentsTheSame(a: Player, b: Player) = a == b
        }
    }
}