package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.BowlerScore

class BowlerAdapter(private var list:List<BowlerScore> =emptyList()): RecyclerView.Adapter<BowlerAdapter.VH>(){
    inner class VH(view: View): RecyclerView.ViewHolder(view){
        val tvName: TextView = view.findViewById(R.id.tvBowlerName)
        val tvOvers: TextView = view.findViewById(R.id.tvBowlerOvers)
        val tvRuns: TextView = view.findViewById(R.id.tvBowlerRuns)
        val tvWickets: TextView = view.findViewById(R.id.tvBowlerWickets)

        val tvBowlerEco: TextView = view.findViewById(R.id.tvBowlerEco)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)=VH(LayoutInflater.from(parent.context).inflate(R.layout.item_bowler_row,parent,false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val bowler = list[position]
        holder.tvName.text = bowler.name
        holder.tvOvers.text = "${bowler.overs}.${bowler.ballsBowled % 6}"
        holder.tvRuns.text = bowler.runsConceded.toString()
        holder.tvWickets.text = bowler.wickets.toString()
        holder.tvBowlerEco.text = String.format("%.2f", bowler.economy.toDouble())
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<BowlerScore>) {
        list = newList
        notifyDataSetChanged()
    }


}