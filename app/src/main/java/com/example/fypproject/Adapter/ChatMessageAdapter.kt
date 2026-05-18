package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.CricketFragment.LiveChatFragment
import com.example.fypproject.R

class ChatMessageAdapter(
    private val messages: List<LiveChatFragment.ChatMessage>,
    private val myUsername: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SYSTEM = 0
        private const val TYPE_MY_MSG = 1
        private const val TYPE_OTHER  = 2
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.type == "system"               -> TYPE_SYSTEM
            msg.username == myUsername         -> TYPE_MY_MSG
            else                               -> TYPE_OTHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SYSTEM -> SystemViewHolder(
                inflater.inflate(R.layout.item_chat_system, parent, false)
            )
            TYPE_MY_MSG -> MyMessageViewHolder(
                inflater.inflate(R.layout.item_chat_my_message, parent, false)
            )
            else -> OtherMessageViewHolder(
                inflater.inflate(R.layout.item_chat_other_message, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is SystemViewHolder      -> holder.bind(msg)
            is MyMessageViewHolder   -> holder.bind(msg)
            is OtherMessageViewHolder -> holder.bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    // ── ViewHolders ──────────────────────────────────────────────────────────

    class SystemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSystem: TextView = view.findViewById(R.id.tvSystemMessage)
        fun bind(msg: LiveChatFragment.ChatMessage) {
            tvSystem.text = msg.message
        }
    }

    class MyMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMyMessage)
        fun bind(msg: LiveChatFragment.ChatMessage) {
            tvMessage.text = msg.message
        }
    }

    class OtherMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvUsername: TextView = view.findViewById(R.id.tvOtherUsername)
        private val tvMessage:  TextView = view.findViewById(R.id.tvOtherMessage)
        fun bind(msg: LiveChatFragment.ChatMessage) {
            tvUsername.text = msg.username
            tvMessage.text  = msg.message
        }
    }
}