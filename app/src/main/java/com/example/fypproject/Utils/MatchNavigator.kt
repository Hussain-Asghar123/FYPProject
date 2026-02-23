package com.example.fypproject.Utils

import android.content.Context
import android.content.Intent
import com.example.fypproject.Activity.StartScoringActivity
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Scoring.CricketScoringActivity
import com.example.fypproject.Scoring.FutsalScoringActivity



object MatchNavigator {

    private const val CRICKET = 1L
    private const val FUTSAL = 2L
    private const val VOLLEYBALL = 3L
    private const val TABLE_TENNIS = 4L
    private const val BADMINTON = 5L
    private const val LUDO = 6L
    private const val TUG_OF_WAR = 7L
    private const val CHESS = 8L

    fun navigate(context: Context, match: MatchResponse) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString("role", "USER") ?: "USER"
        val username = sharedPreferences.getString("username", "") ?: ""

        val intent = when (match.status?.uppercase()) {
            "LIVE" -> getLiveScoringIntent(context, match)
            "UPCOMING" -> getUpcomingIntent(context, match, role, username)
            "COMPLETED", "ABANDONED" -> getCompletedIntent(context, match)
            else -> null
        }
        intent?.let { context.startActivity(it) }
    }

    // LIVE -> seedha scoring screen
    private fun getLiveScoringIntent(context: Context, match: MatchResponse): Intent {
        return getScoringIntent(context, match)
    }

    // UPCOMING -> sirf ADMIN ya assigned scorer ke liye StartScoringActivity
    private fun getUpcomingIntent(
        context: Context,
        match: MatchResponse,
        role: String,
        username: String
    ): Intent? {
        return if (role.equals("ADMIN", true) || match.scorerId.equals(username, true)) {
            Intent(context, StartScoringActivity::class.java).putExtra("match", match)
        } else {
            null
        }
    }

    // COMPLETED -> view only (scoring activity read-only mode mein)
    private fun getCompletedIntent(context: Context, match: MatchResponse): Intent {
        return getScoringIntent(context, match)
    }

    // Sport ke hisaab se sahi scoring activity
    private fun getScoringIntent(context: Context, match: MatchResponse): Intent {
        val targetClass = when (match.sportId) {
            CRICKET -> CricketScoringActivity::class.java
            FUTSAL -> FutsalScoringActivity::class.java
            // Jab baaki activities banao toh yahan add karte rehna:
            // VOLLEYBALL -> VolleyballScoringActivity::class.java
            // TABLE_TENNIS -> TableTennisScoringActivity::class.java
            // BADMINTON -> BadmintonScoringActivity::class.java
            // LUDO -> LudoScoringActivity::class.java
            // TUG_OF_WAR -> TugOfWarScoringActivity::class.java
            // CHESS -> ChessScoringActivity::class.java
            else -> CricketScoringActivity::class.java // fallback
        }
        return Intent(context, targetClass).putExtra("match", match)
    }
}