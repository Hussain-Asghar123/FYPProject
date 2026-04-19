package com.example.fypproject.Adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PtsTableDto
import com.example.fypproject.databinding.ItemBadmintonRowBinding
import com.example.fypproject.databinding.ItemCricketRowBinding
import com.example.fypproject.databinding.ItemFutsalRowBinding
import com.example.fypproject.databinding.ItemVolleyballRowBinding

class PtsTableAdapter(
    private var items: List<PtsTableDto>,
    forcedSport: String? = null
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //naya sport ka lia yahan changes required han
    companion object {
        private const val TYPE_CRICKET = 0
        private const val TYPE_FUTSAL  = 1
        private const val TYPE_VOLLEYBALL = 2

        private const val TYPE_BADMINTON = 3
    }

    private var tableSport: String? = normalizeSport(forcedSport)


    //naya sport ka lia yahan changes required han
    private fun normalizeSport(sport: String?): String? {
        val normalized = sport?.trim()?.lowercase()
        return when (normalized) {
            "futsal", "football" -> "futsal"
            "cricket" -> "cricket"
            "volleyball" -> "volleyball"
            "badminton" -> "badminton"
            else -> null
        }
    }
    //naya sport ka lia yahan changes required han
    override fun getItemViewType(position: Int) = when (tableSport ?: normalizeSport(items.firstOrNull()?.sport)) {
        "futsal"     -> TYPE_FUTSAL
        "volleyball" -> TYPE_VOLLEYBALL
        "badminton" -> TYPE_BADMINTON
        else         -> TYPE_CRICKET
    }

    inner class CricketVH(private val b: ItemCricketRowBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: PtsTableDto, position: Int) {
            b.apply {

                setRankDot(tvRank, position + 1)

                tvTeamName.text = item.teamName
                tvMatches.text  = item.played.toString()

                tvWins.text = item.wins.toString()
                tvWins.setTextColor(Color.parseColor("#16A34A"))

                tvLosses.text = item.losses.toString()
                tvLosses.setTextColor(Color.parseColor("#EF4444"))

                tvPoints.text = item.points.toString()

                val nrr = item.nrr
                tvNRR.text = when {
                    nrr > 0 -> "+%.3f".format(nrr)
                    nrr < 0 -> "%.3f".format(nrr)
                    else    -> "0.000"
                }
                tvNRR.setTextColor(nrrColor(nrr))

                root.setCardBackgroundColor(rowBg(position))
            }
        }
    }

    inner class FutsalVH(private val b: ItemFutsalRowBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: PtsTableDto, position: Int) {
            b.apply {

                setRankDot(tvRank, position + 1)

                tvTeamName.text = item.teamName
                tvMatches.text  = item.played.toString()

                tvWins.text = item.wins.toString()
                tvWins.setTextColor(Color.parseColor("#16A34A"))

                tvDraws.text = (item.draws ?: 0).toString()
                tvDraws.setTextColor(Color.parseColor("#F59E0B"))


                tvLosses.text = item.losses.toString()
                tvLosses.setTextColor(Color.parseColor("#EF4444"))

                tvGoalsFor.text     = (item.goalsFor ?: 0).toString()
                tvGoalsAgainst.text = (item.goalsAgainst ?: 0).toString()

                val gd = item.goalDifference
                    ?: ((item.goalsFor ?: 0) - (item.goalsAgainst ?: 0))
                tvGD.text = when {
                    gd > 0 -> "+$gd"
                    else   -> "$gd"
                }
                tvGD.setTextColor(
                    when {
                        gd > 0 -> Color.parseColor("#16A34A") // green
                        gd < 0 -> Color.parseColor("#EF4444") // red
                        else   -> Color.parseColor("#6B7280") // gray
                    }
                )

                tvPoints.text = item.points.toString()

                root.setCardBackgroundColor(rowBg(position))
            }
        }
    }
    inner class VolleyballVH(private val b: ItemVolleyballRowBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: PtsTableDto, position: Int) {
            b.apply{
                setRankDot(tvRank, position + 1)

                tvTeamName.text = item.teamName
                tvMatches.text  = item.played.toString()

                tvWins.text = item.wins.toString()
                tvWins.setTextColor(Color.parseColor("#16A34A"))

                tvLosses.text = item.losses.toString()
                tvLosses.setTextColor(Color.parseColor("#EF4444"))

                tvSetsWinns.text = (item.setsWons ?: 0).toString()
                tvSetsLosses.text = (item.setsLosses ?: 0).toString()

                val sd = item.setDifference
                    ?: ((item.setsWons ?: 0) - (item.setsLosses ?: 0))
                tvSetsDifference.text = when {
                    sd > 0 -> "+$sd"
                    else -> "$sd"
                }
                tvSetsDifference.setTextColor(
                    when {
                        sd > 0 -> Color.parseColor("#16A34A") // green
                        sd < 0 -> Color.parseColor("#EF4444") // red
                        else -> Color.parseColor("#6B7280") // gray
                    }
                )
                tvPoints.text = item.points.toString()
                root.setCardBackgroundColor(rowBg(position))

            }
        }
    }

    inner class BadmintonVH(private val b: ItemBadmintonRowBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: PtsTableDto, position: Int) {
            b.apply {
                setRankDot(tvRank, position + 1)
                tvTeamName.text = item.teamName
                tvMatches.text  = item.played.toString()

                tvWins.text = item.wins.toString()
                tvWins.setTextColor(Color.parseColor("#16A34A"))

                tvLosses.text = item.losses.toString()
                tvLosses.setTextColor(Color.parseColor("#EF4444"))

                tvPoints.text = item.points.toString()

                root.setCardBackgroundColor(rowBg(position))
            }
        }
    }

    private fun setRankDot(tv: android.widget.TextView, rank: Int) {
        tv.text = rank.toString()
        val color = when (rank) {
            1    -> Color.parseColor("#FACC15")
            2    -> Color.parseColor("#D1D5DB")
            3    -> Color.parseColor("#FB923C")
            else -> Color.parseColor("#E5E7EB")
        }
        tv.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun nrrColor(nrr: Double) = when {
        nrr > 0 -> Color.parseColor("#16A34A")
        nrr < 0 -> Color.parseColor("#EF4444")
        else    -> Color.parseColor("#6B7280")
    }

    private fun rowBg(position: Int) =
        if (position % 2 == 0) Color.WHITE else Color.parseColor("#F9FAFB")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FUTSAL     -> FutsalVH(ItemFutsalRowBinding.inflate(inflater, parent, false))
            TYPE_VOLLEYBALL -> VolleyballVH(ItemVolleyballRowBinding.inflate(inflater, parent, false))
            TYPE_BADMINTON -> BadmintonVH(ItemBadmintonRowBinding.inflate(inflater, parent, false))
            else            -> CricketVH(ItemCricketRowBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CricketVH    -> holder.bind(items[position], position)
            is FutsalVH     -> holder.bind(items[position], position)
            is VolleyballVH -> holder.bind(items[position], position)
            is BadmintonVH -> holder.bind(items[position], position)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<PtsTableDto>, sport: String? = null) {
        tableSport = normalizeSport(sport) ?: tableSport
        items = newItems
        notifyDataSetChanged()
    }
}