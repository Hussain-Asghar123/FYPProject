package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.SportTournamentCount
import com.example.fypproject.databinding.ItemSeasonSportsBinding

class TournamentDetailAdapter(
    private val items: MutableList<SportTournamentCount>,
    private val onItemClick: (SportTournamentCount) -> Unit
) : RecyclerView.Adapter<TournamentDetailAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSeasonSportsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SportTournamentCount) {
            binding.tvSportName.text = item.name.ifBlank { "Unnamed Sport" }
            binding.tvTournamentCount.text = (item.tournamentCount ?: 0).toString()

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSeasonSportsBinding.inflate(
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

    fun updateList(newList: List<SportTournamentCount>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}
