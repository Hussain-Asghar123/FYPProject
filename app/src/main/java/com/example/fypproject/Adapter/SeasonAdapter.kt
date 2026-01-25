package com.example.fypproject.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.SeasonResponse
import com.example.fypproject.R
import kotlin.jvm.java

class SeasonAdapter (
    private val list: List<SeasonResponse>,
    private val onItemClick: (SeasonResponse) -> Unit
): RecyclerView.Adapter<SeasonAdapter.SeasonViewHolder>() {
    inner class SeasonViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val card: CardView = itemView.findViewById(R.id.card)
        val tvName: TextView = itemView.findViewById(R.id.tvSeasonName)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_season, parent, false)
        return SeasonViewHolder(view)
    }
    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        val season=list[position]
        holder.tvName.text=season.name
        holder.card.setOnClickListener {
            onItemClick(season)
        }
    }
    override fun getItemCount(): Int {
        return list.size
    }
}