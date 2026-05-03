package com.example.fypproject.ScoringDTO


data class CricketBall(
    val id: Long? = null,
    val event: String,
    val eventType: String,
    val mediaCount: Int = 0
) {
    val hasMedia: Boolean get() = mediaCount > 0
}