package com.example.fypproject.ScoringDTO

data class VollayBallScoreDTO(
    val team1Score: Int?    = null,
    val team2Score: Int?    = null,
    val team1Sets: Int?     = null,
    val team2Sets: Int?     = null,
    val currentSet: Int?    = null,
    val team1Timeouts: Int? = null,   // ✅ ADD
    val team2Timeouts: Int? = null,   // ✅ ADD
    val setsToWin: Int?     = null,   // ✅ ADD
    val status: String?     = null,
    val comment: String?    = null,
    val volleyballEvents: List<VolleyballEvent>? = null,  // ✅ ADD
    val setStartTime: Long? = null    // ✅ ADD
)

data class VolleyballEvent(
    val id: Long?     = null,
    val eventType: String? = null,
    val teamName: String?  = null,
    val playerName: String? = null,
    val eventTimeSeconds: Int? = null,
    val setNumber: Int? = null
)
