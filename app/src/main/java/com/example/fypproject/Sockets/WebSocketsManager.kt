package com.example.fypproject.Sockets

import okhttp3.*

object WebSocketManager {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var currentMatchId: Int? = null

    var socketStateListener: ((SocketState) -> Unit)? = null
    var messageListener: ((String) -> Unit)? = null

    private val socketListener = WebSocketListenerImpl(
        onStateChange = { socketStateListener?.invoke(it) },
        onMessageReceived = { messageListener?.invoke(it) }
    )

    fun connect(matchId: Int) {
        disconnect()
        // If already connected to same match, don't reconnect
        if (webSocket != null && currentMatchId == matchId) {
            socketStateListener?.invoke(SocketState.Connected)
            return
        }

        // Disconnect existing connection if any

        currentMatchId = matchId

        val url = "wss://mhaseeb-t-a.hf.space/ws?matchId=$matchId"
       // val url = "ws://192.168.155.89:7860/ws?matchId=$matchId"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, socketListener)
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal close")
        webSocket = null
        currentMatchId = null
        socketStateListener?.invoke(SocketState.Disconnected)
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }
}