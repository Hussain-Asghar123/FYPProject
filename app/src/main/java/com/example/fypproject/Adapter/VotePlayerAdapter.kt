package com.example.fypproject.Adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.R

class VotePlayerAdapter(
    private val players: List<TeamPlayerDto>,
    private val onPlayerSelected: (player: TeamPlayerDto, adapter: VotePlayerAdapter) -> Unit
) : RecyclerView.Adapter<VotePlayerAdapter.VH>() {

    private var selectedPosition: Int = -1

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvVotePlayerName)
        val dotSelected: View = itemView.findViewById(R.id.viewSelectedDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vote_player, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val player = players[position]
        holder.tvName.text = player.name ?: "Player"

        val isSelected = position == selectedPosition

        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_rounded_card)
            holder.itemView.backgroundTintList = null
            holder.tvName.setTextColor(Color.WHITE)
            holder.dotSelected.visibility = View.VISIBLE
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_rounded_card)
            holder.itemView.backgroundTintList = null
            holder.tvName.setTextColor(Color.BLACK)
            holder.dotSelected.visibility = View.INVISIBLE
        }

        holder.itemView.setOnClickListener {
            val clicked = holder.bindingAdapterPosition
            if (clicked == RecyclerView.NO_ID.toInt()) return@setOnClickListener

            val prev = selectedPosition
            selectedPosition = clicked
            if (prev != -1) notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onPlayerSelected(player, this@VotePlayerAdapter)
        }
    }

    override fun getItemCount() = players.size

    fun clearSelection() {
        val prev = selectedPosition
        selectedPosition = -1
        if (prev != -1) notifyItemChanged(prev)
    }

    fun getSelectedPlayer(): TeamPlayerDto? =
        if (selectedPosition != -1) players[selectedPosition] else null
}