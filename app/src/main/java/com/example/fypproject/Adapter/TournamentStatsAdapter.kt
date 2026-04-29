package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.TopBatsmanDto
import com.example.fypproject.DTO.TopBowlerDto
import com.example.fypproject.DTO.TopFutsalAssistantDto
import com.example.fypproject.DTO.TopFutsalScorerDto
import com.example.fypproject.R
import com.example.fypproject.databinding.RowBatsmenStatsBinding
import com.example.fypproject.databinding.RowBowlersStatsBinding
import java.util.Locale

class TournamentStatsAdapter(
    private val sportType: String,
    private val isBatting: Boolean,
    private val battingItems: List<TopBatsmanDto>           = emptyList(),
    private val bowlingItems: List<TopBowlerDto>            = emptyList(),
    private val goalScorerItems: List<TopFutsalScorerDto>   = emptyList(),
    private val assistantItems: List<TopFutsalAssistantDto> = emptyList(),
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class BattingViewHolder(val binding: RowBatsmenStatsBinding) :
        RecyclerView.ViewHolder(binding.root)

    class BowlingViewHolder(val binding: RowBowlersStatsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = when (sportType) {
        SPORT_FUTSAL, SPORT_VOLLEYBALL, SPORT_BADMINTON,
        SPORT_TABLETENNIS, SPORT_TUG_OF_WAR, SPORT_LUDO, SPORT_CHESS ->
            if (isBatting) goalScorerItems.size else assistantItems.size
        else ->
            if (isBatting) battingItems.size else bowlingItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (isBatting)
            BattingViewHolder(RowBatsmenStatsBinding.inflate(inf, parent, false))
        else
            BowlingViewHolder(RowBowlersStatsBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val rank  = position + 1
        val rankBg = getRankBackground(rank)

        when (sportType) {
            SPORT_CRICKET -> {
                if (isBatting && holder is BattingViewHolder)
                    bindCricketBatting(holder.binding, battingItems[position], rank, rankBg)
                else if (!isBatting && holder is BowlingViewHolder)
                    bindCricketBowling(holder.binding, bowlingItems[position], rank, rankBg)
            }
            SPORT_FUTSAL -> {
                if (isBatting && holder is BattingViewHolder)
                    bindFutsalScorers(holder.binding, goalScorerItems[position], rank, rankBg)
                else if (!isBatting && holder is BowlingViewHolder)
                    bindFutsalAssistants(holder.binding, assistantItems[position], rank, rankBg)
            }
            SPORT_VOLLEYBALL -> {
                if (isBatting && holder is BattingViewHolder)
                    bindVolleyballScorers(holder.binding, goalScorerItems[position], rank, rankBg)
                else if (!isBatting && holder is BowlingViewHolder)
                    bindVolleyballServers(holder.binding, assistantItems[position], rank, rankBg)
            }
            SPORT_BADMINTON -> {
                if (isBatting && holder is BattingViewHolder)
                    bindBadmintonScorers(holder.binding, goalScorerItems[position], rank, rankBg)
            }
            SPORT_TABLETENNIS -> {
                if (isBatting && holder is BattingViewHolder)
                    bindTableTennisScorers(holder.binding, goalScorerItems[position], rank, rankBg)
            }
            SPORT_TUG_OF_WAR -> {
                if (isBatting && holder is BattingViewHolder)
                    bindTugOfWarScorers(holder.binding, goalScorerItems[position], rank, rankBg)
            }
            SPORT_LUDO -> {
                if (isBatting && holder is BattingViewHolder)
                    bindLudoScorers(holder.binding, goalScorerItems[position], rank, rankBg)
            }
            SPORT_CHESS -> {
                if (isBatting && holder is BattingViewHolder)
                    bindChessScorers(holder.binding, goalScorerItems[position], rank, rankBg)
            }
        }
    }

    // ── Cricket Batting: Runs | Balls | 4s | 6s | POM ────────────────
    private fun bindCricketBatting(b: RowBatsmenStatsBinding, p: TopBatsmanDto, rank: Int, bg: Int) {
        showAllBattingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvRuns.text  = p.runs.toString()
        b.tvBalls.text = p.ballsFaced.toString()
        b.tvFours.text = p.fours.toString()
        b.tvSixes.text = p.sixes.toString()
        b.tvPom.text   = p.playerOfMatchCount.toString()
    }

    // ── Cricket Bowling: Wkts | runsConceded | Balls | Eco | POM ─────
    // JS uses p.runsConceded ?? 0 — so we use runsConceded field
    private fun bindCricketBowling(b: RowBowlersStatsBinding, p: TopBowlerDto, rank: Int, bg: Int) {
        showAllBowlingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvWickets.text = p.wickets.toString()
        b.tvRuns.text    = p.runsConceded.toString()   // JS: p.runsConceded ?? 0
        b.tvBalls.text   = p.ballsBowled.toString()
        b.tvEconomy.text = String.format(Locale.US, "%.2f", p.economy)
        b.tvPom.text     = p.playerOfMatchCount.toString()
    }

    // ── Futsal Scorers: Goals | Assists | G+A | YC | RC ──────────────
    private fun bindFutsalScorers(b: RowBatsmenStatsBinding, p: TopFutsalScorerDto, rank: Int, bg: Int) {
        showAllBattingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvRuns.text  = p.goals.toString()
        b.tvBalls.text = p.assists.toString()
        b.tvFours.text = (p.goals + p.assists).toString()  // G+A
        b.tvSixes.text = p.yellowCards.toString()
        b.tvPom.text   = p.redCards.toString()
    }

    // ── Futsal Assisters: Assists | Goals | G+A ───────────────────────
    private fun bindFutsalAssistants(b: RowBowlersStatsBinding, p: TopFutsalAssistantDto, rank: Int, bg: Int) {
        hideExtraBowlingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvWickets.text = p.assists.toString()
        b.tvRuns.text    = p.goals.toString()
        b.tvPom.text     = (p.goals + p.assists).toString()  // G+A
    }

    // ── Volleyball Scorers: Points | Aces | Blocks | AttackErr | Fantasy
    // JS columns: ["Points", "Aces", "Blocks"]  using goals/assists/futsalFouls
    private fun bindVolleyballScorers(b: RowBatsmenStatsBinding, p: TopFutsalScorerDto, rank: Int, bg: Int) {
        showAllBattingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvRuns.text  = p.goals.toString()        // Points
        b.tvBalls.text = p.assists.toString()       // Aces
        b.tvFours.text = p.futsalFouls.toString()  // Blocks
        b.tvSixes.text = p.yellowCards.toString()   // AttackErr
        b.tvPom.text   = p.totalPoints.toString()   // Fantasy
    }

    // ── Volleyball Servers: Aces | Points | Fantasy ───────────────────
    private fun bindVolleyballServers(b: RowBowlersStatsBinding, p: TopFutsalAssistantDto, rank: Int, bg: Int) {
        hideExtraBowlingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvWickets.text = p.assists.toString()      // Aces
        b.tvRuns.text    = p.goals.toString()        // Points
        b.tvPom.text     = p.totalPoints.toString()  // Fantasy
    }

    // ── Badminton: Points | Smashes+Aces | Faults ─────────────────────
    private fun bindBadmintonScorers(b: RowBatsmenStatsBinding, p: TopFutsalScorerDto, rank: Int, bg: Int) {
        hideExtraBattingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvRuns.text  = p.goals.toString()        // Points
        b.tvBalls.text = p.assists.toString()       // Smashes+Aces
        b.tvFours.text = p.futsalFouls.toString()  // Faults
    }

    // ── Table Tennis: Points | Smashes+Aces | Faults ──────────────────
    private fun bindTableTennisScorers(b: RowBatsmenStatsBinding, p: TopFutsalScorerDto, rank: Int, bg: Int) {
        hideExtraBattingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvRuns.text  = p.goals.toString()        // Points
        b.tvBalls.text = p.assists.toString()       // Smashes+Aces
        b.tvFours.text = p.futsalFouls.toString()  // Faults
    }

    // ── Tug of War: Wins | Strength | POM ────────────────────────────
    private fun bindTugOfWarScorers(b: RowBatsmenStatsBinding, p: TopFutsalScorerDto, rank: Int, bg: Int) {
        hideExtraBattingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvRuns.text  = p.goals.toString()
        b.tvBalls.text = p.assists.toString()
        b.tvFours.text = p.playerOfMatchCount.toString()
    }

    // ── Ludo: Home Runs | Captures | POM ─────────────────────────────
    private fun bindLudoScorers(b: RowBatsmenStatsBinding, p: TopFutsalScorerDto, rank: Int, bg: Int) {
        hideExtraBattingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvRuns.text  = p.goals.toString()    // Home Runs
        b.tvBalls.text = p.assists.toString()  // Captures
        b.tvFours.text = p.playerOfMatchCount.toString()
    }

    // ── Chess: Wins | Checks | POM ────────────────────────────────────
    private fun bindChessScorers(b: RowBatsmenStatsBinding, p: TopFutsalScorerDto, rank: Int, bg: Int) {
        hideExtraBattingColumns(b)
        b.tvRank.text = rank.toString()
        b.tvRank.setBackgroundResource(bg)
        b.tvPlayerName.text = p.playerName
        b.tvRuns.text  = p.goals.toString()    // Wins
        b.tvBalls.text = p.assists.toString()  // Checks
        b.tvFours.text = p.playerOfMatchCount.toString()
    }

    // ── Visibility Helpers ────────────────────────────────────────────

    /** Cricket batting — show all 5 columns */
    private fun showAllBattingColumns(b: RowBatsmenStatsBinding) {
        b.tvSixes.visibility = View.VISIBLE
        b.tvPom.visibility   = View.VISIBLE
    }

    /** Badminton / TT / Ludo / Chess / TugOfWar — only 3 columns */
    private fun hideExtraBattingColumns(b: RowBatsmenStatsBinding) {
        b.tvSixes.visibility = View.GONE
        b.tvPom.visibility   = View.GONE
    }

    /** Cricket bowling — show Balls + Economy */
    private fun showAllBowlingColumns(b: RowBowlersStatsBinding) {
        b.tvBalls.visibility   = View.VISIBLE
        b.tvEconomy.visibility = View.VISIBLE
    }

    /** Futsal / Volleyball bowling — hide Balls + Economy */
    private fun hideExtraBowlingColumns(b: RowBowlersStatsBinding) {
        b.tvBalls.visibility   = View.GONE
        b.tvEconomy.visibility = View.GONE
    }

    private fun getRankBackground(rank: Int): Int = when (rank) {
        1    -> R.drawable.bg_rank_gold
        2    -> R.drawable.bg_rank_silver
        3    -> R.drawable.bg_rank_bronze
        else -> R.drawable.bg_rank_default
    }

    companion object {
        const val SPORT_CRICKET      = "cricket"
        const val SPORT_FUTSAL       = "futsal"
        const val SPORT_VOLLEYBALL   = "volleyball"
        const val SPORT_BADMINTON    = "badminton"
        const val SPORT_TABLETENNIS  = "table_tennis"
        const val SPORT_TUG_OF_WAR  = "tug_of_war"
        const val SPORT_LUDO         = "ludo"
        const val SPORT_CHESS        = "chess"
    }
}