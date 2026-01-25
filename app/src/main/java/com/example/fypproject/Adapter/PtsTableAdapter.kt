package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PtsTableDto
import com.example.fypproject.databinding.ItemPointsTableBinding

class PtsTableAdapter(private var items:List<PtsTableDto>): RecyclerView.Adapter<PtsTableAdapter.ViewHolder>() {
    inner class ViewHolder(private val binding: ItemPointsTableBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PtsTableDto) {
            binding.apply {
                tvTeamName.text = item.teamName
                tvMatches.text = item.played.toString()
                tvWins.text = item.wins.toString()
                tvLosses.text = item.losses.toString()
                tvPoints.text = item.points.toString()
                tvNRR.text = String.format("%.2f", item.nrr)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPointsTableBinding.inflate(
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

    fun updateData(newItems: List<PtsTableDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}