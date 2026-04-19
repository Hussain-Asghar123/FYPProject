package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.BadmintonEvent

class BadmintionEventAdapter(
    private val events: MutableList<BadmintonEvent>,
    private val onEventClick: (BadmintonEvent) -> Unit
) : RecyclerView.Adapter<BadmintionEventAdapter.EventViewHolder>() {

    inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEventType  : TextView = view.findViewById(R.id.tvEventType)
        val tvTeamName   : TextView = view.findViewById(R.id.tvTeamName)
        val tvPlayerName : TextView = view.findViewById(R.id.tvPlayerName)
        val tvTime       : TextView = view.findViewById(R.id.tvTime)
        val tvSetNumber  : TextView = view.findViewById(R.id.tvSetNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badminton_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        val icon = when (event.eventType?.uppercase()) {
            "POINT"        -> "🏸"
            "ACE"          -> "🎯"
            "FOUL"         -> "⚠️"
            "NET_FAULT"    -> "🔴"
            "SERVICE_FAULT"-> "⚡"
            "SUBSTITUTION" -> "🔄"
            "END_SET"      -> "🔔"
            else           -> "📌"
        }

        holder.tvEventType.text  = "$icon ${event.eventType?.replace("_", " ") ?: ""}"
        holder.tvTeamName.text   = event.teamName   ?: ""
        holder.tvPlayerName.text = event.playerName ?: ""

        val mins = (event.eventTimeSeconds ?: 0) / 60
        holder.tvTime.text      = "${mins}'"
        holder.tvSetNumber.text = if (event.gameNumber != null) "G${event.gameNumber}" else ""

        holder.itemView.setOnClickListener { onEventClick(event) }
    }

    override fun getItemCount() = events.size
}