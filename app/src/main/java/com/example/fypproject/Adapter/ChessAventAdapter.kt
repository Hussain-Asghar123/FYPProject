package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.ChessEvent

class ChessEventAdapter(
    private val events: MutableList<ChessEvent>,
    private val onEventClick: (ChessEvent) -> Unit
) : RecyclerView.Adapter<ChessEventAdapter.EventViewHolder>() {

    inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEventType  : TextView = view.findViewById(R.id.tvEventType)
        val tvTeamName   : TextView = view.findViewById(R.id.tvTeamName)
        val tvNotation   : TextView = view.findViewById(R.id.tvNotation)
        val tvMoveNumber : TextView = view.findViewById(R.id.tvMoveNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chess_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        val icon = when (event.eventType?.uppercase()) {
            "MOVE"        -> "♟️"
            "CHECK"       -> "⚔️"
            "CHECKMATE"   -> "♛"
            "RESIGN"      -> "🏳️"
            "TIMEOUT"     -> "⏰"
            "STALEMATE"   -> "🤝"
            "DRAW_AGREED" -> "🤝"
            "END_MATCH"   -> "🏁"
            else          -> "📌"
        }

        holder.tvEventType.text  = "$icon ${event.eventType?.replace("_", " ") ?: ""}"
        holder.tvTeamName.text   = event.teamName ?: ""
        holder.tvNotation.text   = event.moveNotation ?: ""
        holder.tvMoveNumber.text = if (event.moveNumber != null) "#${event.moveNumber}" else ""

        holder.itemView.setOnClickListener { onEventClick(event) }
    }

    override fun getItemCount() = events.size
}