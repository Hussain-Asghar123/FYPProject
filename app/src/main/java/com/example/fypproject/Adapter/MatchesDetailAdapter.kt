package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.MatchDetail
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Utils.MatchNavigator

class MatchesDetailAdapter(
    private var items: MutableList<MatchResponse> = mutableListOf(),
    private val onItemClick: ((MatchResponse) -> Unit)? = null
) : RecyclerView.Adapter<MatchesDetailAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTournamentName: TextView = view.findViewById(R.id.tvTournamentName)
        val tvMatchTitle: TextView = view.findViewById(R.id.tvMatchTitle)
        val tvVenueTime: TextView = view.findViewById(R.id.tvVenueTime)

        val tvSportName: TextView=view.findViewById(R.id.tvSportName)
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
        holder.tvSportName.text = getSportName(match.sportId)
        holder.itemView.setOnClickListener { onItemClick?.invoke(match) }

    }
    override fun getItemCount(): Int = items.size

    private fun getSportName(sportId: Long?): String {
        return when (sportId) {
            1L -> "Cricket"
            2L -> "Futsal"
            3L -> "Volleyball"
            4L -> "Table Tennis"
            5L -> "Badminton"
            6L -> "Ludo"
            7L -> "Tug of War"
            8L -> "Chess"
            else -> "Unknown"
        }
    }

    fun setItems(newItems: List<MatchResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
