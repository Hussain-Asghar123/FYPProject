package com.example.fypproject.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.StatRowItem
import com.example.fypproject.databinding.ItemStatsRowBinding

class StatsAdapter : ListAdapter<StatRowItem, StatsAdapter.StatViewHolder>(DiffCallback()) {

    inner class StatViewHolder(
        private val binding: ItemStatsRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StatRowItem) {
            binding.tvStatLabel.text = item.label
            binding.tvVal1.text = item.val1
            binding.tvVal2.text = item.val2

            // Reset backgrounds
            binding.tvVal1.setBackgroundColor(Color.TRANSPARENT)
            binding.tvVal2.setBackgroundColor(Color.TRANSPARENT)
            binding.tvVal1.setTextColor(Color.parseColor("#374151"))
            binding.tvVal2.setTextColor(Color.parseColor("#374151"))

            when {
                item.isTie -> {
                    binding.tvVal1.setBackgroundColor(Color.parseColor("#F9FAFB"))
                    binding.tvVal2.setBackgroundColor(Color.parseColor("#F9FAFB"))
                    binding.tvVal1.setTextColor(Color.parseColor("#6B7280"))
                    binding.tvVal2.setTextColor(Color.parseColor("#6B7280"))
                }
                item.p1Wins -> {
                    // P1 green (winner), P2 red (loser)
                    binding.tvVal1.setBackgroundColor(Color.parseColor("#F0FDF4"))
                    binding.tvVal1.setTextColor(Color.parseColor("#16A34A"))
                    binding.tvVal2.setBackgroundColor(Color.parseColor("#FFF5F5"))
                    binding.tvVal2.setTextColor(Color.parseColor("#DC2626"))
                }
                item.p2Wins -> {
                    // P2 green (winner), P1 red (loser)
                    binding.tvVal2.setBackgroundColor(Color.parseColor("#F0FDF4"))
                    binding.tvVal2.setTextColor(Color.parseColor("#16A34A"))
                    binding.tvVal1.setBackgroundColor(Color.parseColor("#FFF5F5"))
                    binding.tvVal1.setTextColor(Color.parseColor("#DC2626"))
                }
            }

            // Bold winner value
            binding.tvVal1.setTypeface(null,
                if (item.p1Wins) android.graphics.Typeface.BOLD
                else android.graphics.Typeface.NORMAL
            )
            binding.tvVal2.setTypeface(null,
                if (item.p2Wins) android.graphics.Typeface.BOLD
                else android.graphics.Typeface.NORMAL
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val binding = ItemStatsRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<StatRowItem>() {
        override fun areItemsTheSame(a: StatRowItem, b: StatRowItem) = a.label == b.label
        override fun areContentsTheSame(a: StatRowItem, b: StatRowItem) = a == b
    }
}