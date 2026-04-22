package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.ScrorerAdapter
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Scoring.CricketScoringActivity
import com.example.fypproject.Utils.MatchNavigator
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityScrorerBinding
import kotlinx.coroutines.launch

class ScrorerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScrorerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScrorerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scoringRecycler.layoutManager = LinearLayoutManager(this)

        binding.btnBack.setOnClickListener {
            finish()
        }

        fetchMatches()
    }

    private fun getScorerId(): Long {
        val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return sharedPref.getLong("id", -1)
    }

    private fun fetchMatches() {
        val scorerId = getScorerId()
        if (scorerId == -1L) {
            toastShort("Scorer ID not found")
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                val response = api.getMatchesByScorer(scorerId)
                if (response.isSuccessful) {
                    val matchList = response.body()
                    if (!matchList.isNullOrEmpty()) {
                        binding.scoringRecycler.adapter = ScrorerAdapter(matchList) { match ->
                            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                            val role = sharedPreferences.getString("role", "")
                            val username = sharedPreferences.getString("username", "") ?: ""
                            if (role.equals("ADMIN", true) || match.scorerId.equals(username, true)) {
                                MatchNavigator.navigate(this@ScrorerActivity, match)
                            }
                        }
                    } else {
                        toastShort("No matches assigned to score")
                    }
                    checkEmptyState()
                } else {
                    toastLong(NetworkUi.userMessage(response, "Failed to fetch matches"))
                    checkEmptyState()
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
                checkEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnBack.isEnabled = !isLoading
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        val isEmpty = (binding.scoringRecycler.adapter?.itemCount ?: 0) == 0
        binding.scoringRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        fetchMatches()
    }
}
