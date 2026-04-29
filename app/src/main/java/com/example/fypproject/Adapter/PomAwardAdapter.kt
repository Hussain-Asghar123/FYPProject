package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PlayerAwardDto
import com.example.fypproject.databinding.ItemPomAwardBinding

/**
 * Adapter for the "Player of the Match Awards" list.
 *
 * Requires a simple list item layout: res/layout/item_pom_award.xml
 *   <LinearLayout horizontal>
 *     <TextView android:id="@+id/tvPomPlayerName" />
 *     <TextView android:id="@+id/tvPomReason" />
 *   </LinearLayout>
 */
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