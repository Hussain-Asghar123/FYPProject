package com.example.fypproject.Adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PtsTableDto
import com.example.fypproject.databinding.ItemCricketRowBinding
import com.example.fypproject.databinding.ItemFutsalRowBinding

class PtsTableAdapter(
    private var items: List<PtsTableDto>,
    forcedSport: String? = null
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CRICKET = 0
        private const val TYPE_FUTSAL  = 1
    }

    private var tableSport: String? = normalizeSport(forcedSport)

    private fun isFutsal(): Boolean {
        val fromRows = normalizeSport(items.firstOrNull()?.sport)
        return (tableSport ?: fromRows) == "futsal"
    }

    private fun normalizeSport(sport: String?): String? {
        val normalized = sport?.trim()?.lowercase()
        return when (normalized) {
            "futsal", "football" -> "futsal"
            "cricket" -> "cricket"
            else -> null
        }
    }

    override fun getItemViewType(position: Int) =
        if (isFutsal()) TYPE_FUTSAL else TYPE_CRICKET

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

    private fun setRankDot(tv: android.widget.TextView, rank: Int) {
        tv.text = rank.toString()
        val color = when (rank) {
            1    -> Color.parseColor("#FACC15") // gold
            2    -> Color.parseColor("#D1D5DB") // silver
            3    -> Color.parseColor("#FB923C") // bronze
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
        return if (viewType == TYPE_FUTSAL) {
            FutsalVH(ItemFutsalRowBinding.inflate(inflater, parent, false))
        } else {
            CricketVH(ItemCricketRowBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CricketVH -> holder.bind(items[position], position)
            is FutsalVH  -> holder.bind(items[position], position)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<PtsTableDto>, sport: String? = null) {
        tableSport = normalizeSport(sport) ?: tableSport
        items = newItems
        notifyDataSetChanged()
    }
}