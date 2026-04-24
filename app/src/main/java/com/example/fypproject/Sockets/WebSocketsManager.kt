package com.example.fypproject.Sockets

import okhttp3.*

object WebSocketManager {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var currentMatchId: Long? = null
    private val pendingMessages = mutableListOf<String>()

    private val stateListeners = mutableMapOf<String, (SocketState) -> Unit>()
    private val messageListeners = mutableMapOf<String, (String) -> Unit>()

    // Backward compatibility (purana code jo directly assign karta tha)
    var socketStateListener: ((SocketState) -> Unit)?
        get() = stateListeners["__default__"]
        set(value) {
            if (value == null) stateListeners.remove("__default__")
            else stateListeners["__default__"] = value
        }

    var messageListener: ((String) -> Unit)?
        get() = messageListeners["__default__"]
        set(value) {
            if (value == null) messageListeners.remove("__default__")
            else messageListeners["__default__"] = value
        }

    fun addStateListener(key: String, listener: (SocketState) -> Unit) {
        stateListeners[key] = listener
    }

    fun removeStateListener(key: String) {
        stateListeners.remove(key)
    }

    fun addMessageListener(key: String, listener: (String) -> Unit) {
        messageListeners[key] = listener
    }

    fun removeMessageListener(key: String) {
        messageListeners.remove(key)
    }

    fun connect(matchId: Long) {
        if (webSocket != null && currentMatchId == matchId) {
            stateListeners.values.forEach { it.invoke(SocketState.Connected) }
            return
        }
        disconnect()
        currentMatchId = matchId

        val url = "wss://mhaseeb-t-a.hf.space/ws?matchId=$matchId"
        //  val url = "wss://mhaseeb-t-fyp.hf.space/ws?matchId=$matchId"
       // val url = "ws://192.168.1.105:7860/ws?matchId=$matchId"

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                android.util.Log.d("WS", "Connected to match $matchId")
                synchronized(pendingMessages) {
                    pendingMessages.forEach { ws.send(it) }
                    pendingMessages.clear()
                }
                stateListeners.values.forEach { it.invoke(SocketState.Connected) }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                messageListeners.values.forEach { it.invoke(text) }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("WS", "Error: ${t.message}")
                webSocket = null
                stateListeners.values.forEach {
                    it.invoke(SocketState.Error(t.message ?: "Unknown error"))
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                webSocket = null
                stateListeners.values.forEach { it.invoke(SocketState.Disconnected) }
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal close")
        webSocket = null
        currentMatchId = null
        synchronized(pendingMessages) { pendingMessages.clear() }
        stateListeners.values.forEach { it.invoke(SocketState.Disconnected) }
    }

    fun send(message: String) {
        val ws = webSocket
        if (ws == null) {
            android.util.Log.w("WS", "Not connected yet — queuing: $message")
            synchronized(pendingMessages) { pendingMessages.add(message) }
            return
        }
        android.util.Log.d("TT_SEND", "Sending: $message")
        ws.send(message)
    }

    fun isConnected(): Boolean = webSocket != null
}