package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.TeamDTO
import com.example.fypproject.R

class TeamAdapter(
    private val teams: List<TeamDTO>
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>(){
    class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teamName: TextView = itemView.findViewById(R.id.tvTeamName)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_teams, parent, false)
        return TeamViewHolder(view)

    }
    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
       holder.teamName.text = teams[position].name
    }
    override fun getItemCount(): Int {
        return teams.size
    }




}