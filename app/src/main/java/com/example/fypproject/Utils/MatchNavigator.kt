package com.example.fypproject.Utils

import android.content.Context
import android.content.Intent
import com.example.fypproject.Activity.StartScoringActivity
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Scoring.CricketScoringActivity
import com.example.fypproject.Scoring.FutsalScoringActivity

object MatchNavigator {

    private const val CRICKET      = 1L
    private const val FUTSAL       = 2L
    private const val VOLLEYBALL   = 3L
    private const val TABLE_TENNIS = 4L
    private const val BADMINTON    = 5L
    private const val LUDO         = 6L
    private const val TUG_OF_WAR   = 7L
    private const val CHESS        = 8L

    fun navigate(context: Context, match: MatchResponse) {
        val prefs    = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val role     = prefs.getString("role", "USER") ?: "USER"
        val username = prefs.getString("username", "") ?: ""

        val intent = when (match.status?.uppercase()) {

            "LIVE" -> getScoringIntent(context, match)

            "UPCOMING" -> {
                val isAdmin   = role.equals("ADMIN", ignoreCase = true)
                val isScorer  = match.scorerId.equals(username, ignoreCase = true)
                if (isAdmin || isScorer)
                    Intent(context, StartScoringActivity::class.java)
                        .putExtra("match", match)
                else null
            }

            "COMPLETED", "ABANDONED" -> getScoringIntent(context, match)

            else -> null
        }

        intent?.let { context.startActivity(it) }
    }

    fun getScoringIntent(context: Context, match: MatchResponse): Intent {

        android.util.Log.d("NAV_DEBUG", "sportId = ${match.sportId}, status = ${match.status}")

        val target = when (match.sportId) {
            CRICKET      -> CricketScoringActivity::class.java
            FUTSAL       -> FutsalScoringActivity::class.java
            else         -> {
                android.util.Log.e("NAV_DEBUG", "sportId null ya unknown: ${match.sportId}")
                CricketScoringActivity::class.java
            }
        }
        return Intent(context, target).putExtra("match", match)
    }
}