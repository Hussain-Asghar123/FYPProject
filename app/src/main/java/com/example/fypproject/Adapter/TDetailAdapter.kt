package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.TournamentResponse
import com.example.fypproject.databinding.ItemSportsBinding

class TDetailAdapter (
    private val list:MutableList<TournamentResponse>,
    private val onItemCLick: (TournamentResponse) -> Unit
): RecyclerView.Adapter<TDetailAdapter.ViewHolder>() {
    inner class ViewHolder(private val binding: ItemSportsBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(item:TournamentResponse){
            binding.tvSportName.text = item.name.ifBlank { "Unnamed Tournament" }
            binding.root.setOnClickListener {
                onItemCLick(item)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSportsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }
    override fun getItemCount(): Int = list.size

    fun setData(newList:List<TournamentResponse>){
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}