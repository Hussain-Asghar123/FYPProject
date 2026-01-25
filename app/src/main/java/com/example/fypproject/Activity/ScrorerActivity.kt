package com.example.fypproject.Activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.ScrorerAdapter
import com.example.fypproject.Network.ApiClient.api
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
            setLoading(true)
            try {
                val response = api.getMatchesByScorer(scorerId)
                if (response.isSuccessful) {
                    val matchList = response.body()
                    if (!matchList.isNullOrEmpty()) {
                        binding.scoringRecycler.adapter = ScrorerAdapter(matchList)
                    } else {
                        toastShort("No matches assigned to score")
                    }
                } else {
                    toastLong(NetworkUi.userMessage(response, "Failed to fetch matches"))
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnBack.isEnabled = !isLoading
    }
    override fun onResume() {
        super.onResume()
        fetchMatches()
    }
}
