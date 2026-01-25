package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PointsTableItem
import com.example.fypproject.databinding.ItemTopTeamBinding

class TournamentOverViewAdapter (
    private val teams:List<PointsTableItem>
): RecyclerView.Adapter<TournamentOverViewAdapter.ViewHolder>() {
    inner class ViewHolder(
        private val binding: ItemTopTeamBinding
    ):RecyclerView.ViewHolder(binding.root){
        fun bind(item: PointsTableItem,position:Int){
            binding.tvTeamName.text="${position}. ${item.name}"
            binding.tvPoints.text="${item.points} pts"
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding=ItemTopTeamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(teams[position],position)
    }
    override fun getItemCount(): Int {
        return teams.size
    }
}