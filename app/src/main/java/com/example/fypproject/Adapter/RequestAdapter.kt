package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PlayerRequestDto
import com.example.fypproject.DTO.TeamRequestDto
import com.example.fypproject.databinding.ItemRequestBinding

class RequestAdapter<T>(
    private val requestList: MutableList<T>,
    private val onApprove: (T, Int) -> Unit,
    private val onReject: (T, Int) -> Unit
) : RecyclerView.Adapter<RequestAdapter<T>.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = requestList[position]

        with(holder.binding) {
            when (item) {
                is PlayerRequestDto -> {
                    requestTitle.text = item.teamName
                    requestCreator.text = "Owner: ${item.teamCreatorName}"
                    requestStatus.text = "Status: ${item.status}"
                }
                is TeamRequestDto -> {
                    requestTitle.text = item.teamName
                    requestCreator.text = "Tournament: ${item.tournamentName}"
                    requestStatus.text = "Status: ${item.status}"
                    val playersText = item.players.joinToString(", ") { it.name }
                    requestCreator.text = "Tournament: ${item.tournamentName} | Players: $playersText"

                }
            }
            btnApprove.setOnClickListener { onApprove(item, position) }
            btnReject.setOnClickListener { onReject(item, position) }
        }
    }

    override fun getItemCount(): Int = requestList.size
}
