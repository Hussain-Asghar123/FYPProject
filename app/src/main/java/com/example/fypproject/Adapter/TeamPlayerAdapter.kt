package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.databinding.ItemTeamPlayerBinding

class TeamPlayerAdapter(
    private val players: List<TeamPlayerDto>
) : RecyclerView.Adapter<TeamPlayerAdapter.PlayerVH>() {

    inner class PlayerVH(private val b: ItemTeamPlayerBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(player: TeamPlayerDto) {
            b.tvPlayerName.text = player.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PlayerVH(
            ItemTeamPlayerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: PlayerVH, position: Int) =
        holder.bind(players[position])

    override fun getItemCount() = players.size
}