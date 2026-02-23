package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Utils.MatchNavigator
import com.example.fypproject.databinding.ItemLiveMatchBinding
import com.example.fypproject.databinding.ItemUpcomingMatchBinding

class MatchAdapter (
    private val matches:MutableList<MatchResponse>,
    private val isLive:Boolean,
    private val onItemClick:(MatchResponse) -> Unit
): RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    inner class MatchViewHolder(val binding: ViewBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = if (isLive){
            ItemLiveMatchBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        }else{
            ItemUpcomingMatchBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        }
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        holder.itemView.setOnClickListener { onItemClick(match) }
        if(isLive){
            val b=holder.binding as ItemLiveMatchBinding
            b.txtTeamNames.text = "${match.team1Name ?: "Team1"} - ${match.team2Name ?: "Team2"}"
            b.txtVenue.text = match.venue ?: "Unknown"
            b.txtMatchStatus.text = match.status ?: "Live"
            b.txtSportName.text = getSportName(match.sportId)
        }
        else {
            val b = holder.binding as ItemUpcomingMatchBinding
            b.txtTeamNames.text = "${match.team1Name ?: "Team1"} vs ${match.team2Name ?: "Team2"}"
            val date = match.date ?: "TBD"
            val time = match.time ?: ""
            val venue = match.venue ?: ""
            b.txtSportName.text = getSportName(match.sportId)
            b.txtMatchStatus.text = listOf(date, time, venue).filter { it.isNotBlank() }.joinToString(" | ")
        }
    }
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

    override fun getItemCount(): Int =matches.size

    fun updateData(newMatches:List<MatchResponse>){
        matches.clear()
        matches.addAll(newMatches)
        notifyDataSetChanged()
    }
}