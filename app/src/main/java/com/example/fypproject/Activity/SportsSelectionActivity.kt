package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.fypproject.DTO.Season
import com.example.fypproject.DTO.SeasonSportsRequest
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivitySportsSelectionBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SportsSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySportsSelectionBinding

    companion object {
        private const val CRICKET = 1L
        private const val FUTSAL = 2L
        private const val VOLLEY = 3L
        private const val TABLE_TENNIS = 4L
        private const val BADMINTON = 5L
        private const val LUDO = 6L
        private const val TUG_OF_WAR = 7L
        private const val CHESS = 8L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySportsSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val seasonId = intent.getLongExtra("seasonId", -1L)
        if (seasonId == -1L) {
            showToast("No season found")
            finish()
            return
        }

        val role = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("role", "")

        binding.btnAdd.visibility =
            if (role.equals("ADMIN", true)) View.VISIBLE else View.GONE
        setupCardClickListeners()

        binding.btnAdd.setOnClickListener {
            val selectedSports = getSelectedSports()

            if (selectedSports.isEmpty()) {
                showToast("Please select at least one sport")
                return@setOnClickListener
            }

            disableButton()

            val request = SeasonSportsRequest(
                seasonId = seasonId,
                sportsIds = selectedSports
            )

            addSportsToSeason(request, seasonId, selectedSports)
        }
    }

    private fun setupCardClickListeners() {
        binding.cricketCard.setOnClickListener { binding.cbCricket.toggle() }
        binding.futsalCard.setOnClickListener { binding.cbFutsal.toggle() }
        binding.volleyballCard.setOnClickListener { binding.cbVolleyball.toggle() }
        binding.tabletennisCard.setOnClickListener { binding.cbTableTennis.toggle() }
        binding.badmintonCard.setOnClickListener { binding.cbBadminton.toggle() }
        binding.ludoCard.setOnClickListener { binding.cbLudo.toggle() }
        binding.tugofwarCard.setOnClickListener { binding.cbTugOfWar.toggle() }
        binding.chessCard.setOnClickListener { binding.cbChess.toggle() }
    }

    private fun getSelectedSports(): List<Long> {
        val list = mutableListOf<Long>()
        if (binding.cbCricket.isChecked) list.add(CRICKET)
        if (binding.cbFutsal.isChecked) list.add(FUTSAL)
        if (binding.cbVolleyball.isChecked) list.add(VOLLEY)
        if (binding.cbTableTennis.isChecked) list.add(TABLE_TENNIS)
        if (binding.cbBadminton.isChecked) list.add(BADMINTON)
        if (binding.cbLudo.isChecked) list.add(LUDO)
        if (binding.cbTugOfWar.isChecked) list.add(TUG_OF_WAR)
        if (binding.cbChess.isChecked) list.add(CHESS)
        return list
    }

    private fun addSportsToSeason(
        request: SeasonSportsRequest,
        seasonId: Long,
        sportsIds: List<Long>
    ) {
        api.addSportsToSeason(request).enqueue(object : Callback<Season> {

            override fun onResponse(
                call: Call<Season>,
                response: Response<Season>
            ) {
                showLoading(false)
                enableButton()

                if (response.isSuccessful) {
                    showToast("Sports added successfully!")
                    checkEmptyState()
                    openNextScreen(seasonId, sportsIds)
                } else {
                    toastLong(NetworkUi.userMessage(response, "Server error"))
                    checkEmptyState()
                }
            }

            override fun onFailure(call: Call<Season>, t: Throwable) {
                showLoading(false)
                enableButton()
                toastLong(NetworkUi.userMessage(t))
                checkEmptyState()
            }
        })
    }

    private fun openNextScreen(seasonId: Long, sportsIds: List<Long>) {
        val intent = Intent(this, SeasonsActivity::class.java)
        intent.putExtra("seasonId", seasonId)
        intent.putExtra("sportsIds", sportsIds.toLongArray())
        startActivity(intent)
        finish()
    }

    private fun disableButton() {
        binding.btnAdd.isEnabled = false
        binding.btnAdd.text = "Adding..."
        showLoading(true)
    }

    private fun enableButton() {
        binding.btnAdd.isEnabled = true
        binding.btnAdd.text = "Add"
        showLoading(false)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        val selectedSports = getSelectedSports()
        val isEmpty = selectedSports.isEmpty()
        binding.btnAdd.isEnabled = !isEmpty
    }

    private fun showToast(msg: String) {
        toastShort(msg)
    }
}