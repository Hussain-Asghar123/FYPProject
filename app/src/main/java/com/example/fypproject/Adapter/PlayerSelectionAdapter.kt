package com.example.fypproject.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.R

class PlayerSelectionAdapter(
    private val players: List<TeamPlayerDto>,
    private val onPlayerSelected: (TeamPlayerDto) -> Unit
) : RecyclerView.Adapter<PlayerSelectionAdapter.PlayerViewHolder>() {

    private var selectedPosition = -1

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlayerName: TextView = itemView.findViewById(R.id.tvPlayerName)


        fun bind(player: TeamPlayerDto, position: Int) {

            tvPlayerName.text = player.name ?: "Unknown Player"

            val isSelected = position == selectedPosition
            itemView.setBackgroundColor(
                if (isSelected) Color.parseColor("#FFE0E0E0")
                else Color.WHITE
            )

            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position

                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onPlayerSelected(player)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_selection, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position], position)
    }

    override fun getItemCount() = players.size
    fun getSelectedPlayer(): TeamPlayerDto? {
        return if (selectedPosition != -1) players[selectedPosition] else null
    }
}