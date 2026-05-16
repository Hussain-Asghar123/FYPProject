package com.example.fypproject.DTO

data class TeamStatsDto(
    val teamId: Long,
    val teamName: String?,
    val sport: String?,
    val sportId: Long? = null,

    // ── Match Record ─────────────────────────────────────────────────────
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,

    // ── Cricket ──────────────────────────────────────────────────────────
    val totalRunsScored: Int = 0,
    val totalWicketsTaken: Int = 0,
    val totalFours: Int = 0,
    val totalSixes: Int = 0,
    val highestTeamScore: Int = 0,
    val totalCatches: Int = 0,

    // ── Futsal ───────────────────────────────────────────────────────────
    val totalGoals: Int = 0,
    val totalAssists: Int = 0,
    val totalFouls: Int = 0,
    val totalYellowCards: Int = 0,
    val totalRedCards: Int = 0,

    // ── Volleyball ───────────────────────────────────────────────────────
    val totalPoints: Int = 0,
    val totalAces: Int = 0,
    val totalBlocks: Int = 0,
    val totalAttackErrors: Int = 0,
    val totalServiceErrors: Int = 0,

    // ── Badminton / Table Tennis ─────────────────────────────────────────
    // totalPoints reused for scored points
    val totalSmashes: Int = 0,
    val totalFaults: Int = 0,
    val totalOutShots: Int = 0,

    // ── Tug of War ───────────────────────────────────────────────────────
    // wins/losses reused
    val totalRoundsWon: Int = 0,

    // ── Ludo ─────────────────────────────────────────────────────────────
    val totalHomeRuns: Int = 0,
    val totalCaptures: Int = 0,

    // ── Chess ─────────────────────────────────────────────────────────────
    val totalChecks: Int = 0,
    // wins reused for chess wins

    // ── Top Performer (all sports) ───────────────────────────────────────
    val topScorerName: String? = null,
    val topScorerStat: String? = null,
)
