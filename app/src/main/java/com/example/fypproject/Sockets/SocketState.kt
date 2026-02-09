package com.example.fypproject.Sockets

sealed class SocketState {
    object Connected : SocketState()
    object Disconnected : SocketState()
    data class Error(val message: String) : SocketState()
}
