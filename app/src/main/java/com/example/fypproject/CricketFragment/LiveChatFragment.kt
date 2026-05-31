package com.example.fypproject.CricketFragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.ChatMessageAdapter
import com.example.fypproject.R
import com.example.fypproject.databinding.FragmentLiveChatBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class LiveChatFragment : Fragment() {

    private var _binding: FragmentLiveChatBinding? = null
    private val binding get() = _binding!!

    private var matchId: Long = -1L
    private var username: String = "Guest"
    private var isCommentator: Boolean = false   // ← NEW
    private var isAdmin: Boolean = false        // ADD THIS
    private var isGuest: Boolean = false

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatMessageAdapter

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private var isConnected = false
    private var isOpen = false
    private var unreadCount = 0

    // ─── Data class ──────────────────────────────────────────────────────────
    data class ChatMessage(
        val type: String = "chat",
        val username: String = "",
        val message: String = ""
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchId      = arguments?.getLong("matchId", -1L) ?: -1L
        username     = arguments?.getString("username", "Guest") ?: "Guest"
        isCommentator = arguments?.getBoolean("isCommentator", false) ?: false
        isAdmin      = arguments?.getBoolean("isAdmin", false) ?: false          // ADD THIS
        isGuest      = arguments?.getBoolean("isGuest", false) ?: false          // ADD THIS// ← NEW

        setupRecyclerView()
        setupToggleHeader()
        setupInput()            // commentator ya user — alag alag UI
        connectWebSocket()

        // Commentator ke liye auto-expand karo
        if (isCommentator) {
            isOpen = true
            updateChatVisibility()
        }
    }

    // ── RecyclerView setup ───────────────────────────────────────────────────
    private fun setupRecyclerView() {
        chatAdapter = ChatMessageAdapter(messages, username)
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).also {
                it.stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    // ── Collapsible header toggle ────────────────────────────────────────────
    private fun setupToggleHeader() {
        binding.layoutChatHeader.setOnClickListener {
            isOpen = !isOpen
            updateChatVisibility()
            if (isOpen) {
                unreadCount = 0
                updateBadge()
                scrollToBottom()
            }
        }
    }

    private fun updateChatVisibility() {
        binding.layoutChatBody.visibility = if (isOpen) View.VISIBLE else View.GONE
        binding.ivChevron.rotation = if (isOpen) 180f else 0f
    }

    // ── Input setup — commentator vs regular user ────────────────────────────
    private fun setupInput() {
        when {
            isCommentator || isAdmin -> {
                // ── Commentator/Admin: text input dikhao ──────────────────────────────
                binding.etChatInput.visibility  = View.VISIBLE
                binding.btnSendChat.visibility  = View.VISIBLE
                binding.btnSendChat.setOnClickListener { sendTextMessage() }
                binding.etChatInput.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        sendTextMessage(); true
                    } else false
                }
            }
            isGuest -> {
                // ── Guest: bilkul read-only (kuch bhi nahi dikhao) ──────────────────
                binding.etChatInput.visibility = View.GONE
                binding.btnSendChat.visibility = View.GONE
            }
            else -> {
                // ── Regular user: text input hide karo, 5 emoji buttons dikhao ─
                binding.etChatInput.visibility = View.GONE
                binding.btnSendChat.visibility = View.GONE
                addEmojiPanel()
            }
        }
    }

    // ── Emoji panel for regular users ────────────────────────────────────────
    private fun addEmojiPanel() {
        val emojis = listOf("🔥", "❤️", "👏", "😮", "🏏")

        val px8  = dpToPx(8f)
        val px12 = dpToPx(12f)
        val px56 = dpToPx(56f)

        // Outer container — white background, padded
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(px12, px8, px12, px8)
        }

        // Label
        val label = TextView(requireContext()).apply {
            text = "React to live match:"
            textSize = 11f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = px8 }
        }
        container.addView(label)

        // Emoji row
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        emojis.forEach { emoji ->
            val tv = TextView(requireContext()).apply {
                text = emoji
                textSize = 30f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, px56, 1f)
                isClickable = true
                isFocusable = true

                // Press effect
                setOnClickListener {
                    sendEmoji(emoji)
                    // Choti animation — scale down phir wapis
                    animate().scaleX(0.75f).scaleY(0.75f).setDuration(80).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    }.start()
                }
            }
            row.addView(tv)
        }

        container.addView(row)

        // Input row ke parent mein add karo
        // (etChatInput ka parent = inner LinearLayout "input row")
        val inputRowParent = binding.etChatInput.parent as? ViewGroup
        inputRowParent?.addView(container)
    }

    private fun sendEmoji(emoji: String) {
        val ws = webSocket ?: return
        if (!isConnected) return
        try {
            val json = JSONObject().apply {
                put("message", emoji)
                put("username", username)
            }
            ws.send(json.toString())
        } catch (e: Exception) {
            Log.e("LiveChat", "Emoji send error: ${e.message}")
        }
    }

    private fun sendTextMessage() {
        val text = binding.etChatInput.text?.toString()?.trim() ?: return
        if (text.isEmpty() || !isConnected) return
        val ws = webSocket ?: return
        try {
            val json = JSONObject().apply {
                put("message", text)
                put("username", username)
            }
            ws.send(json.toString())
            binding.etChatInput.setText("")
        } catch (e: Exception) {
            Log.e("LiveChat", "Send error: ${e.message}")
        }
    }

    // ── WebSocket ────────────────────────────────────────────────────────────
    private fun connectWebSocket() {
        if (matchId == -1L) return

        val baseUrl = "ws://10.107.69.89:7860/ws/chat"
        val url = "$baseUrl?matchId=$matchId&username=${Uri.encode(username)}"

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                activity?.runOnUiThread { updateConnectionDot() }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val jo  = JSONObject(text)
                    val msg = ChatMessage(
                        type     = jo.optString("type", "chat"),
                        username = jo.optString("username", ""),
                        message  = jo.optString("message", "")
                    )
                    activity?.runOnUiThread {
                        if (messages.size >= 100) messages.removeAt(0)
                        messages.add(msg)
                        chatAdapter.notifyItemInserted(messages.size - 1)

                        if (isOpen) scrollToBottom()
                        else { unreadCount++; updateBadge() }
                    }
                } catch (e: Exception) {
                    Log.e("LiveChat", "Parse error: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                webSocket   = null
                activity?.runOnUiThread { updateConnectionDot() }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                webSocket   = null
                activity?.runOnUiThread { updateConnectionDot() }
            }
        })
    }

    private fun updateConnectionDot() {
        if (_binding == null) return
        binding.viewConnectionDot.setBackgroundResource(
            if (isConnected) R.drawable.dot_green else R.drawable.dot_grey
        )
        // Only enable send button for commentator
        if (isCommentator||isAdmin) {
            binding.btnSendChat.isEnabled  = isConnected
            binding.etChatInput.isEnabled  = isConnected
        }
    }

    private fun updateBadge() {
        if (_binding == null) return
        if (unreadCount > 0) {
            binding.tvUnreadBadge.visibility = View.VISIBLE
            binding.tvUnreadBadge.text = unreadCount.toString()
        } else {
            binding.tvUnreadBadge.visibility = View.GONE
        }
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            binding.rvChatMessages.smoothScrollToPosition(messages.size - 1)
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private fun dpToPx(dp: Float): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        webSocket?.close(1000, "Fragment destroyed")
        webSocket = null
        _binding  = null
    }

    companion object {
        fun newInstance(
            matchId: Long,
            username: String,
            isCommentator: Boolean = false
        ): LiveChatFragment {
            return LiveChatFragment().apply {
                arguments = Bundle().apply {
                    putLong("matchId", matchId)
                    putString("username", username)
                    putBoolean("isCommentator", isCommentator)

                }
            }
        }
    }
}