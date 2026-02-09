package com.example.fypproject.Sockets



import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketListenerImpl(
    private val onStateChange: (SocketState) -> Unit,
    private val onMessageReceived: (String) -> Unit
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {

        onStateChange(SocketState.Connected)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        onMessageReceived(text)
    }

    override fun onFailure(
        webSocket: WebSocket,
        t: Throwable,
        response: Response?
    ) {
        onStateChange(
            SocketState.Error(t.message ?: "Unknown WebSocket error")
        )
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        onStateChange(SocketState.Disconnected)
    }
}
