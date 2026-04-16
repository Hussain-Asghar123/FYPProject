package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.TopBatsmanDto
import com.example.fypproject.DTO.TopBowlerDto
import com.example.fypproject.DTO.TopFutsalScorerDto
import com.example.fypproject.DTO.TopFutsalAssistantDto
import com.example.fypproject.R
import com.example.fypproject.databinding.RowBatsmenStatsBinding
import com.example.fypproject.databinding.RowBowlersStatsBinding

class TournamentStatsAdapter(
    private val battingItems: List<TopBatsmanDto> = emptyList(),
    private val bowlingItems: List<TopBowlerDto> = emptyList(),
    private val isBatting: Boolean,
    private val goalScorerItems: List<TopFutsalScorerDto> = emptyList(),
    private val assistantItems: List<TopFutsalAssistantDto> = emptyList(),
    private val isFutsal: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class BattingViewHolder(val binding: RowBatsmenStatsBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class BowlingViewHolder(val binding: RowBowlersStatsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = when {
        isFutsal && isBatting -> goalScorerItems.size
        isFutsal && !isBatting -> assistantItems.size
        !isFutsal && isBatting -> battingItems.size
        else -> bowlingItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when {
            isFutsal -> {
                if (isBatting) {
                    BattingViewHolder(RowBatsmenStatsBinding.inflate(inflater, parent, false))
                } else {
                    BowlingViewHolder(RowBowlersStatsBinding.inflate(inflater, parent, false))
                }
            }
            isBatting -> BattingViewHolder(RowBatsmenStatsBinding.inflate(inflater, parent, false))
            else -> BowlingViewHolder(RowBowlersStatsBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val rank = position + 1
        val rankBg = getRankBackground(rank)
        when {
            isFutsal && isBatting && holder is BattingViewHolder -> {
                 val scorer = goalScorerItems[position]
                holder.binding.apply {
                    tvRank.text = rank.toString()
                    tvRank.setBackgroundResource(rankBg)
                    tvPlayerName.text = scorer.playerName
                    tvRuns.text = "${scorer.goals}"                          // Goals
                    tvBalls.text = "${scorer.assists}"                       // Assists
                    tvFours.text = "${scorer.goals + scorer.assists}"        // G+A
                    tvSixes.text = "${scorer.yellowCards}"                   // YC 🟨
                    tvPom.text = "${scorer.redCards}"                        // RC 🟥
                }
            }


            !isFutsal && isBatting && holder is BattingViewHolder -> {
                val player = battingItems[position]
                holder.binding.apply {
                    tvRank.text = rank.toString()
                    tvRank.setBackgroundResource(rankBg)
                    tvPlayerName.text = player.playerName
                    tvRuns.text = player.runs.toString()
                    tvBalls.text = player.ballsFaced.toString()
                    tvFours.text = player.fours.toString()
                    tvSixes.text = player.sixes.toString()
                    tvPom.text = player.playerOfMatchCount.toString()
                }
            }

            !isFutsal && !isBatting && holder is BowlingViewHolder -> {
                val player = bowlingItems[position]
                holder.binding.apply {
                    tvRank.text = rank.toString()
                    tvRank.setBackgroundResource(rankBg)
                    tvPlayerName.text = player.playerName
                    tvWickets.text = player.wickets.toString()
                    tvRuns.text = player.runs.toString()
                    tvBalls.text = player.ballsBowled.toString()
                    tvEconomy.text = String.format("%.2f", player.economy)
                    tvPom.text = player.playerOfMatchCount.toString()
                }
            }
        }
    }

    private fun getRankBackground(rank: Int): Int = when (rank) {
        1 -> R.drawable.bg_rank_gold
        2 -> R.drawable.bg_rank_silver
        3 -> R.drawable.bg_rank_bronze
        else -> R.drawable.bg_rank_default
    }
}