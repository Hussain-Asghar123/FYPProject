package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PlayerPerformanceDto
import com.example.fypproject.databinding.RowBatsmenStatsBinding
import com.example.fypproject.databinding.RowBowlersStatsBinding

class TournamentStatsAdapter(
    private val items: List<PlayerPerformanceDto>,
    private val isBatting: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class BattingViewHolder(val binding: RowBatsmenStatsBinding): RecyclerView.ViewHolder(binding.root)
    inner class BowlingViewHolder(val binding: RowBowlersStatsBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater= LayoutInflater.from(parent.context)
        return if (isBatting){
            BattingViewHolder(RowBatsmenStatsBinding.inflate(inflater, parent, false))
        }
        else{
            BowlingViewHolder(RowBowlersStatsBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val player=items[position]
        val rank=position+1
        if(holder is BattingViewHolder){
            holder.binding.apply {
                tvRank.text = rank.toString()
                tvPlayer.text = player.playerName
                tvRuns.text = player.runs.toString()
                tvBalls.text = player.ballsFaced.toString()
                tv4s.text = player.fours.toString()
                tv6s.text = player.sixes.toString()
                tvPOM.text = player.pomCount.toString()
            }
        }
        else if (holder is BowlingViewHolder) {
            holder.binding.apply {
                tvRank.text = rank.toString()
                tvPlayer.text = player.playerName
                tvWickets.text = player.wickets.toString()
                tvRuns.text = player.runsConceded.toString()
                tvEconomy.text = String.format("%.2f", player.economy ?: 0.0)
                tvPOM.text = player.pomCount.toString()
            }
        }
    }

    override fun getItemCount() = items.take(5).size


}