package com.example.fypproject.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.ActivityMatchSummaryBinding
import kotlinx.coroutines.launch

class MatchSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchSummaryBinding
    private var matchId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        matchId = intent.getLongExtra(EXTRA_MATCH_ID, -1L)
        if (matchId == -1L) {
            matchId = intent.getLongExtra("matchId", -1L)
        }

        if (matchId == -1L) {
            showError("Invalid match ID")
            return
        }
        loadSummary()
    }

    private fun loadSummary() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.getMatchSummary(matchId)
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        runOnUiThread { bindData(data) }
                    } else {
                        showError("No summary data available.")
                        checkEmptyState()
                    }
                } else {
                    showError("Failed to load summary.")
                    checkEmptyState()
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
                checkEmptyState()
            } finally {
                runOnUiThread {
                    showLoading(false)
                }
            }
        }
    }

    private fun bindData(data: com.example.fypproject.ScoringDTO.MatchSummaryDto) {
        binding.apply {
            tvMatchResult.text = data.result

            tvTeam1Name.text = data.team1Name
            tvTeam1Score.text = "${data.team1Runs}-${data.team1Wickets}"
            tvTeam1Overs.text = "(${data.team1Overs})"

            tvTeam2Name.text = data.team2Name
            tvTeam2Score.text = "${data.team2Runs}-${data.team2Wickets}"
            tvTeam2Overs.text = "(${data.team2Overs})"

            tvManOfTheMatch.text = data.manOfTheMatch ?: "—"

            tvTeam1HeaderPerformers.text = data.team1Name
            tvTeam2HeaderPerformers.text = data.team2Name

            // Team 1 batsmen
            layoutTeam1Batsmen.removeAllViews()
            data.topBatsmen1?.forEach { player ->
                val row = layoutInflater.inflate(R.layout.item_performer_row, layoutTeam1Batsmen, false)
                row.findViewById<android.widget.TextView>(R.id.tvPlayerName).text = player.playerName
                val tvStat = row.findViewById<android.widget.TextView>(R.id.tvPlayerStat)
                tvStat.text = "${player.runs} runs"
                tvStat.setTextColor(getColor(R.color.colorprimary))
                layoutTeam1Batsmen.addView(row)
            }

            // Team 1 bowlers
            layoutTeam1Bowlers.removeAllViews()
            data.topBowlers1?.forEach { player ->
                val row = layoutInflater.inflate(R.layout.item_performer_row, layoutTeam1Bowlers, false)
                row.findViewById<android.widget.TextView>(R.id.tvPlayerName).text = player.playerName
                val tvStat = row.findViewById<android.widget.TextView>(R.id.tvPlayerStat)
                tvStat.text = "${player.wickets}-${player.runsConceded}"
                tvStat.setTextColor(getColor(android.R.color.holo_red_dark))
                layoutTeam1Bowlers.addView(row)
            }

            // Team 2 batsmen
            layoutTeam2Batsmen.removeAllViews()
            data.topBatsmen2?.forEach { player ->
                val row = layoutInflater.inflate(R.layout.item_performer_row, layoutTeam2Batsmen, false)
                row.findViewById<android.widget.TextView>(R.id.tvPlayerName).text = player.playerName
                val tvStat = row.findViewById<android.widget.TextView>(R.id.tvPlayerStat)
                tvStat.text = "${player.runs} runs"
                tvStat.setTextColor(getColor(R.color.colorprimary))
                layoutTeam2Batsmen.addView(row)
            }

            // Team 2 bowlers
            layoutTeam2Bowlers.removeAllViews()
            data.topBowlers2?.forEach { player ->
                val row = layoutInflater.inflate(R.layout.item_performer_row, layoutTeam2Bowlers, false)
                row.findViewById<android.widget.TextView>(R.id.tvPlayerName).text = player.playerName
                val tvStat = row.findViewById<android.widget.TextView>(R.id.tvPlayerStat)
                tvStat.text = "${player.wickets}-${player.runsConceded}"
                tvStat.setTextColor(getColor(android.R.color.holo_red_dark))
                layoutTeam2Bowlers.addView(row)
            }


            layoutLoading.visibility = View.GONE
            tvError.visibility = View.GONE
            layoutContent.visibility = View.VISIBLE
            checkEmptyState()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.layoutContent.visibility = View.GONE
            binding.tvError.visibility = View.GONE
        }
    }

    private fun checkEmptyState() {
        // Check if summary data is available
        val isEmpty = binding.tvMatchResult.text.isNullOrEmpty() || binding.tvMatchResult.text.toString().trim().isEmpty()
        binding.layoutContent.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvError.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        runOnUiThread {
            binding.layoutLoading.visibility = View.GONE
            binding.layoutContent.visibility = View.GONE
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = message
        }
    }

    companion object {
        const val EXTRA_MATCH_ID = "extra_match_id"

        fun newIntent(context: Context, matchId: Long): Intent {
            return Intent(context, MatchSummaryActivity::class.java).apply {
                putExtra(EXTRA_MATCH_ID, matchId)
            }
        }
    }
}