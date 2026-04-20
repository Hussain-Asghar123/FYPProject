package com.example.fypproject.DTO

data class PlayerStatsDto(
    val playerId: Long = -1L,
    val playerName: String? = null,
    val sport: String? = null,

    // ── Shared ───────────────────────────────────
    val matchesPlayed: Int = 0,
    val pomCount: Int = 0,

    // ── Cricket ──────────────────────────────────
    val cricketMatchesPlayed: Int = 0,
    val totalRuns: Int = 0,
    val highest: Int = 0,
    val ballsFaced: Int = 0,
    val ballsBowled: Int = 0,
    val runsConceded: Int = 0,
    val strikeRate: Double = 0.0,
    val economy: Double = 0.0,
    val battingAvg: Double = 0.0,
    val bowlingAverage: Double = 0.0,
    val notOuts: Int = 0,
    val wickets: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,

    // ── Futsal ───────────────────────────────────
    val futsalMatchesPlayed: Int = 0,
    val goals: Int = 0,        // Futsal=goals | Badminton=points | TT=points | TOW=rounds won
    val assists: Int = 0,      // Futsal=assists | Badminton=smashes+aces | TT=smashes+aces | TOW=matches won
    val futsalFouls: Int = 0,  // Futsal=fouls | Badminton=faults | TT=net/svc faults | TOW=matches lost
    val yellowCards: Int = 0,  // Futsal=yellow | Badminton=out shots | TT=out shots
    val redCards: Int = 0,

    // ── Volleyball ───────────────────────────────
    val volleyballMatchesPlayed: Int = 0,
    val pointsScored: Int = 0,
    val aces: Int = 0,
    val blocks: Int = 0,
    val attackErrors: Int = 0,
    val serviceErrors: Int = 0,

    // ── Badminton ────────────────────────────────
    val badmintonMatchesPlayed: Int = 0,
    val badmintonPoints: Int = 0,
    val smashes: Int = 0,
    val faults: Int = 0,
    val outShots: Int = 0,

    // ── Table Tennis ─────────────────────────────
    val tableTennisMatchesPlayed: Int = 0,

    // ── Tug of War ───────────────────────────────
    val tugOfWarMatchesPlayed: Int = 0,
)