package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.databinding.ItemScoringCardBinding

class ScrorerAdapter(
    private val items: List<MatchResponse>,
    private val onItemClick:(MatchResponse) -> Unit
) : RecyclerView.Adapter<ScrorerAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemScoringCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(match: MatchResponse) {
            binding.tvTournamentName.text = match.tournamentName
            binding.tvMatchTitle.text = "${match.team1Name} vs ${match.team2Name}"
            binding.tvVenueTime.text = "${match.date ?: ""} ${match.time ?: ""}"

            binding.root.setOnClickListener { onItemClick(match) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScoringCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
