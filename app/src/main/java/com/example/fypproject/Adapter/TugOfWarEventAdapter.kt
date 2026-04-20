package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.TugOfWarEvent

class TugOfWarEventAdapter(
    private val events: MutableList<TugOfWarEvent>
) : RecyclerView.Adapter<TugOfWarEventAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEventType: TextView = view.findViewById(R.id.tvEventType)
        val tvWinnerTeam: TextView = view.findViewById(R.id.tvWinnerTeam)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvRoundNumber: TextView = view.findViewById(R.id.tvRoundNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tug0fwar_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ev = events[position]
        holder.tvEventType.text = when (ev.eventType) {
            "ROUND_WIN" -> "💪 Round Win"
            "END_MATCH" -> "🏁 Match End"
            else -> ev.eventType.orEmpty()
        }
        holder.tvWinnerTeam.text = ev.winnerTeamName.orEmpty()
        holder.tvRoundNumber.text = "Round ${ev.roundNumber ?: ""}"
        if (ev.roundDurationSeconds != null && ev.roundDurationSeconds > 0) {
            val m = ev.roundDurationSeconds / 60
            val s = ev.roundDurationSeconds % 60
            holder.tvDuration.text = "Duration: ${m}m ${s}s"
        } else {
            holder.tvDuration.text = ""
        }
    }

    override fun getItemCount() = events.size
}