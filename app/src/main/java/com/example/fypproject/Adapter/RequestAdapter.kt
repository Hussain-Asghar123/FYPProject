package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.PlayerRequestDto
import com.example.fypproject.DTO.TeamRequestDto
import com.example.fypproject.R
import com.example.fypproject.databinding.ItemRequestBinding

class RequestAdapter<T>(
    private val requestList: MutableList<T>,
    private val onApprove: (T, Int) -> Unit,
    private val onReject: (T, Int) -> Unit
) : RecyclerView.Adapter<RequestAdapter<T>.ViewHolder>() {

    private var expandedPosition = -1

    inner class ViewHolder(val binding: ItemRequestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = requestList[position]
        val isExpanded = position == expandedPosition

        with(holder.binding) {

            when (item) {

                is PlayerRequestDto -> {
                    requestTitle.text = item.teamName ?: "Unknown Team"
                    requestCreator.text = "Creator: ${item.teamCreatorName ?: "Unknown Creator"}"
                    requestStatus.text = "Status: ${item.status}"

                    chevron.visibility = View.GONE
                    playersContainer.visibility = View.GONE
                    cardRoot.isClickable = false
                }

                is TeamRequestDto -> {
                    requestTitle.text = item.teamName
                    requestCreator.text = "Captain: ${item.CaptainName ?: "N/A"}"
                    requestStatus.text = "Status: ${item.status}"

                    chevron.visibility = View.VISIBLE
                    chevron.animate().rotation(if (isExpanded) 180f else 0f).setDuration(200)
                        .start()

                    if (isExpanded) {
                        playersContainer.visibility = View.VISIBLE
                        playersListContainer.removeAllViews()

                        if (item.players.isNotEmpty()) {
                            item.players.forEach { player ->
                                val row = LayoutInflater.from(root.context)
                                    .inflate(R.layout.item_player_row, playersListContainer, false)
                                row.findViewById<TextView>(R.id.player_name).text = player.name
                                row.findViewById<TextView>(R.id.player_username).text =
                                    "@${player.username}"
                                playersListContainer.addView(row)
                            }
                        } else {
                            val empty = TextView(root.context).apply {
                                text = "No players listed."
                                textSize = 13f
                                setTextColor(0xFF9CA3AF.toInt())
                                setPadding(0, 4, 0, 4)
                            }
                            playersListContainer.addView(empty)
                        }
                    } else {
                        playersContainer.visibility = View.GONE
                    }

                    cardRoot.setOnClickListener {
                        val prev = expandedPosition
                        expandedPosition = if (isExpanded) -1 else holder.adapterPosition
                        if (prev != -1) notifyItemChanged(prev)
                        notifyItemChanged(holder.adapterPosition)
                    }
                }
            }

            val status = when (item) {
                is PlayerRequestDto -> item.status
                is TeamRequestDto -> item.status
                else -> ""
            }
            actionButtons.visibility =
                if (status?.uppercase() == "PENDING") View.VISIBLE else View.GONE

            btnApprove.setOnClickListener {
                it.parent.requestDisallowInterceptTouchEvent(true)
                onApprove(item, holder.adapterPosition)
            }
            btnReject.setOnClickListener {
                it.parent.requestDisallowInterceptTouchEvent(true)
                onReject(item, holder.adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int = requestList.size
}