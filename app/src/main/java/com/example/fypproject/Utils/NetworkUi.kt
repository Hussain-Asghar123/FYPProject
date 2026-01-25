package com.example.fypproject.Utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException
import retrofit2.Response

object NetworkUi {
    fun userMessage(code: Int?, serverMessage: String? = null): String {
        val server = serverMessage?.trim().orEmpty()
        val hasServerMsg = server.isNotBlank()

        return when (code) {
            400 -> if (hasServerMsg) server else "Invalid request. Please check your input."
            401 -> if (hasServerMsg) server else "Session expired. Please login again."
            403 -> if (hasServerMsg) server else "You are not authorized to perform this action."
            404 -> if (hasServerMsg) server else "Service not found. Please try again later."
            409 -> if (hasServerMsg) server else "Already exists. Please try different data."
            422 -> if (hasServerMsg) server else "Invalid details. Please try again."
            in 500..599 -> if (hasServerMsg) server else "Server error. Please try again later."
            else -> if (hasServerMsg) server else "Something went wrong. Please try again."
        }
    }

    fun userMessage(t: Throwable): String {
        return when (t) {
            is UnknownHostException -> "No internet connection."
            is SocketTimeoutException -> "Request timed out. Please try again."
            is IOException -> "Network error. Please check your connection."
            is HttpException -> {
                val code = t.code()
                userMessage(code)
            }
            else -> "Something went wrong. Please try again."
        }
    }

    fun userMessage(response: Response<*>, fallbackServerMessage: String? = null): String {
        val serverMessage = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        val msg = serverMessage?.takeIf { it.isNotBlank() } ?: fallbackServerMessage
        return userMessage(response.code(), msg)
    }
}

fun Context.toastShort(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.toastLong(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Fragment.toastShort(message: String) {
    context?.toastShort(message)
}

fun Fragment.toastLong(message: String) {
    context?.toastLong(message)
}
