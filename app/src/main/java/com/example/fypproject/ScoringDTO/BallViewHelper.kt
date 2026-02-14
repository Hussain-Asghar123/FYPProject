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
        "wicket" to "W",
        "bye" to "B",
        "legbye" to "LB",
        "noball" to "NB",
        "wide" to "WD",
        "bonus" to "BN",
        "boundary" to "",
        "run" to ""
    )

    fun createBallView(context: Context, ball: CricketBall): TextView {
        val sizePx = dpToPx(context, 42f)
        val tv = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                marginEnd = dpToPx(context, 6f)
            }
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            isSingleLine = true

            val bgColor = when (ball.eventType) {
                "wicket"                          -> 0xFFDC2626.toInt() // Red
                "bye", "legbye", "noball", "wide" -> 0xFF2563EB.toInt() // Blue
                "boundary"                        -> 0xFFFF9800.toInt() // Orange (4s and 6s)
                "run" -> {
                    val runs = ball.event.toIntOrNull() ?: 0
                    if (runs == 0) 0xFF9E9E9E.toInt()   // Grey for dot ball
                    else           0xFF16A34A.toInt()    // Green for 1,2,3 runs
                }
                else -> 0xFFCA8A04.toInt() // Yellow (bonus etc)
            }

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }
            background = drawable

            val abbrev = eventAbbreviations[ball.eventType] ?: ball.eventType.uppercase()
            text = if (ball.eventType != "run" && ball.eventType != "boundary") {
                "${ball.event}$abbrev"
            } else {
                ball.event
            }
        }
        return tv
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
        ).toInt()
    }
}