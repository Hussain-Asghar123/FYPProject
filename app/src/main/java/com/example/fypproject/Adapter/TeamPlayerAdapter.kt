package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.R

class TeamPlayerAdapter(
    private val players: List<TeamPlayerDto>
) : RecyclerView.Adapter<TeamPlayerAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatar : TextView = itemView.findViewById(R.id.tvPlayerAvatar)
        val tvName   : TextView = itemView.findViewById(R.id.tvPlayerName)
        val tvRole   : TextView = itemView.findViewById(R.id.tvPlayerRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false))

    override fun getItemCount() = players.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = players[position]

        // First letter avatar
        holder.tvAvatar.text = p.name?.firstOrNull()?.uppercase() ?: "?"

        holder.tvName.text = p.name

        // Captain badge — source of truth is isCreator from API
        holder.tvRole.text = if (p.isCreator) "⭐ Captain" else "Player"
    }
}