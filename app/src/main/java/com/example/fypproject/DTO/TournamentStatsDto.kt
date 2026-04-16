package com.example.fypproject.DTO

data class TournamentStatsDto(
    val tournamentId: Long,
    val tournamentName: String?,
    val sport: String? = null,
    val sportId: Long? = null,
    val manOfTournament: PlayerAwardDto?,
    val bestBatsman: PlayerAwardDto?,
    val bestBowler: PlayerAwardDto?,
    val bestFielder: PlayerAwardDto?,
    val bestGoalScorer: PlayerAwardDto? = null,
    val mostSixes: SixesStatDto?,
    val topRunScorers: List<TopBatsmanDto>? = null,          // ✅ nullable
    val topBowlers: List<TopBowlerDto>? = null,              // ✅ nullable
    val topGoalScorers: List<TopFutsalScorerDto>? = null,    // ✅ nullable
    val topAssistants: List<TopFutsalAssistantDto>? = null   // ✅ nullable
)

data class PlayerAwardDto(
    val playerId: Long,
    val playerName: String,
    val awardType: String?,
    val points: Int?,
    val reason: String?
)

data class SixesStatDto(
    val playerId: Long,
    val playerName: String,
    val sixes: Int
)

data class TopBatsmanDto(
    val playerId: Long,
    val playerName: String,
    val runs: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val strikeRate: Int,
    val playerOfMatchCount: Int
)

data class TopBowlerDto(
    val playerId: Long,
    val playerName: String,
    val wickets: Int,
    val runs: Int,
    val ballsBowled: Int,
    val economy: Double,
    val runsConceded: Int,
    val playerOfMatchCount: Int
)

data class TopFutsalScorerDto(
    val playerId: Long,
    val playerName: String,
    val goals: Int,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val matches: Int,
    val playerOfMatchCount: Int
)
data class TopFutsalAssistantDto(
    val playerId: Long,
    val playerName: String,
    val assists: Int,
    val goals: Int = 0,
    val matches: Int,
    val playerOfMatchCount: Int
)
