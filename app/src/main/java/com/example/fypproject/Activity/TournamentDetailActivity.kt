package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TournamentDetailAdapter
import com.example.fypproject.DTO.SportTournamentCount
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.databinding.ActivityTournamentDetailBinding
import kotlinx.coroutines.launch

class TournamentDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTournamentDetailBinding
    private lateinit var adapter: TournamentDetailAdapter
    private val sportList=mutableListOf<SportTournamentCount>()
    private var seasonId:Long=-1L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityTournamentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.ivVideo.setOnClickListener {
            val intent=Intent(this,SeasonMediaActivity::class.java)
            intent.putExtra("seasonId",seasonId)
            startActivity(intent)
        }
        seasonId=intent.getLongExtra("seasonId",-1L)
        val seasonName=intent.getStringExtra("seasonName")
        binding.tvTitle.text=seasonName
        binding.tvSubtitle.text=seasonName

        setupRecyclerView()
        setupAddButton()
        if(seasonId!= -1L){
            loadSports(seasonId)
        }
    }
    private fun setupAddButton(){
      val role=getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("role","")
        binding.btnAdd.visibility=if(role.equals("ADMIN",true)) View.VISIBLE else View.GONE
        binding.btnAdd.setOnClickListener {
            if(seasonId == -1L){
                return@setOnClickListener
            }
            val intent= Intent(this, SportsSelectionActivity::class.java)
            intent.putExtra("seasonId",seasonId)
            startActivity(intent)
        }
    }
    private fun setupRecyclerView(){
        adapter=TournamentDetailAdapter(sportList) { clickedSport ->
            val intent = Intent(this, TDetailActivity::class.java)
            intent.putExtra("seasonId", seasonId)
            intent.putExtra("sportID", clickedSport.sportId)
            intent.putExtra("sportName", clickedSport.name)
            intent.putExtra("seasonName", binding.tvTitle.text.toString())
            startActivity(intent)
        }
        binding.rvSports.adapter=adapter
        binding.rvSports.layoutManager= LinearLayoutManager(this)
    }
    private fun loadSports(seasonId: Long) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                val response = api.getSeasonById(seasonId)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val addedSports = data
                        .map {
                            SportTournamentCount(
                                sportId = it.sportId,
                                name = it.name,
                                tournamentCount = it.tournamentCount
                            )
                        }

                    sportList.clear()
                    sportList.addAll(addedSports)
                    adapter.updateList(addedSports)
                    checkEmptyState()
                } else {
                    toastLong(NetworkUi.userMessage(response, "No sports found"))
                    checkEmptyState()
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
                checkEmptyState()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun checkEmptyState() {
        val isEmpty = sportList.isEmpty()
        binding.rvSports.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvNoData.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAdd.isEnabled = !isLoading
        binding.ivBack.isEnabled = !isLoading
    }

    override fun onResume() {
        super.onResume()
        loadSports(seasonId)
    }
}
