package com.example.fypproject.ScoringDTO

data class FutsalScoreDTO(
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val team1Fouls: Int = 0,
    val team2Fouls: Int = 0,
    val team1YellowCards: Int = 0,
    val team2YellowCards: Int = 0,
    val team1RedCards: Int = 0,
    val team2RedCards: Int = 0,
    val currentHalf: Int = 1,
    val status: String = "LIVE",
    val halfStartTime: Long? = null,
    val halfDurationMinutes: Int = 25,
    val futsalEvents: List<FutsalEventDTO> = emptyList()
)
data class FutsalEventDTO(
    val id: Long?,
    val eventType: String,
    val eventTimeSeconds: Int,
    val half: Int,
    val scorerName: String?,
    val assistPlayerName: String?,
    val teamName: String? = null,
    val outPlayerName: String? = null
)


