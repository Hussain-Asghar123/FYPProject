package com.example.fypproject.ScoringDTO

data class HockeyScoringDto(
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val team1Fouls: Int = 0,
    val team2Fouls: Int = 0,
    val team1GreenCards: Int = 0,
    val team2GreenCards: Int = 0,
    val team1YellowCards: Int = 0,
    val team2YellowCards: Int = 0,
    val team1RedCards: Int = 0,
    val team2RedCards: Int = 0,
    val team1PenaltyCorners: Int = 0,
    val team2PenaltyCorners: Int = 0,
    val currentPeriod: Int = 1,
    val status: String = "LIVE",
    val inExtraTime: Boolean = false,
    val periodStartTime: Long? = null,
    val periodDurationMinutes: Int = 15,
    val hockeyEvents: List<HockeyEventDTO> = emptyList(),
    val comment: String = ""
)

data class HockeyEventDTO(
    val id: Long?,
    val eventType: String,
    val eventTimeSeconds: Int,
    val period: Int,
    val scorerName: String?,
    val assistPlayerName: String?,
    val teamName: String? = null,
    val outPlayerName: String? = null,
    val goalType: String? = null
)
