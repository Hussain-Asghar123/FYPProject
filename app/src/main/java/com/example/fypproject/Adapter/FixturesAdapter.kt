package com.example.fypproject.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.FixturesResponse
import com.example.fypproject.DTO.MatchStatus
import com.example.fypproject.databinding.ItemFixturesBinding

class FixturesAdapter(
    private val matches: MutableList<FixturesResponse>,
    private var role: String,
    private val onClick: (FixturesResponse) -> Unit,
    private val onEdit: (FixturesResponse) -> Unit
) : RecyclerView.Adapter<FixturesAdapter.MatchViewHolder>() {

    inner class MatchViewHolder(
        private val binding: ItemFixturesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fixture: FixturesResponse) {

            // ── Team Names ──────────────────────────────────────────
            binding.tvTeam1Name.text = fixture.team1Name.ifEmpty { "TBD" }
            binding.tvTeam2Name.text = fixture.team2Name.ifEmpty { "TBD" }

            // tvMatchTitle is kept for binding compatibility (hidden via gone)
            binding.tvMatchTitle.text = "${fixture.team1Name} vs ${fixture.team2Name}"

            // ── Winner Highlight (green = winner, gray = loser) ─────
            when {
                fixture.winnerTeamId == null -> {
                    // Match abhi complete nahi hua
                    binding.tvTeam1Name.setTextColor(Color.parseColor("#212121"))
                    binding.tvTeam2Name.setTextColor(Color.parseColor("#212121"))
                }
                fixture.winnerTeamId == fixture.team1Id -> {
                    binding.tvTeam1Name.setTextColor(Color.parseColor("#16A34A")) // green
                    binding.tvTeam2Name.setTextColor(Color.parseColor("#9CA3AF")) // gray
                }
                fixture.winnerTeamId == fixture.team2Id -> {
                    binding.tvTeam2Name.setTextColor(Color.parseColor("#16A34A")) // green
                    binding.tvTeam1Name.setTextColor(Color.parseColor("#9CA3AF")) // gray
                }
                else -> {
                    binding.tvTeam1Name.setTextColor(Color.parseColor("#212121"))
                    binding.tvTeam2Name.setTextColor(Color.parseColor("#212121"))
                }
            }

            // ── Group Name Badge ─────────────────────────────────────
            if (!fixture.groupName.isNullOrEmpty()) {
                binding.tvGroupName.visibility = View.VISIBLE
                binding.tvGroupName.text = fixture.groupName
            } else {
                binding.tvGroupName.visibility = View.GONE
            }

            // ── Date & Venue/Time ────────────────────────────────────
            binding.tvDate.text = "📅  ${fixture.date}"
            binding.tvVenueTime.text = "🕐  ${fixture.time}   |   📍 ${fixture.venue}"

            // ── Status Badge ─────────────────────────────────────────
            when (fixture.status) {
                "LIVE" -> {
                    binding.tvStatus.visibility = View.VISIBLE
                    binding.tvStatus.text = "● LIVE"
                    binding.tvStatus.setBackgroundColor(Color.parseColor("#E31212"))
                }
                "UPCOMING" -> {
                    binding.tvStatus.visibility = View.VISIBLE
                    binding.tvStatus.text = "UPCOMING"
                    binding.tvStatus.setBackgroundColor(Color.parseColor("#2563EB"))
                }
                "COMPLETED" -> {
                    binding.tvStatus.visibility = View.VISIBLE
                    binding.tvStatus.text = "DONE"
                    binding.tvStatus.setBackgroundColor(Color.parseColor("#16A34A"))
                }
                else -> {
                    binding.tvStatus.visibility = View.GONE
                }
            }

            // ── Edit Button (ADMIN only) ──────────────────────────────
            if (role.equals("ADMIN", ignoreCase = true)) {
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnEdit.setOnClickListener { onEdit(fixture) }
            } else {
                binding.btnEdit.visibility = View.GONE
            }

            // ── Click to open scoring ────────────────────────────────
            binding.root.setOnClickListener { onClick(fixture) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemFixturesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(matches[position])
    }

    override fun getItemCount(): Int = matches.size

    fun updateRole(newRole: String) {
        this.role = newRole
        notifyDataSetChanged()
    }
}