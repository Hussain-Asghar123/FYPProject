package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.LudoEvent

class LudoEventAdapter(
    private val events: MutableList<LudoEvent>,
    private val onEventClick: (LudoEvent) -> Unit
) : RecyclerView.Adapter<LudoEventAdapter.EventViewHolder>() {

    inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEventType  : TextView = view.findViewById(R.id.tvEventType)
        val tvTeamName   : TextView = view.findViewById(R.id.tvTeamName)
        val tvPlayerName : TextView = view.findViewById(R.id.tvPlayerName)
        val tvTime       : TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ludo_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        val icon = when (event.eventType?.uppercase()) {
            "HOME_RUN"  -> "🏠"
            "CAPTURE"   -> "⚔️"
            "WIN"       -> "🏆"
            "END_MATCH" -> "🏁"
            else        -> "📌"
        }

        holder.tvEventType.text  = "$icon ${event.eventType?.replace("_", " ") ?: ""}"
        holder.tvTeamName.text   = event.teamName   ?: ""
        holder.tvPlayerName.text = event.playerName ?: ""

        val mins = (event.eventTimeSeconds ?: 0) / 60
        holder.tvTime.text = "${mins}'"

        holder.itemView.setOnClickListener { onEventClick(event) }
    }

    override fun getItemCount() = events.size
}