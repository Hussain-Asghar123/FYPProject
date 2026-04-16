package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.FutsalEventDTO

class FutsalEventsAdapter(
    private val events: MutableList<FutsalEventDTO>,
    private val onEventClick: (FutsalEventDTO) -> Unit
) : RecyclerView.Adapter<FutsalEventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMinute: TextView = view.findViewById(R.id.tvEventMinute)
        val tvType: TextView = view.findViewById(R.id.tvEventType)
        val tvTeam: TextView = view.findViewById(R.id.tvEventTeam)
        val tvPlayer: TextView = view.findViewById(R.id.tvEventPlayer)
        val tvExtra: TextView = view.findViewById(R.id.tvEventExtra)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.tvMinute.text = "${event.eventTimeSeconds / 60}'"

        holder.tvPlayer.text = event.scorerName?.takeIf { it.isNotBlank() } ?: "Unknown Player"

        val team = event.teamName?.takeIf { it.isNotBlank() }
        if (team != null && team != "Unknown") {
            holder.tvTeam.visibility = View.VISIBLE
            holder.tvTeam.text = team
        } else {
            holder.tvTeam.visibility = View.GONE
        }

        val extra = event.assistPlayerName?.takeIf { it.isNotBlank() }
        if (extra != null) {
            holder.tvExtra.visibility = View.VISIBLE
            holder.tvExtra.text = when (event.eventType) {
                "GOAL"         -> "Assist: $extra"
                "OWN_GOAL"     -> "Assist: $extra"
                "SUBSTITUTION" -> "IN: $extra  OUT: ${event.outPlayerName ?: ""}"
                else           -> extra
            }
        } else {
            holder.tvExtra.visibility = View.GONE
        }

        holder.tvType.text = when (event.eventType) {
            "GOAL"         -> " Goal"
            "OWN_GOAL"     -> " Own Goal"
            "YELLOW_CARD"  -> " Yellow Card"
            "RED_CARD"     -> " Red Card"
            "FOUL"         -> " Foul"
            "SUBSTITUTION" -> " Substitution"
            else           -> event.eventType
        }

        holder.itemView.setOnClickListener { onEventClick(event) }
    }

    override fun getItemCount(): Int = events.size
}