package com.example.fypproject.CricketFragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatMessageAdapter

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private var isConnected = false
    private var isOpen = false
    private var unreadCount = 0

    // ─── Data class ──────────────────────────────────────────────────────────
    data class ChatMessage(
        val type: String = "chat",   // "chat" or "system"
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

        matchId  = arguments?.getLong("matchId", -1L) ?: -1L
        username = arguments?.getString("username", "Guest") ?: "Guest"

        setupRecyclerView()
        setupToggleHeader()
        setupInput()
        connectWebSocket()
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

    // ── Input & send ─────────────────────────────────────────────────────────
    private fun setupInput() {
        binding.btnSendChat.setOnClickListener { sendMessage() }
        binding.etChatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(); true
            } else false
        }
    }

    private fun sendMessage() {
        val text = binding.etChatInput.text?.toString()?.trim() ?: return
        if (text.isEmpty() || !isConnected) return
        val ws = webSocket ?: return

        try {
            val json = JSONObject().apply { put("message", text) }
            ws.send(json.toString())
            binding.etChatInput.setText("")
        } catch (e: Exception) {
            Log.e("LiveChat", "Send error: ${e.message}")
        }
    }

    // ── WebSocket ────────────────────────────────────────────────────────────
    private fun connectWebSocket() {
        if (matchId == -1L) return

        // Same base URL as scoring WS but /ws/chat endpoint
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
                        // Keep max 100 messages
                        if (messages.size >= 100) messages.removeAt(0)
                        messages.add(msg)
                        chatAdapter.notifyItemInserted(messages.size - 1)

                        if (isOpen) {
                            scrollToBottom()
                        } else {
                            unreadCount++
                            updateBadge()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LiveChat", "Parse error: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                webSocket = null
                activity?.runOnUiThread { updateConnectionDot() }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                webSocket = null
                activity?.runOnUiThread { updateConnectionDot() }
            }
        })
    }

    private fun updateConnectionDot() {
        if (_binding == null) return
        binding.viewConnectionDot.setBackgroundResource(
            if (isConnected) R.drawable.dot_green else R.drawable.dot_grey
        )
        binding.btnSendChat.isEnabled = isConnected
        binding.etChatInput.isEnabled = isConnected
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

    override fun onDestroyView() {
        super.onDestroyView()
        webSocket?.close(1000, "Fragment destroyed")
        webSocket = null
        _binding = null
    }

    companion object {
        fun newInstance(matchId: Long, username: String): LiveChatFragment {
            return LiveChatFragment().apply {
                arguments = Bundle().apply {
                    putLong("matchId", matchId)
                    putString("username", username)
                }
            }
        }
    }
}