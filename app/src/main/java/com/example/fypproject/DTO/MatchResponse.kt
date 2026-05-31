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
    val mediaScorerUsername: String? = null,
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
    val halfDurationMins: Int? = null,
    val matchFormat: String? = null,

    // ── Cricket extras ─────────────────────────────────────────
    val battingTeamId: Long? = null,
    val battingTeamName: String? = null,

    // ── NEW: Double Wicket & Commentator ───────────────────────
    val doubleWicket: Boolean? = null,          // true = 2 wickets = all out
    val commentatorUsername: String? = null,    // assigned commentator username

    // ── Volleyball extras ───────────────────────────────────────
    val sets: Int? = null,
    val pointsPerSet: Int? = null,
    val finalSetPoints: Int? = null,

    // ── Futsal extras ───────────────────────────────────────────
    val winnerTeamName: String? = null,
    val winnerTeamId: Long? = null,
    val team1PlayingIds: List<Long>? = null,
    val team2PlayingIds: List<Long>? = null,
) : Serializable