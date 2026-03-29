package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.R
import com.example.fypproject.ScoringDTO.Ball
import com.example.fypproject.databinding.BallItemBinding

class BallByBallAdapter : ListAdapter<Ball, BallByBallAdapter.BallViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BallViewHolder {
        val binding = BallItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BallViewHolder(
        private val binding: BallItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ball: Ball) {


            binding.tvOverBall.text = ball.overBall ?: ""

            // Bowler → Batsman title
            binding.tvBallTitle.text = "${ball.bowlerName} to ${ball.batsmanName}"

            // Non striker
            if (!ball.nonStrikerName.isNullOrEmpty()) {
                binding.tvNonStriker.visibility = View.VISIBLE
                binding.tvNonStriker.text = "Non-striker: ${ball.nonStrikerName}"
            } else {
                binding.tvNonStriker.visibility = View.GONE
            }

            setupBallCircle(ball)
            setupWicket(ball)
            setupBoundary(ball)
            setupExtras(ball)
            setupComment(ball)
        }

        // ✅ React getBallBgColor() — exact same 8-case logic
        private fun setupBallCircle(ball: Ball) {
            val bg = when {
                ball.isWicket == true      -> R.drawable.circle_ball_wicket
                ball.event == "6"          -> R.drawable.circle_ball_six
                ball.event == "4"          -> R.drawable.circle_ball_four
                ball.eventType == "wide"   -> R.drawable.circle_ball_wide
                ball.eventType == "noball" -> R.drawable.circle_ball_noball
                ball.eventType == "bye"    -> R.drawable.circle_ball_bye
                ball.eventType == "legbye" -> R.drawable.circle_ball_legbye
                else                       -> R.drawable.circle_ball_dot
            }

            binding.tvBallEvent.setBackgroundResource(bg)
            binding.tvBallEvent.text = getBallText(ball)
        }

        // ✅ React getBallDisplay() — WD / NB / LB / B + runs
        private fun getBallText(ball: Ball): String {
            val runs = if ((ball.runs ?: 0) > 0) "+${ball.runs}" else ""   // ✅ nullable safe

            return when (ball.extraType) {
                "wide"   -> if (runs.isNotEmpty()) "WD\n$runs" else "WD"
                "noball" -> if (runs.isNotEmpty()) "NB\n$runs" else "NB"
                "bye"    -> if (runs.isNotEmpty()) "B\n$runs"  else "B"
                "legbye" -> if (runs.isNotEmpty()) "LB\n$runs" else "LB"
                else     -> ball.event ?: "0"
            }
        }

        // ✅ React wicket block — OUT! + dismissalType + outPlayer + fielder
        private fun setupWicket(ball: Ball) {
            if (ball.isWicket == true) {                                    // ✅ nullable safe
                binding.wicketCard.visibility = View.VISIBLE

                binding.tvWicketType.text = "OUT! ${ball.dismissalType ?: "Wicket"}"

                binding.tvOutPlayer.text = buildString {
                    append(ball.outPlayerName ?: ball.batsmanName)
                    if (!ball.fielderName.isNullOrEmpty()) {
                        append(" c ${ball.fielderName}")
                    }
                }
            } else {
                binding.wicketCard.visibility = View.GONE
            }
        }

        // ✅ React boundary badge — MAXIMUM / FOUR (sirf jab isWicket false ho)
        private fun setupBoundary(ball: Ball) {
            if (ball.isBoundary == true && ball.isWicket != true) {         // ✅ nullable safe
                binding.boundaryBadge.visibility = View.VISIBLE

                when (ball.event) {
                    "6" -> {
                        binding.boundaryBadge.text = "MAXIMUM"
                        binding.boundaryBadge.setBackgroundResource(R.drawable.badge_six)
                    }
                    "4" -> {
                        binding.boundaryBadge.text = "FOUR"
                        binding.boundaryBadge.setBackgroundResource(R.drawable.badge_four)
                    }
                }
            } else {
                binding.boundaryBadge.visibility = View.GONE
            }
        }

        // ✅ React EXTRA_CONFIG — Wide / No Ball / Bye / Leg Bye + extra runs
        private fun setupExtras(ball: Ball) {
            val config = when (ball.extraType) {
                "wide"   -> Pair("Wide",    R.drawable.badge_wide)
                "noball" -> Pair("No Ball", R.drawable.badge_noball)
                "bye"    -> Pair("Bye",     R.drawable.badge_bye)
                "legbye" -> Pair("Leg Bye", R.drawable.badge_legbye)
                else     -> null
            }

            if (config != null) {
                binding.extraBadge.visibility = View.VISIBLE
                binding.extraBadge.text = config.first
                binding.extraBadge.setBackgroundResource(config.second)

                if ((ball.extra ?: 0) > 0) {                               // ✅ nullable safe
                    binding.tvExtraRuns.visibility = View.VISIBLE
                    binding.tvExtraRuns.text = "+${ball.extra} extra"
                } else {
                    binding.tvExtraRuns.visibility = View.GONE
                }
            } else {
                binding.extraBadge.visibility = View.GONE
                binding.tvExtraRuns.visibility = View.GONE
            }
        }

        // ✅ React commentary block
        private fun setupComment(ball: Ball) {
            if (!ball.comment.isNullOrEmpty()) {
                binding.tvComment.visibility = View.VISIBLE
                binding.tvComment.text = ball.comment
            } else {
                binding.tvComment.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Ball>() {

        override fun areItemsTheSame(oldItem: Ball, newItem: Ball): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ball, newItem: Ball): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.event == newItem.event &&
                    oldItem.runs == newItem.runs &&
                    oldItem.isWicket == newItem.isWicket &&
                    oldItem.comment == newItem.comment
        }
    }
}