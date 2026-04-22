package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TDetailAdapter
import com.example.fypproject.DTO.SportTournamentCount
import com.example.fypproject.DTO.TournamentResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.databinding.ActivityTdetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class TDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTdetailBinding
    private lateinit var adapter: TDetailAdapter

    private val tournamneList=mutableListOf<TournamentResponse>()

    private var seasonId: Long = -1L
    private var sportId: Long = -1L
    private var sportName: String = ""
    private var seasonName: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTdetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        seasonId = intent.getLongExtra("seasonId", -1L)
        sportId = intent.getLongExtra("sportID", -1L)
        sportName = intent.getStringExtra("sportName") ?: ""
        seasonName = intent.getStringExtra("seasonName") ?: ""

        binding.tvTitle.text = sportName
        binding.tvSubtitle.text = seasonName
        binding.ivVideo.setOnClickListener {
            val intent=Intent(this,SportsMediaActivity::class.java)
            intent.putExtra("sportsId",sportId)
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener { finish() }

        val role = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("role", "")
        val isAdmin = role.equals("ADMIN", true)

        binding.btnAdd.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.btnAdd.setOnClickListener {
            val intent = Intent(this, CreateTournamnetActivity::class.java)
            intent.putExtra("seasonId", seasonId)
            intent.putExtra("sportsId", sportId)
            startActivity(intent)
        }

        setupRecycler()
        fetchTournaments()
    }

    private fun setupRecycler() {
        adapter = TDetailAdapter(tournamneList) { clickedTournament->
            val intent= Intent(this, TournamentOverviewActivity::class.java)
            intent.putExtra("tournamentId",clickedTournament.id)
            intent.putExtra("seasonId",seasonId)
            intent.putExtra("sportId",sportId)
            intent.putExtra("sportName",sportName)
            intent.putExtra("seasonName",seasonName)
            startActivity(intent)

        }

        binding.rvSports.layoutManager = LinearLayoutManager(this)
        binding.rvSports.adapter = adapter
    }

    private fun fetchTournaments() {
        showLoading(true)

        api.getSeasonWiseTournaments( seasonId,sportId)

            .enqueue(object : Callback<List<TournamentResponse>> {


                override fun onResponse(
                    call: Call<List<TournamentResponse>>,
                    response: Response<List<TournamentResponse>>
                ) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val list = response.body().orEmpty()
                        tournamneList.clear()
                        tournamneList.addAll(list)
                        adapter.setData(list)
                        if (list.isEmpty()) {
                            showEmpty("No Tournament Available")
                        }
                        checkEmptyState()
                    }
                    else {
                        showEmpty("No Tournament available")
                        checkEmptyState()
                    }
                }

                override fun onFailure(
                    call: Call<List<TournamentResponse>>,
                    t: Throwable
                ) {
                    showLoading(false)
                    showEmpty("No Tournament available")
                    checkEmptyState()
                }
            })
    }

    private fun showEmpty(msg: String) {
        adapter.setData(emptyList())
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = msg
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        val isEmpty = tournamneList.isEmpty()
        binding.rvSports.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        fetchTournaments()
    }
}
