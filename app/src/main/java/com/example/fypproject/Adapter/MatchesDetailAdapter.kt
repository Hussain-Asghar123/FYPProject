package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.MatchDetail
import com.example.fypproject.R

class MatchesDetailAdapter(
    private var items: MutableList<MatchDetail> = mutableListOf(),
    private val onItemClick: ((MatchDetail) -> Unit)? = null
) : RecyclerView.Adapter<MatchesDetailAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTournamentName: TextView = view.findViewById(R.id.tvTournamentName)
        val tvMatchTitle: TextView = view.findViewById(R.id.tvMatchTitle)
        val tvVenueTime: TextView = view.findViewById(R.id.tvVenueTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_matches_detail, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val match = items[position]
        holder.tvTournamentName.text = match.tournamentName
        holder.tvMatchTitle.text = "${match.team1Name} vs ${match.team2Name}"
        val dateTime = listOfNotNull(
            if (!match.date.isNullOrEmpty() || !match.time.isNullOrEmpty())
                "${match.date ?: ""} ${match.time ?: ""}".trim() else null
        ).joinToString(" • ")
        holder.tvVenueTime.text = dateTime
        holder.itemView.setOnClickListener { onItemClick?.invoke(match) }
    }
    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<MatchDetail>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
