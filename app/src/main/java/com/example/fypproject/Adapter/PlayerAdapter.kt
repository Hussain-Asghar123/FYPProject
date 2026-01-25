package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.Player
import com.example.fypproject.databinding.ItemPlayerBinding

class PlayerAdapter : ListAdapter<Player, PlayerAdapter.PlayerViewHolder>(PlayerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlayerViewHolder(private val binding: ItemPlayerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(player: Player) {
            binding.tvPlayerName.text = player.name
            binding.tvPlayerStatus.text = player.status
        }
    }

    private class PlayerDiffCallback : DiffUtil.ItemCallback<Player>() {
        override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean {
            return oldItem == newItem
        }
    }
}