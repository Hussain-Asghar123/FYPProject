package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.databinding.ItemLiveMatchBinding
import com.example.fypproject.databinding.ItemUpcomingMatchBinding

class MatchAdapter (
    private val matches:MutableList<MatchResponse>,
    private val isLive:Boolean
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
        if(isLive){
            val b=holder.binding as ItemLiveMatchBinding
            b.txtTeamNames.text = "${match.team1Name ?: "Team1"} - ${match.team2Name ?: "Team2"}"
            b.txtVenue.text = match.venue ?: "Unknown"
            b.txtMatchStatus.text = match.status ?: "Live"
        }
        else {
            val b = holder.binding as ItemUpcomingMatchBinding
            b.txtTeamNames.text = "${match.team1Name ?: "Team1"} vs ${match.team2Name ?: "Team2"}"
            val date = match.date ?: "TBD"
            val time = match.time ?: ""
            val venue = match.venue ?: ""
            b.txtMatchStatus.text = listOf(date, time, venue).filter { it.isNotBlank() }.joinToString(" | ")
        }
    }

    override fun getItemCount(): Int =matches.size

    fun updateData(newMatches:List<MatchResponse>){
        matches.clear()
        matches.addAll(newMatches)
        notifyDataSetChanged()
    }
}