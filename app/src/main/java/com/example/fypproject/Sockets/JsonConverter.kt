package com.example.fypproject.Sockets


import android.util.Log
import com.example.fypproject.ScoringDTO.ScoreDTO
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object JsonConverter {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val compactGson: Gson = Gson()

    fun toJson(scoreDTO: ScoreDTO, pretty: Boolean = false): String {
        return if (pretty) {
            gson.toJson(scoreDTO)
        } else {
            compactGson.toJson(scoreDTO)
        }
    }

    fun fromJson(jsonString: String): ScoreDTO? {
        return try {
            gson.fromJson(jsonString, ScoreDTO::class.java)
        } catch (e: Exception) {
            Log.e("JsonConverter", "Error parsing JSON: ${e.message}")
            null
        }
    }

    fun sendScore(scoreDTO: ScoreDTO) {
        val jsonString = toJson(scoreDTO, pretty = false)
        WebSocketManager.send(jsonString)
        Log.d("JsonConverter", "📤 Sent: $jsonString")
    }
}
