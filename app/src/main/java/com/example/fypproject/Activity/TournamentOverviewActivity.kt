package com.example.fypproject.Activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.Fragment.FixturesFragement
import com.example.fypproject.Fragment.MediaFragment
import com.example.fypproject.Fragment.OverviewFragment
import com.example.fypproject.Fragment.PointsTableFragement
import com.example.fypproject.Fragment.TeamFragement
import com.example.fypproject.databinding.ActivityTournamentOverviewBinding
import com.google.android.material.button.MaterialButton

class TournamentOverviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTournamentOverviewBinding

    private var tournamentId: Long = -1L
    private var seasonId: Long = -1L
    private var sportId: Long = -1L
    private var sportName: String = ""
    private var seasonName: String = ""

    private lateinit var buttons: List<MaterialButton>
    private var loadingCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTournamentOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tournamentId = intent.getLongExtra("tournamentId", -1L)
        seasonId = intent.getLongExtra("seasonId", -1L)
        sportId = intent.getLongExtra("sportId", -1L)
        sportName = intent.getStringExtra("sportName") ?: ""
        seasonName = intent.getStringExtra("seasonName") ?: ""

        binding.tvHeaderTitle.text = sportName

        binding.btnBack.setOnClickListener { finish() }

        binding.btnEdit.setOnClickListener {
            openEditTournament()
        }

        buttons = listOf(
            binding.btnOverview,
            binding.btnFixtures,
            binding.btnTeams,
            binding.btnStats,
            binding.btnPoints,
            binding.btnMedia
        )

        selectButton(binding.btnOverview)
        setMenuVisible(true)
        loadFragment(OverviewFragment.newInstance(tournamentId))

        binding.btnOverview.setOnClickListener {
            selectButton(binding.btnOverview)
            setMenuVisible(true)
            loadFragment(OverviewFragment.newInstance(tournamentId))
        }

        binding.btnFixtures.setOnClickListener {
            selectButton(binding.btnFixtures)
            setMenuVisible(false)
            loadFragment(FixturesFragement.newInstance(tournamentId, sportId))
        }
        binding.btnTeams.setOnClickListener {
            selectButton(binding.btnTeams)
            setMenuVisible(false)
            loadFragment(TeamFragement.newInstance(tournamentId))
        }

        binding.btnPoints.setOnClickListener {
            selectButton(binding.btnPoints)
            setMenuVisible(false)
            loadFragment(PointsTableFragement.newInstance(tournamentId))
        }
        binding.btnMedia.setOnClickListener {
            selectButton(binding.btnMedia)
            setMenuVisible(false)
            loadFragment(MediaFragment.newInstance(tournamentId))
        }
        binding.btnStats.setOnClickListener {
            selectButton(binding.btnStats)
            setMenuVisible(false)
            loadFragment(StatsFragment.newInstance(tournamentId))
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) loadingCount++ else loadingCount = (loadingCount - 1).coerceAtLeast(0)
        binding.progressOverlay.visibility = if (loadingCount > 0) View.VISIBLE else View.GONE
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    private fun selectButton(active: MaterialButton) {
        buttons.forEach {
            it.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.DKGRAY)
        }
        active.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#E31212"))
    }

    private fun setMenuVisible(show: Boolean) {
        binding.btnEdit.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    private fun openEditTournament() {
        val intent = Intent(this, EditTournamentActivity::class.java)
        intent.putExtra("tournamentId", tournamentId)
        intent.putExtra("seasonId", seasonId)
        intent.putExtra("sportId", sportId)
        intent.putExtra("sportName", sportName)
        intent.putExtra("seasonName", seasonName)
        startActivity(intent)
    }
}