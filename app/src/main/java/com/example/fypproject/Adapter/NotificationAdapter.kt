package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.NotificationDto
import com.example.fypproject.R

class NotificationAdapter(
    private var currentList: List<NotificationDto>,
    private val onItemClick: (NotificationDto) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle   : TextView = itemView.findViewById(R.id.tvNotifTitle)
        val tvMessage : TextView = itemView.findViewById(R.id.tvNotifMessage)
        val tvTime    : TextView = itemView.findViewById(R.id.tvNotifTime)
        val tvType    : TextView = itemView.findViewById(R.id.tvNotifType)  // ✅ XML mein exist karta hai
        val dotUnread : View     = itemView.findViewById(R.id.dotUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun getItemCount(): Int = currentList.size

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val notif = currentList[position]

        holder.tvTitle.text   = notif.title   ?: "Notification"
        holder.tvMessage.text = notif.message ?: ""
        holder.tvTime.text    = formatTime(notif.createdAt)

        // ✅ Type based emoji — ab yeh set ho raha hai
        holder.tvType.text = when (notif.type) {
            "MATCH_START"   -> "🏏"
            "SCORE_UPDATE"  -> "📊"
            "RESULT"        -> "🏆"
            "MATCH_REQUEST" -> "📩"
            else            -> "🔔"
        }

        // Unread dot
        holder.dotUnread.visibility = if (!notif.isRead) View.VISIBLE else View.INVISIBLE

        // Unread background highlight
        holder.itemView.setBackgroundColor(
            if (!notif.isRead)
                android.graphics.Color.parseColor("#1A1A1A")
            else
                android.graphics.Color.parseColor("#111111")
        )

        holder.itemView.setOnClickListener { onItemClick(notif) }
    }

    fun update(newList: List<NotificationDto>) {
        currentList = newList
        notifyDataSetChanged()
    }

    fun markItemRead(id: Long) {
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = currentList.toMutableList()
            updated[index] = updated[index].copy(isRead = true)
            currentList = updated
            notifyItemChanged(index)
        }
    }

    fun markAllRead() {
        currentList = currentList.map { it.copy(isRead = true) }
        notifyDataSetChanged()
    }

    fun getUnreadIds(): List<Long> =
        currentList.filter { !it.isRead }.mapNotNull { it.id }

    fun getUnreadCount(): Int =
        currentList.count { !it.isRead }

    private fun formatTime(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrBlank()) return ""
        return try {
            val dateTime = java.time.LocalDateTime.parse(dateTimeStr)
            val now  = java.time.LocalDateTime.now()
            val diff = java.time.Duration.between(dateTime, now)
            when {
                diff.toMinutes() < 1  -> "Just now"
                diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
                diff.toHours()   < 24 -> "${diff.toHours()}h ago"
                diff.toDays()    < 7  -> "${diff.toDays()}d ago"
                else -> dateTime.format(
                    java.time.format.DateTimeFormatter.ofPattern("MMM d")
                )
            }
        } catch (e: Exception) { "" }
    }
}