package com.example.fypproject.ScoringDTO

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

object BallViewHelper {

    private val eventAbbreviations = mapOf(
        "wicket"         to "W",
        "noball_runout"  to "NR",   // ← NEW: NB + Run Out label
        "bye"            to "B",
        "legbye"         to "LB",
        "noball"         to "NB",
        "wide"           to "WD",
        "bonus"          to "BN",
        "boundary"       to "",
        "run"            to ""
    )

    fun createBallView(context: Context, ball: CricketBall): android.view.View {
        val sizePx   = dpToPx(context, 42f)
        val badgePx  = dpToPx(context, 14f)

        val bgColor = when (ball.eventType) {
            "wicket",
            "noball_runout"                    -> 0xFFDC2626.toInt()  // ← same red as wicket
            "bye", "legbye", "noball", "wide"  -> 0xFF2563EB.toInt()
            "boundary"                         -> 0xFFFF9800.toInt()
            "run" -> {
                val runs = ball.event.toIntOrNull() ?: 0
                if (runs == 0) 0xFF9E9E9E.toInt() else 0xFF16A34A.toInt()
            }
            else -> 0xFFCA8A04.toInt()
        }

        // Label logic — noball_runout shows e.g. "0NR", others unchanged
        val abbrev = eventAbbreviations[ball.eventType] ?: ball.eventType.uppercase()
        val label  = when (ball.eventType) {
            "noball_runout" -> "${ball.event}NR"
            "run", "boundary" -> ball.event
            else -> "${ball.event}$abbrev"
        }

        // Ball circle TextView
        val ballTv = TextView(context).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(sizePx, sizePx)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            isSingleLine = true
            text = label
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }
        }

        // If no media — return plain ball
        if (!ball.hasMedia) {
            return TextView(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginEnd = dpToPx(context, 6f)
                }
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                isSingleLine = true
                text = label
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(bgColor)
                }
            }
        }

        // Has media — wrap in FrameLayout with 📷 badge
        val container = android.widget.FrameLayout(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                sizePx + dpToPx(context, 6f),
                sizePx + dpToPx(context, 6f)
            ).apply {
                marginEnd = dpToPx(context, 6f)
            }
        }

        container.addView(ballTv)

        val badge = TextView(context).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                badgePx, badgePx, Gravity.TOP or Gravity.END
            )
            text = "📷"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 7f)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF1E1E1E.toInt())
            }
        }

        container.addView(badge)
        return container
    }

    private fun dpToPx(context: Context, dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
        ).toInt()
}