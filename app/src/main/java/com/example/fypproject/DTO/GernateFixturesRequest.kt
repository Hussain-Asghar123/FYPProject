package com.example.fypproject.DTO

data class GenerateFixturesRequest(
    val tournamentType: String,   // ROUND_ROBIN | LEAGUE | KNOCK_OUT | MIXED
    val startDate: String,         // yyyy-MM-dd
    val startTime: String,         // HH:mm
    val gapMinutes: Int,           // Gap between matches in minutes
    val venue: String,
    val overs: Int,
    val scorerId: String?,
    val mediaScorerUsername: String?
)