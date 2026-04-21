package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.TugOfWarEvent

class TugOfWarEventAdapter(
    private val events: MutableList<TugOfWarEvent>,
    private val onEventClick: ((TugOfWarEvent) -> Unit)? = null
) : RecyclerView.Adapter<TugOfWarEventAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val onEventClick: ((TugOfWarEvent) -> Unit)? = null) : RecyclerView.ViewHolder(view) {
        val tvEventType: TextView = view.findViewById(R.id.tvEventType)
        val tvWinnerTeam: TextView = view.findViewById(R.id.tvWinnerTeam)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvRoundNumber: TextView = view.findViewById(R.id.tvRoundNumber)

        fun bind(event: TugOfWarEvent) {
            tvEventType.text = when (event.eventType) {
                "ROUND_WIN" -> "💪 Round Win"
                "END_MATCH" -> "🏁 Match End"
                else -> event.eventType.orEmpty()
            }
            tvWinnerTeam.text = event.winnerTeamName.orEmpty()
            tvRoundNumber.text = "Round ${event.roundNumber ?: ""}"
            if (event.roundDurationSeconds != null && event.roundDurationSeconds > 0) {
                val m = event.roundDurationSeconds / 60
                val s = event.roundDurationSeconds % 60
                tvDuration.text = "Duration: ${m}m ${s}s"
            } else {
                tvDuration.text = ""
            }
            itemView.setOnClickListener { onEventClick?.invoke(event) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tug0fwar_event, parent, false)
        return ViewHolder(view, onEventClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size
}