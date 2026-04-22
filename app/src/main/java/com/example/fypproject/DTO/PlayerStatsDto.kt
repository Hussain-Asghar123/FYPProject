package com.example.fypproject.DTO

data class PlayerStatsDto(
    val playerId: Long = -1L,
    val playerName: String? = null,
    val sport: String? = null,

    // ── Shared ───────────────────────────────────────────────────────────────
    val matchesPlayed: Int = 0,
    val pomCount: Int = 0,

    // ── Cricket ──────────────────────────────────────────────────────────────
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

    // ── Futsal ───────────────────────────────────────────────────────────────
    // goals        → Futsal: goals      | Badminton: points scored | TT: points scored | TOW: rounds won  | Ludo: home runs | Chess: wins
    // assists      → Futsal: assists    | Badminton: smashes+aces  | TT: smashes+aces  | TOW: matches won | Ludo: captures  | Chess: checks delivered
    // futsalFouls  → Futsal: fouls      | Badminton: faults        | TT: net/svc faults| TOW: matches lost
    // yellowCards  → Futsal: yellow     | Badminton: out shots      | TT: out shots
    // redCards     → Futsal: red cards
    val futsalMatchesPlayed: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val futsalFouls: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,

    // ── Volleyball ───────────────────────────────────────────────────────────
    val volleyballMatchesPlayed: Int = 0,
    val pointsScored: Int = 0,
    val aces: Int = 0,
    val blocks: Int = 0,
    val attackErrors: Int = 0,
    val serviceErrors: Int = 0,

    // ── Badminton ────────────────────────────────────────────────────────────
    // (uses shared futsal fields: goals, assists, futsalFouls, yellowCards)
    val badmintonMatchesPlayed: Int = 0,
    val badmintonPoints: Int = 0,
    val smashes: Int = 0,
    val faults: Int = 0,
    val outShots: Int = 0,

    // ── Table Tennis ─────────────────────────────────────────────────────────
    // (uses shared futsal fields: goals, assists, futsalFouls, yellowCards)
    val tableTennisMatchesPlayed: Int = 0,

    // ── Tug of War ───────────────────────────────────────────────────────────
    // (uses shared futsal fields: goals=roundsWon, assists=matchesWon, futsalFouls=matchesLost)
    val tugOfWarMatchesPlayed: Int = 0,

    // ── Ludo ─────────────────────────────────────────────────────────────────
    // (uses shared futsal fields: goals=homeRuns, assists=captures)
    val ludoMatchesPlayed: Int = 0,

    // ── Chess ─────────────────────────────────────────────────────────────────
    // (uses shared futsal fields: goals=wins, assists=checksDelivered)
    val chessMatchesPlayed: Int = 0,
)