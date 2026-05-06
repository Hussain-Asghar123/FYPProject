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
        val ev = events[position]

        // ✅ JS: icons exact match
        val icon = when (ev.eventType?.uppercase()) {
            "CHECKMATE"   -> "♟️"
            "DRAW_AGREED" -> "🤝"
            "STALEMATE"   -> "🤝"
            "RESIGN"      -> "🏳️"
            "TIMEOUT"     -> "⏰"
            else          -> "🏁"
        }

        // ✅ JS: RESULT_LABELS match
        val label = when (ev.eventType?.uppercase()) {
            "CHECKMATE"   -> "Checkmate"
            "DRAW_AGREED" -> "Draw Agreed"
            "STALEMATE"   -> "Stalemate"
            "RESIGN"      -> "Resignation"
            "TIMEOUT"     -> "Timeout"
            else          -> ev.eventType?.replace("_", " ") ?: ""
        }

        holder.tvEventType.text  = "$icon $label"
        holder.tvTeamName.text   = ev.teamName   ?: ""
        holder.tvNotation.text   = ev.moveNotation ?: ""
        holder.tvMoveNumber.text = if (ev.moveNumber != null) "#${ev.moveNumber}" else ""

        holder.itemView.setOnClickListener { onEventClick(ev) }
    }

    override fun getItemCount() = events.size
}