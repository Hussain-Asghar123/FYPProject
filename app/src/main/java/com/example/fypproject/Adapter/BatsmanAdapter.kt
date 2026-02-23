package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.BatsmanScore

class BatsmanAdapter(private var list:List<BatsmanScore> =emptyList()): RecyclerView.Adapter<BatsmanAdapter.VH>(){
    inner class VH(view: View): RecyclerView.ViewHolder(view){
        val tvName: TextView = view.findViewById(R.id.tvBatsmanName)
        val tvRuns: TextView = view.findViewById(R.id.tvBatsmanRuns)
        val tvBalls: TextView = view.findViewById(R.id.tvBatsmanBalls)
        val tvFours: TextView = view.findViewById(R.id.tvBatsmanFours)
        val tvSixes: TextView = view.findViewById(R.id.tvBatsmanSixes)
        val tvSR: TextView = view.findViewById(R.id.tvBatsmanSR)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val batsman = list[position]
        holder.tvName.text = batsman.name
        holder.tvRuns.text = batsman.runs.toString()
        holder.tvBalls.text = batsman.balls.toString()
        holder.tvFours.text = batsman.fours.toString()
        holder.tvSixes.text = batsman.sixes.toString()
        holder.tvSR.text = String.format("%.2f", batsman.strikeRate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)=
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_batsman_row,parent,false))

    override fun getItemCount() = list.size

    fun updateData(newList: List<BatsmanScore>) {
        list = newList
        notifyDataSetChanged()
    }

}