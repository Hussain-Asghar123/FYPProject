package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PlayerAwardDto
import com.example.fypproject.databinding.ItemPomAwardBinding

class PomAwardAdapter(
    private val items: List<PlayerAwardDto>
) : RecyclerView.Adapter<PomAwardAdapter.VH>() {

    inner class VH(val binding: ItemPomAwardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPomAwardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val award = items[position]
        holder.binding.tvPomPlayerName.text = award.playerName
        holder.binding.tvPomReason.text     = award.reason.orEmpty()
    }
}