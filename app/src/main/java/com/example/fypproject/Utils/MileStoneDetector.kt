package com.example.fypproject.Utils

import com.example.fypproject.DTO.MilestoneDto
import com.example.fypproject.ScoringDTO.CricketBall


object MilestoneDetector {

    fun detectCricketMilestone(
        balls: List<CricketBall>?,
        data: Map<String, Any?>,
        prevData: Map<String, Any?>?,
        isDoubleWicket: Boolean = false
    ): MilestoneDto? {
        if (balls.isNullOrEmpty()) return null
        val prevBalls = prevData?.getList("cricketBalls") ?: emptyList<Any>()

        // Guard: only on genuinely new ball
        if (balls.size <= prevBalls.size) return null

        val latest = balls.last()
        val event = latest.event.lowercase()

        // 1. Consecutive Sixes
        if (event == "6") {
            val sixStreak     = balls.count { it.event == "6" }
            val prevSixStreak = balls.dropLast(1).count { it.event == "6" }
            if (sixStreak == 3 && prevSixStreak == 2) return MilestoneDto(
                "3 Consecutive Sixes! 🔥", "Batter on fire!", "🎆", "gold"
            )
            if (sixStreak == 4 && prevSixStreak == 3) return MilestoneDto(
                "4 Sixes in a Row!", "Unstoppable!", "🚀", "gold"
            )
            if (sixStreak >= 5 && prevSixStreak == sixStreak - 1) return MilestoneDto(
                "$sixStreak Sixes Streak!", "LEGEND", "🏆", "gold"
            )
        }

        // 2. Consecutive Fours
        if (event == "4") {
            val fourStreak     = balls.count { it.event == "4" }
            val prevFourStreak = balls.dropLast(1).count { it.event == "4" }
            if (fourStreak == 3 && prevFourStreak == 2) return MilestoneDto(
                "3 Fours in a Row!", "Boundary Machine", "🏅", "blue"
            )
            if (fourStreak >= 4 && prevFourStreak == fourStreak - 1) return MilestoneDto(
                "$fourStreak Fours in a Row!", "Excellent!", "🏅", "blue"
            )
        }

        // 3. Hat-Trick
        if (latest.eventType == "wicket") {
            val last3 = balls.takeLast(3)
            if (last3.size == 3 && last3.all { it.eventType == "wicket" }) {
                return MilestoneDto(
                    "HAT-TRICK! 🎩",
                    "3 wickets in a row!",
                    "🎩", "red"
                )
            }
        }

        // 4. Batsman Milestones (50/100/150)
        fun checkBatsman(
            currentStats: Map<String, Any?>?,
            prevStatsMap: Map<String, Any?>?
        ): MilestoneDto? {
            val runs     = (currentStats?.get("runs") as? Number)?.toInt() ?: return null
            val prevRuns = (prevStatsMap?.get("runs") as? Number)?.toInt() ?: 0
            val name     = currentStats["playerName"] as? String ?: "Batsman"
            return when {
                prevRuns < 50  && runs >= 50  && runs < 100 -> MilestoneDto("FIFTY! ✿",  "$name — 50 runs",      "🏆", "gold")
                prevRuns < 100 && runs >= 100 && runs < 150 -> MilestoneDto("CENTURY! 🎉", "$name — 100 runs!",   "🏆", "gold")
                prevRuns < 150 && runs >= 150               -> MilestoneDto("150! MAGNIFICENT!", name,            "🏆", "gold")
                else -> null
            }
        }

        @Suppress("UNCHECKED_CAST")
        val b1Stats = data["batsman1Stats"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val b2Stats = data["batsman2Stats"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val pb1Stats = prevData?.get("batsman1Stats") as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val pb2Stats = prevData?.get("batsman2Stats") as? Map<String, Any?>

        checkBatsman(b1Stats, pb1Stats)?.let { return it }
        checkBatsman(b2Stats, pb2Stats)?.let { return it }

        // 5. Bowler 5-wicket haul
        @Suppress("UNCHECKED_CAST")
        val bowlerStats = data["bowlerStats"] as? Map<String, Any?>
        val bowlerW     = (bowlerStats?.get("wickets") as? Number)?.toInt() ?: 0
        @Suppress("UNCHECKED_CAST")
        val prevBowlerW = (prevData?.getMap("bowlerStats")?.get("wickets") as? Number)?.toInt() ?: 0
        if (prevBowlerW < 5 && bowlerW >= 5) return MilestoneDto(
            "FIVE-FOR! 🔥",
            "${bowlerStats?.get("playerName") ?: "Bowler"} — 5 wickets!",
            "🎩", "red"
        )

        // 6. Match-winning shot
        val firstInnings = data["firstInnings"] as? Boolean ?: true
        val target       = (data["target"] as? Number)?.toInt() ?: Int.MAX_VALUE
        if (!firstInnings && target <= 0 && !isDoubleWicket) {
            if (event == "6")  return MilestoneDto("WON WITH A SIX! 🏆",  "Match winner!", "🏆", "green")
            if (event == "4") return MilestoneDto("WON WITH A FOUR! 🏆", "Match winner!", "🏆", "green")
        }

        // 7. Maiden Over
        val overs     = (data["overs"] as? Number)?.toInt() ?: 0
        val ballsNum  = (data["balls"] as? Number)?.toInt() ?: 0
        val prevOvers = (prevData?.get("overs") as? Number)?.toInt() ?: 0
        if (ballsNum == 0 && overs > 0 && overs > prevOvers) {
            val completedOverNo = overs - 1
            val overBalls = balls.filter { it.eventType != "wicket" && it.event !in listOf("4", "6") }
            if (overBalls.size >= 6) {
                return MilestoneDto(
                    "MAIDEN OVER! 🎯",
                    "Over $completedOverNo",
                    "🎯", "blue"
                )
            }
        }

        return null
    }

    // ── VOLLEYBALL ───────────────────────────────────────────────────────────────
    fun detectVolleyballMilestone(
        data: Map<String, Any?>,
        prevData: Map<String, Any?>?
    ): MilestoneDto? {
        if (prevData == null) return null
        val events     = data.getList("volleyballEvents")
        val prevEvents = prevData.getList("volleyballEvents")
        if (events.size <= prevEvents.size) return null

        @Suppress("UNCHECKED_CAST")
        val latest = events.last() as? Map<String, Any?> ?: return null
        val scoringTypes = setOf("POINT", "ACE", "BLOCK")

        if (latest["eventType"] == "ACE") return MilestoneDto(
            "ACE! 🎾", "${latest["playerName"] ?: latest["teamName"] ?: ""}", "🎾", "gold"
        )
        if (latest["eventType"] == "BLOCK") return MilestoneDto(
            "MONSTER BLOCK! 🛡", "${latest["playerName"] ?: latest["teamName"] ?: ""}", "🛡", "red"
        )

        // 3 consecutive points same team
        if (scoringTypes.contains(latest["eventType"] as? String)) {
            @Suppress("UNCHECKED_CAST")
            val last3 = events.takeLast(3).map { it as Map<String, Any?> }
            if (last3.size == 3 &&
                last3.all { it["teamId"] == latest["teamId"] } &&
                last3.all { scoringTypes.contains(it["eventType"] as? String) }) {
                @Suppress("UNCHECKED_CAST")
                val prev2 = prevEvents.takeLast(2).map { it as Map<String, Any?> }
                val prevStreak2 = prev2.size == 2 &&
                        prev2.all { it["teamId"] == latest["teamId"] && scoringTypes.contains(it["eventType"] as? String) }
                if (prevStreak2) return MilestoneDto(
                    "${latest["teamName"] ?: "Team"} — 3 Point Run! ⚡", "Momentum shift!", "⚡", "blue"
                )
            }
        }

        // Set won
        val t1Sets  = (data["team1Sets"] as? Number)?.toInt() ?: 0
        val t2Sets  = (data["team2Sets"] as? Number)?.toInt() ?: 0
        val pt1Sets = (prevData["team1Sets"] as? Number)?.toInt() ?: 0
        val pt2Sets = (prevData["team2Sets"] as? Number)?.toInt() ?: 0
        if (t1Sets != pt1Sets || t2Sets != pt2Sets) {
            val winnerName = if (t1Sets > pt1Sets) data["team1Name"] as? String ?: "Team 1"
            else data["team2Name"] as? String ?: "Team 2"
            return MilestoneDto("Set Won! 🏐", "$winnerName — Set ${t1Sets + t2Sets}", "🏐", "green")
        }

        return null
    }

    // ── FUTSAL ───────────────────────────────────────────────────────────────────
    fun detectFutsalMilestone(
        data: Map<String, Any?>,
        prevData: Map<String, Any?>?
    ): MilestoneDto? {
        if (prevData == null) return null
        val events     = data.getList("futsalEvents")
        val prevEvents = prevData.getList("futsalEvents")
        if (events.size <= prevEvents.size) return null

        @Suppress("UNCHECKED_CAST")
        val latest = events.last() as? Map<String, Any?> ?: return null

        if (latest["eventType"] == "GOAL" || latest["eventType"] == "OWN_GOAL") {
            if (latest["eventType"] == "OWN_GOAL") return MilestoneDto(
                "Own Goal! 😬",
                "${latest["playerName"] ?: latest["scorerName"] ?: "Player"} — into their own net",
                "😬", "red"
            )
            if (latest["goalType"] == "PENALTY") return MilestoneDto(
                "Penalty Goal! 🎾",
                "${latest["playerName"] ?: latest["scorerName"] ?: latest["teamName"] ?: ""}",
                "🎾", "gold"
            )

            @Suppress("UNCHECKED_CAST")
            val playerGoals     = events.count { (it as? Map<String, Any?>)?.get("eventType") == "GOAL" && (it["playerId"] == latest["playerId"] || it["scorerName"] == latest["scorerName"]) }
            @Suppress("UNCHECKED_CAST")
            val prevPlayerGoals = prevEvents.count { (it as? Map<String, Any?>)?.get("eventType") == "GOAL" && (it["playerId"] == latest["playerId"] || it["scorerName"] == latest["scorerName"]) }

            if (playerGoals == 3 && prevPlayerGoals == 2) return MilestoneDto(
                "HAT-TRICK! 🎩",
                "${latest["playerName"] ?: latest["scorerName"] ?: "Player"} — 3 goals!",
                "🎩", "gold"
            )
            if (playerGoals == 2 && prevPlayerGoals == 1) return MilestoneDto(
                "BRACE! ⚽⚽",
                "${latest["playerName"] ?: latest["scorerName"] ?: "Player"} — 2 goals",
                "⚽", "green"
            )
            if (playerGoals >= 4 && prevPlayerGoals == playerGoals - 1) return MilestoneDto(
                "$playerGoals Goals! INSANE! 🔥",
                "${latest["playerName"] ?: latest["scorerName"] ?: "Player"}",
                "🔥", "gold"
            )

            val currT1 = (data["team1Score"] as? Number)?.toInt() ?: 0
            val currT2 = (data["team2Score"] as? Number)?.toInt() ?: 0
            val prevT1 = (prevData["team1Score"] as? Number)?.toInt() ?: 0
            val prevT2 = (prevData["team2Score"] as? Number)?.toInt() ?: 0
            if (currT1 == currT2 && prevT1 != prevT2) return MilestoneDto(
                "EQUALIZER! ⚡",
                "${latest["teamName"] ?: "Team"} — $currT1–$currT2",
                "⚡", "blue"
            )
            if (currT1 + currT2 == 1) return MilestoneDto(
                "FIRST BLOOD! 🔴",
                "${latest["playerName"] ?: latest["scorerName"] ?: latest["teamName"] ?: ""}",
                "🔴", "green"
            )
        }

        if (latest["eventType"] == "RED_CARD") return MilestoneDto(
            "RED CARD! 🟥",
            "${latest["playerName"] ?: "Player"} — ${latest["teamName"] ?: "Team"} is down!",
            "🟥", "red"
        )

        val t1Fouls  = (data["team1Fouls"] as? Number)?.toInt() ?: 0
        val t2Fouls  = (data["team2Fouls"] as? Number)?.toInt() ?: 0
        val pt1Fouls = (prevData["team1Fouls"] as? Number)?.toInt() ?: 0
        val pt2Fouls = (prevData["team2Fouls"] as? Number)?.toInt() ?: 0
        if (pt1Fouls < 5 && t1Fouls >= 5) return MilestoneDto(
            "5 Fouls! Penalty Zone ⚠️",
            "${data["team1Name"] ?: "Team 1"} — opponents get penalty kicks",
            "⚠️", "red"
        )
        if (pt2Fouls < 5 && t2Fouls >= 5) return MilestoneDto(
            "5 Fouls! Penalty Zone ⚠️",
            "${data["team2Name"] ?: "Team 2"} — opponents get penalty kicks",
            "⚠️", "red"
        )

        return null
    }

    // ── TABLE TENNIS ─────────────────────────────────────────────────────────────
    fun detectTableTennisMilestone(
        data: Map<String, Any?>,
        prevData: Map<String, Any?>?
    ): MilestoneDto? {
        if (prevData == null) return null
        val events     = data.getList("tableTennisEvents")
        val prevEvents = prevData.getList("tableTennisEvents")
        if (events.size <= prevEvents.size) return null

        val t1  = (data["team1Points"] as? Number)?.toInt() ?: 0
        val t2  = (data["team2Points"] as? Number)?.toInt() ?: 0
        val pt1 = (prevData["team1Points"] as? Number)?.toInt() ?: 0
        val pt2 = (prevData["team2Points"] as? Number)?.toInt() ?: 0
        val g1  = (data["team1Games"] as? Number)?.toInt() ?: 0
        val g2  = (data["team2Games"] as? Number)?.toInt() ?: 0
        val pg1 = (prevData["team1Games"] as? Number)?.toInt() ?: 0
        val pg2 = (prevData["team2Games"] as? Number)?.toInt() ?: 0

        if (g1 > pg1) return MilestoneDto("Game ${g1 + g2} Won! 🏓", "${data["team1Name"] ?: "Team 1"} — $g1–$g2 in games", "🏓", "green")
        if (g2 > pg2) return MilestoneDto("Game ${g1 + g2} Won! 🏓", "${data["team2Name"] ?: "Team 2"} — $g2–$g1 in games", "🏓", "green")

        val deucePts = ((data["pointsPerGame"] as? Number)?.toInt() ?: 11) - 1
        if (t1 == deucePts && t2 == deucePts && !(pt1 == deucePts && pt2 == deucePts)) return MilestoneDto(
            "DEUCE! 🏓",
            "${data["team1Name"] ?: "Team 1"} vs ${data["team2Name"] ?: "Team 2"} — $deucePts–$deucePts",
            "🏓", "blue"
        )

        val gameTarget = (data["pointsPerGame"] as? Number)?.toInt() ?: 11
        val deuceMode  = t1 >= deucePts && t2 >= deucePts
        if (!deuceMode) {
            if (t1 == gameTarget - 1 && t1 > t2 && !(pt1 == gameTarget - 1 && pt1 > pt2))
                return MilestoneDto("Game Point! 🎾", "${data["team1Name"] ?: "Team 1"} — one away!", "🎾", "gold")
            if (t2 == gameTarget - 1 && t2 > t1 && !(pt2 == gameTarget - 1 && pt2 > pt1))
                return MilestoneDto("Game Point! 🎾", "${data["team2Name"] ?: "Team 2"} — one away!", "🎾", "gold")
        }

        return null
    }

    // ── BADMINTON ────────────────────────────────────────────────────────────────
    fun detectBadmintonMilestone(
        data: Map<String, Any?>,
        prevData: Map<String, Any?>?
    ): MilestoneDto? {
        if (prevData == null) return null
        val events     = data.getList("badmintonEvents")
        val prevEvents = prevData.getList("badmintonEvents")
        if (events.size <= prevEvents.size) return null

        val g1  = (data["team1Games"] as? Number)?.toInt() ?: 0
        val g2  = (data["team2Games"] as? Number)?.toInt() ?: 0
        val pg1 = (prevData["team1Games"] as? Number)?.toInt() ?: 0
        val pg2 = (prevData["team2Games"] as? Number)?.toInt() ?: 0

        if (g1 > pg1) {
            val gamesNeeded = (data["gamesToWin"] as? Number)?.toInt() ?: 2
            if (g1 == gamesNeeded - 1 && g2 == gamesNeeded - 1)
                return MilestoneDto("Deciding Game! ⚡", "${data["team1Name"] ?: "Team 1"} vs ${data["team2Name"] ?: "Team 2"} — Winner takes all!", "⚡", "red")
            return MilestoneDto("Game ${g1 + g2} Won! 🏸", "${data["team1Name"] ?: "Team 1"} — $g1–$g2 games", "🏸", "green")
        }
        if (g2 > pg2) {
            val gamesNeeded = (data["gamesToWin"] as? Number)?.toInt() ?: 2
            if (g1 == gamesNeeded - 1 && g2 == gamesNeeded - 1)
                return MilestoneDto("Deciding Game! ⚡", "${data["team1Name"] ?: "Team 1"} vs ${data["team2Name"] ?: "Team 2"} — Winner takes all!", "⚡", "red")
            return MilestoneDto("Game ${g1 + g2} Won! 🏸", "${data["team2Name"] ?: "Team 2"} — $g2–$g1 games", "🏸", "green")
        }

        val t1     = (data["team1Points"] as? Number)?.toInt() ?: 0
        val t2     = (data["team2Points"] as? Number)?.toInt() ?: 0
        val pt1    = (prevData["team1Points"] as? Number)?.toInt() ?: 0
        val pt2    = (prevData["team2Points"] as? Number)?.toInt() ?: 0
        val deucePts = ((data["pointsPerGame"] as? Number)?.toInt() ?: 21) - 1

        if (t1 == deucePts && t2 == deucePts && !(pt1 == deucePts && pt2 == deucePts)) return MilestoneDto(
            "DEUCE! 🏸", "$deucePts–$deucePts — First to 2 ahead wins!", "🏸", "blue"
        )

        val gameTarget = (data["pointsPerGame"] as? Number)?.toInt() ?: 21
        val deuceMode  = t1 >= deucePts && t2 >= deucePts
        if (!deuceMode) {
            if (t1 == gameTarget - 1 && t1 > t2 && !(pt1 == gameTarget - 1 && pt1 > pt2))
                return MilestoneDto("Game Point! 🎾", "${data["team1Name"] ?: "Team 1"} — one away!", "🎾", "gold")
            if (t2 == gameTarget - 1 && t2 > t1 && !(pt2 == gameTarget - 1 && pt2 > pt1))
                return MilestoneDto("Game Point! 🎾", "${data["team2Name"] ?: "Team 2"} — one away!", "🎾", "gold")
        }

        return null
    }

    // ── TUG OF WAR ───────────────────────────────────────────────────────────────
    fun detectTugOfWarMilestone(
        data: Map<String, Any?>,
        prevData: Map<String, Any?>?
    ): MilestoneDto? {
        if (prevData == null) return null
        val events     = data.getList("tugOfWarEvents")
        val prevEvents = prevData.getList("tugOfWarEvents")
        if (events.size <= prevEvents.size) return null

        val r1  = (data["team1Rounds"] as? Number)?.toInt() ?: 0
        val r2  = (data["team2Rounds"] as? Number)?.toInt() ?: 0
        val pr1 = (prevData["team1Rounds"] as? Number)?.toInt() ?: 0
        val pr2 = (prevData["team2Rounds"] as? Number)?.toInt() ?: 0

        if (r1 > pr1) {
            if (pr1 < pr2) return MilestoneDto("COMEBACK! Round Won! 💪", "${data["team1Name"] ?: "Team 1"} — back in it!", "💪", "blue")
            val rNeeded = (data["roundsToWin"] as? Number)?.toInt() ?: 3
            if (r1 == rNeeded - 1 && r2 == rNeeded - 1) return MilestoneDto("Deciding Round! ⚡", "Winner takes the match!", "⚡", "red")
            return MilestoneDto("Round ${r1 + r2} Won! 🏆", "${data["team1Name"] ?: "Team 1"} — $r1–$r2 rounds", "🏆", "green")
        }
        if (r2 > pr2) {
            if (pr2 < pr1) return MilestoneDto("COMEBACK! Round Won! 💪", "${data["team2Name"] ?: "Team 2"} — back in it!", "💪", "blue")
            val rNeeded = (data["roundsToWin"] as? Number)?.toInt() ?: 3
            if (r1 == rNeeded - 1 && r2 == rNeeded - 1) return MilestoneDto("Deciding Round! ⚡", "Winner takes the match!", "⚡", "red")
            return MilestoneDto("Round ${r1 + r2} Won! 🏆", "${data["team2Name"] ?: "Team 2"} — $r2–$r1 rounds", "🏆", "green")
        }

        return null
    }

    // ── LUDO ─────────────────────────────────────────────────────────────────────
    fun detectLudoMilestone(
        data: Map<String, Any?>,
        prevData: Map<String, Any?>?
    ): MilestoneDto? {
        if (prevData == null) return null
        val events     = data.getList("ludoEvents")
        val prevEvents = prevData.getList("ludoEvents")
        if (events.size <= prevEvents.size) return null

        @Suppress("UNCHECKED_CAST")
        val latest = events.last() as? Map<String, Any?> ?: return null

        if (latest["eventType"] == "HOME_RUN") {
            val h1  = (data["team1HomeRuns"] as? Number)?.toInt() ?: 0
            val h2  = (data["team2HomeRuns"] as? Number)?.toInt() ?: 0
            val max = (data["maxHomeRuns"]   as? Number)?.toInt() ?: 4
            if (h1 >= max || h2 >= max) {
                val winner = if (latest["teamId"] == data["team1Id"]) data["team1Name"] ?: "Team 1"
                else data["team2Name"] ?: "Team 2"
                return MilestoneDto("ALL HOME! 🏆", "$winner WINS!", "🏆", "gold")
            }
            val teamRuns  = if (latest["teamId"] == data["team1Id"]) h1 else h2
            val threeFour = Math.ceil(max * 3.0 / 4).toInt()
            if (teamRuns == threeFour) {
                val tName = if (latest["teamId"] == data["team1Id"]) data["team1Name"] ?: "Team 1" else data["team2Name"] ?: "Team 2"
                return MilestoneDto("Almost There! 🏁", "$tName — $teamRuns/$max home", "🏁", "green")
            }
            val tName = if (latest["teamId"] == data["team1Id"]) data["team1Name"] ?: "Team 1" else data["team2Name"] ?: "Team 2"
            return MilestoneDto("Home Run! 🏁", "$tName — $teamRuns/$max", "🏁", "green")
        }

        if (latest["eventType"] == "WIN") return MilestoneDto(
            "MATCH WON! 🎊",
            "${latest["teamName"] ?: "Team"}",
            "🎊", "gold"
        )

        return null
    }

    // ── CHESS ────────────────────────────────────────────────────────────────────
    fun detectChessMilestone(
        data: Map<String, Any?>,
        prevData: Map<String, Any?>?
    ): MilestoneDto? {
        if (prevData == null) return null
        val events     = data.getList("chessEvents")
        val prevEvents = prevData.getList("chessEvents")
        if (events.size <= prevEvents.size) return null

        @Suppress("UNCHECKED_CAST")
        val latest = events.last() as? Map<String, Any?> ?: return null

        return when (latest["eventType"] as? String) {
            "CHECKMATE"   -> MilestoneDto("CHECKMATE! ♛", "${latest["teamName"] ?: "Player"} WINS!", "♛", "gold")
            "DRAW_AGREED" -> MilestoneDto("Draw by Agreement 🤝", "${data["team1Name"] ?: ""} vs ${data["team2Name"] ?: ""}", "🤝", "blue")
            "STALEMATE"   -> MilestoneDto("Stalemate — Draw! 🤝", "No legal moves — game drawn", "🤝", "blue")
            "RESIGN"      -> MilestoneDto("Resigned! 🏳", "${latest["teamName"] ?: "Player"} — surrenders", "🏳", "red")
            "TIMEOUT"     -> MilestoneDto("Time Out! ⏱", "${latest["teamName"] ?: "Player"} — ran out of time", "⏱", "red")
            else          -> null
        }
    }

    // ── Helper extensions ─────────────────────────────────────────────────────────
    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.getList(key: String): List<Any> =
        (this[key] as? List<*>)?.filterNotNull() ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.getMap(key: String): Map<String, Any?>? =
        this[key] as? Map<String, Any?>
}