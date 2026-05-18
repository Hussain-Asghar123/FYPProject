package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.BowlerScore

class BowlerAdapter(private var list: List<BowlerScore> = emptyList()) :
    RecyclerView.Adapter<BowlerAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName:     TextView = view.findViewById(R.id.tvBowlerName)
        val tvOvers:    TextView = view.findViewById(R.id.tvBowlerOvers)
        val tvWickets:  TextView = view.findViewById(R.id.tvBowlerWickets)
        val tvEco:      TextView = view.findViewById(R.id.tvBowlerEco)
        val tvRC:       TextView = view.findViewById(R.id.tvBowlerRC)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bowler_row, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val b = list[position]
        holder.tvName.text    = b.name
        holder.tvOvers.text   = "${b.overs}.${b.ballsBowled % 6}"
        holder.tvWickets.text = b.wickets.toString()
        holder.tvEco.text     = String.format("%.2f", b.economy.toDouble())
        holder.tvRC.text      = b.runsConceded.toString()
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<BowlerScore>) {
        list = newList
        notifyDataSetChanged()
    }
}