package com.example.fypproject.Activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.MatchesDetailAdapter
import com.example.fypproject.DTO.MatchDetail
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityMatchesDetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MatchesDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchesDetailBinding
    private lateinit var adapter: MatchesDetailAdapter
    private var selectedSport: String? = "ALL"
    private var selectedStatus: String = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchesDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        adapter = MatchesDetailAdapter(mutableListOf()) { match ->
            toastShort("${match.team1Name} vs ${match.team2Name}")
        }
        binding.rvMatches.layoutManager = LinearLayoutManager(this)
        binding.rvMatches.adapter = adapter

        binding.btnAllSports.setOnClickListener { onSportSelected("All", it) }
        binding.btnCricket.setOnClickListener { onSportSelected("Cricket", it) }
        binding.btnFootball.setOnClickListener { onSportSelected("Football", it) }
        binding.btnVolleyball.setOnClickListener { onSportSelected("Volleyball", it) }
        binding.btnBadminton.setOnClickListener { onSportSelected("Badminton", it) }
        binding.btnTugOfWar.setOnClickListener { onSportSelected("Tug of War", it) }
        binding.btnLudo.setOnClickListener { onSportSelected("Ludo", it) }
        binding.btnChess.setOnClickListener { onSportSelected("Chess", it) }
        binding.btnTableTennis.setOnClickListener { onSportSelected("Table Tennis", it) }

        binding.btnAllMatches.setOnClickListener { onStatusSelected("ALL", it) }
        binding.btnLive.setOnClickListener { onStatusSelected("LIVE", it) }
        binding.btnUpcoming.setOnClickListener { onStatusSelected("UPCOMING", it) }
        binding.btnCompleted.setOnClickListener { onStatusSelected("FINISHED", it) }

        fetchMatches(selectedSport, selectedStatus)
    }

    private fun onSportSelected(sport: String, view: View) {
        selectedSport = if (sport.equals("ALL", true)) null else sport
        highlightSelectedSport(view)
        fetchMatches(selectedSport, selectedStatus)
    }

    private fun onStatusSelected(status: String, view: View) {
        selectedStatus = status
        highlightSelectedStatus(view)
        fetchMatches(selectedSport, selectedStatus)
    }

    private fun fetchMatches(sport: String?, status: String) {
        setLoading(true)
        api.getMatchesBySport(sport, status).enqueue(object : Callback<List<MatchDetail>> {
            override fun onResponse(call: Call<List<MatchDetail>>, response: Response<List<MatchDetail>>) {
                setLoading(false)
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    adapter.setItems(list)
                } else {
                    toastLong(NetworkUi.userMessage(response, "Failed to load matches"))
                }
            }
            override fun onFailure(call: Call<List<MatchDetail>>, t: Throwable) {
                setLoading(false)
                toastLong(NetworkUi.userMessage(t))
            }
        })
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnBack.isEnabled = !isLoading
    }


    private fun highlightSelectedSport(selected: View) {
        val allButtons = listOf(
            binding.btnAllSports, binding.btnCricket, binding.btnFootball, binding.btnVolleyball,
            binding.btnBadminton, binding.btnTugOfWar, binding.btnLudo, binding.btnChess, binding.btnTableTennis
        )
        for (btn in allButtons) {
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.grayColor))
        }
        selected.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
    }

    private fun highlightSelectedStatus(selected: View) {
        val allButtons = listOf(binding.btnAllMatches, binding.btnLive, binding.btnUpcoming, binding.btnCompleted)
        for (btn in allButtons) {
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.grayColor))
        }
        selected.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
    }
    override fun onResume() {
        super.onResume()
        fetchMatches(selectedSport, selectedStatus)
    }
}
