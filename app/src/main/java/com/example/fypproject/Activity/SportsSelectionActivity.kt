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
                enableButton()

                if (response.isSuccessful) {
                    openNextScreen(seasonId, sportsIds)
                } else {
                    toastLong(NetworkUi.userMessage(response, "Server error"))
                }
            }

            override fun onFailure(call: Call<Season>, t: Throwable) {
                enableButton()
                toastLong(NetworkUi.userMessage(t))
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
        setLoading(true)
    }

    private fun enableButton() {
        binding.btnAdd.isEnabled = true
        binding.btnAdd.text = "Add"
        setLoading(false)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(msg: String) {
        toastShort(msg)
    }
}
