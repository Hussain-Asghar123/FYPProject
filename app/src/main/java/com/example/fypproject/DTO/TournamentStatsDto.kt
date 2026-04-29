package com.example.fypproject.DTO

import com.google.gson.annotations.SerializedName

data class TournamentStatsDto(
    val tournamentId: Long,
    val tournamentName: String?,
    val sport: String? = null,
    val sportId: Long? = null,
    val manOfTournament: PlayerAwardDto? = null,
    val bestBatsman: PlayerAwardDto? = null,
    val bestBowler: PlayerAwardDto? = null,
    val bestFielder: PlayerAwardDto? = null,
    val bestGoalScorer: PlayerAwardDto? = null,
    val topScorer: PlayerAwardDto? = null,
    val topAssist: PlayerAwardDto? = null,
    val mostSixes: SixesStatDto? = null,
    val allAwards: List<PlayerAwardDto>? = null,
    val topRunScorers: List<TopBatsmanDto>? = null,
    val topBowlers: List<TopBowlerDto>? = null,
    val topGoalScorers: List<TopFutsalScorerDto>? = null,

    // JS uses "topAssisters" — support both spellings
    @SerializedName(value = "topAssisters", alternate = ["topAssistants"])
    val topAssistants: List<TopFutsalAssistantDto>? = null
)

data class PlayerAwardDto(
    val playerId: Long,
    val playerName: String,
    val awardType: String? = null,
    val points: Int? = null,
    val reason: String? = null
)

data class SixesStatDto(
    val playerId: Long,
    val playerName: String,
    val sixes: Int
)

data class TopBatsmanDto(
    val playerId: Long,
    val playerName: String,
    val runs: Int = 0,
    val ballsFaced: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val strikeRate: Double = 0.0,
    val playerOfMatchCount: Int = 0
)

data class TopBowlerDto(
    val playerId: Long,
    val playerName: String,
    val wickets: Int = 0,
    // API may send "runsConceded" OR "runs" — handle both, no duplicate field
    @SerializedName(value = "runsConceded", alternate = ["runs"])
    val runsConceded: Int = 0,
    val ballsBowled: Int = 0,
    val economy: Double = 0.0,
    val playerOfMatchCount: Int = 0
)

data class TopFutsalScorerDto(
    val playerId: Long,
    val playerName: String,
    val goals: Int = 0,
    val assists: Int = 0,
    val futsalFouls: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val totalPoints: Int = 0,
    val matches: Int = 0,
    val playerOfMatchCount: Int = 0
)

data class TopFutsalAssistantDto(
    val playerId: Long,
    val playerName: String,
    val assists: Int = 0,
    val goals: Int = 0,
    val totalPoints: Int = 0,
    val matches: Int = 0,
    val playerOfMatchCount: Int = 0
)

data class TopVotedPlayerDto(
    val playerId: Long,
    val playerName: String,
    val votes: Int
)