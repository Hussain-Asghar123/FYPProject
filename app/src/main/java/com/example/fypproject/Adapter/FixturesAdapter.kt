package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.FixturesResponse
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
            binding.tvMatchTitle.text = "${fixture.team1Name} vs ${fixture.team2Name}"
            binding.tvVenueTime.text = "${fixture.date} ${fixture.time}"
            binding.root.setOnClickListener { onClick(fixture) }

            if (role.equals("ADMIN", true)) {
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnEdit.setOnClickListener { onEdit(fixture) }
            } else {
                binding.btnEdit.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemFixturesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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