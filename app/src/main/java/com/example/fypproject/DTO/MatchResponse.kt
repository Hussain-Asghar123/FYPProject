package com.example.fypproject.DTO

import java.io.Serializable

data class MatchResponse(
    val id: Long? = null,
    val tournamentId: Long? = null,
    val tournamentName: String? = null,
    val team1Id: Long? = null,
    val team1Name: String? = null,
    val team2Id: Long? = null,
    val team2Name: String? = null,
    val scorerId: String? = null,
    val status: String? = null,
    val venue: String? = null,
    val date: String? = null,
    val time: String? = null,
    val tossWinnerId: Long? = null,
    val tossWinnerName: String? = null,
    val decision: String? = null,
    val sportId: Long? = null,
    val overs: Int? = null,
    val inningsId: Long? = null,

    // ── Cricket extras ─────────────────────────────────────────
    val battingTeamId: Long? = null,
    val battingTeamName: String? = null,

    // ── Volleyball extras ───────────────────────────────────────
    val sets: Int? = null,
    val pointsPerSet: Int? = null,      // JS: vbPointsPerSet  (default 25)
    val finalSetPoints: Int? = null,    // JS: vbFinalSetPoints (default 15)

    // ── Futsal extras ───────────────────────────────────────────
    val winnerTeamName: String? = null,
    val winnerTeamId: Long? = null,
) : Serializable