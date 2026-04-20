package com.example.fypproject.Sockets

import okhttp3.*

object WebSocketManager {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var currentMatchId: Long? = null
    private val pendingMessages = mutableListOf<String>() // ← BUG 1 FIX: queue add karo

    var socketStateListener: ((SocketState) -> Unit)? = null
    var messageListener: ((String) -> Unit)? = null

    fun connect(matchId: Long) {
        // ← BUG 2 FIX: pehle same match check karo, PHIR disconnect
        if (webSocket != null && currentMatchId == matchId) {
            socketStateListener?.invoke(SocketState.Connected)
            return
        }

        disconnect()
        currentMatchId = matchId

        val url = "wss://mhaseeb-t-a.hf.space/ws?matchId=$matchId"

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                android.util.Log.d("WS", "Connected to match $matchId")
                // ← BUG 1 FIX: pending messages flush karo
                synchronized(pendingMessages) {
                    pendingMessages.forEach { ws.send(it) }
                    pendingMessages.clear()
                }
                socketStateListener?.invoke(SocketState.Connected)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                messageListener?.invoke(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("WS", "Error: ${t.message}")
                webSocket = null
                socketStateListener?.invoke(SocketState.Error(t.message ?: "Unknown error"))
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                webSocket = null
                socketStateListener?.invoke(SocketState.Disconnected)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal close")
        webSocket = null
        currentMatchId = null
        pendingMessages.clear() // ← queue bhi clear karo
        socketStateListener?.invoke(SocketState.Disconnected)
    }

    fun send(message: String) {
        val ws = webSocket
        if (ws == null) {
            // ← BUG 1 FIX: drop mat karo — queue karo
            android.util.Log.w("WS", "Not connected yet — queuing: $message")
            synchronized(pendingMessages) { pendingMessages.add(message) }
            return
        }
        android.util.Log.d("TT_SEND", "Sending: $message")
        ws.send(message)
    }

    fun isConnected(): Boolean = webSocket != null
}