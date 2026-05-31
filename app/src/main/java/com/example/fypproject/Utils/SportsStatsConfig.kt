package com.example.fypproject.Utils

import com.example.fypproject.DTO.PlayeraStatsDTO1
import com.example.fypproject.DTO.StatRowItem

// JS ke SPORT_STATS ka Android equivalent
data class StatConfig(
    val key: String,
    val label: String,
    val decimal: Int = 0,
    val lowerIsBetter: Boolean = false,
    val isString: Boolean = false
)

object SportStatsConfig {

    val SPORTS = listOf(
        Pair("cricket",       " Cricket"),
        Pair("futsal",        " Futsal"),
        Pair("volleyball",    " Volleyball"),
        Pair("badminton",     " Badminton"),
        Pair("table tennis",  " Table Tennis"),
        Pair("ludo",          " Ludo"),
        Pair("chess",         " Chess"),
        Pair("hockey",        " Hockey"),
    )

    private val CRICKET = listOf(
        StatConfig("matchesPlayed",  "Matches"),
        StatConfig("runsScored",     "Runs"),
        StatConfig("average",        "Batting Average",   decimal = 2),
        StatConfig("strikeRate",     "Strike Rate",       decimal = 2),
        StatConfig("highestScore",   "Highest Score"),
        StatConfig("hundreds",       "100s"),
        StatConfig("fifties",        "50s"),
        StatConfig("fours",          "Fours (4s)"),
        StatConfig("sixes",          "Sixes (6s)"),
        StatConfig("wicketsTaken",   "Wickets"),
        StatConfig("economy",        "Economy",           decimal = 2, lowerIsBetter = true),
        StatConfig("bowlingAverage", "Bowling Average",   decimal = 2, lowerIsBetter = true),
        StatConfig("bestBowling",    "Best Bowling",      isString = true),
        StatConfig("catches",        "Catches"),
    )

    private val FUTSAL = listOf(
        StatConfig("matchesPlayed", "Matches"),
        StatConfig("goals",         "Goals"),
        StatConfig("assists",       "Assists"),
        StatConfig("futsalFouls",   "Fouls",        lowerIsBetter = true),
        StatConfig("yellowCards",   "Yellow Cards", lowerIsBetter = true),
        StatConfig("redCards",      "Red Cards",    lowerIsBetter = true),
    )

    private val VOLLEYBALL = listOf(
        StatConfig("matchesPlayed", "Matches"),
        StatConfig("goals",         "Points"),
        StatConfig("assists",       "Aces"),
        StatConfig("futsalFouls",   "Blocks"),
        StatConfig("yellowCards",   "Attack Errors",  lowerIsBetter = true),
        StatConfig("redCards",      "Service Errors", lowerIsBetter = true),
    )

    private val BADMINTON = listOf(
        StatConfig("matchesPlayed", "Matches"),
        StatConfig("goals",         "Points"),
        StatConfig("assists",       "Smashes"),
        StatConfig("futsalFouls",   "Faults", lowerIsBetter = true),
    )

    private val TABLE_TENNIS = listOf(
        StatConfig("matchesPlayed", "Matches"),
        StatConfig("goals",         "Points"),
        StatConfig("assists",       "Games Won"),
        StatConfig("futsalFouls",   "Errors", lowerIsBetter = true),
    )

    private val LUDO = listOf(
        StatConfig("matchesPlayed", "Matches"),
        StatConfig("goals",         "Games Won"),
    )

    private val CHESS = listOf(
        StatConfig("matchesPlayed", "Matches"),
        StatConfig("goals",         "Wins"),
    )

    private val HOCKEY = listOf(
        StatConfig("matchesPlayed", "Matches"),
        StatConfig("goals",         "Goals"),
        StatConfig("assists",       "Assists"),
        StatConfig("futsalFouls",   "Fouls",        lowerIsBetter = true),
        StatConfig("yellowCards",   "Yellow Cards", lowerIsBetter = true),
        StatConfig("redCards",      "Red Cards",    lowerIsBetter = true),
    )

    fun getConfig(sport: String): List<StatConfig> = when (sport) {
        "cricket"      -> CRICKET
        "futsal"       -> FUTSAL
        "volleyball"   -> VOLLEYBALL
        "badminton"    -> BADMINTON
        "table tennis" -> TABLE_TENNIS
        "ludo"         -> LUDO
        "chess"        -> CHESS
        "hockey"       -> HOCKEY
        else           -> CRICKET
    }

    // Stats DTO se value key ke hisaab se nikalo
    fun getValue(stats: PlayeraStatsDTO1?, key: String): Any? = when (key) {
        "matchesPlayed"  -> stats?.matchesPlayed
        "runsScored"     -> stats?.runsScored
        "average"        -> stats?.average
        "strikeRate"     -> stats?.strikeRate
        "highestScore"   -> stats?.highestScore
        "hundreds"       -> stats?.hundreds
        "fifties"        -> stats?.fifties
        "fours"          -> stats?.fours
        "sixes"          -> stats?.sixes
        "wicketsTaken"   -> stats?.wicketsTaken
        "economy"        -> stats?.economy
        "bowlingAverage" -> stats?.bowlingAverage
        "bestBowling"    -> stats?.bestBowling
        "catches"        -> stats?.catches
        "goals"          -> stats?.goals
        "assists"        -> stats?.assists
        "futsalFouls"    -> stats?.futsalFouls
        "yellowCards"    -> stats?.yellowCards
        "redCards"       -> stats?.redCards
        else             -> null
    }

    fun formatValue(value: Any?, config: StatConfig): String {
        if (value == null) return "—"
        if (config.isString) return value.toString().ifEmpty { "—" }
        if (config.decimal > 0) {
            val d = (value as? Number)?.toDouble() ?: return "—"
            return String.format("%.${config.decimal}f", d)
        }
        return value.toString()
    }

    // Build list of StatRowItem for adapter
    fun buildRows(
        stats1: PlayeraStatsDTO1?,
        stats2: PlayeraStatsDTO1?,
        sport: String
    ): List<StatRowItem> {
        val configs = getConfig(sport)
        return configs.map { config ->
            val raw1 = getValue(stats1, config.key)
            val raw2 = getValue(stats2, config.key)
            val formatted1 = formatValue(raw1, config)
            val formatted2 = formatValue(raw2, config)

            val v1 = (raw1 as? Number)?.toDouble() ?: 0.0
            val v2 = (raw2 as? Number)?.toDouble() ?: 0.0

            val p1Wins: Boolean
            val p2Wins: Boolean
            val isTie: Boolean

            if (config.isString || (v1 == 0.0 && v2 == 0.0)) {
                p1Wins = false; p2Wins = false; isTie = false
            } else if (v1 == v2) {
                p1Wins = false; p2Wins = false; isTie = true
            } else {
                val v1Better = if (config.lowerIsBetter) v1 < v2 else v1 > v2
                p1Wins = v1Better
                p2Wins = !v1Better
                isTie = false
            }

            StatRowItem(config.label, formatted1, formatted2, p1Wins, p2Wins, isTie)
        }
    }
}