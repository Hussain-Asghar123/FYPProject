package com.example.fypproject.Activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fypproject.DTO.TournamentRequest
import com.example.fypproject.Network.ApiService
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityCreateTournamnetBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateTournamnetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateTournamnetBinding
    private lateinit var api: ApiService
    private var seasonId: Long = -1L
    private var sportId: Long = -1L
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityCreateTournamnetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        api= RetrofitInstance.api
        seasonId = intent.getLongExtra("seasonId", -1L)
        sportId = intent.getLongExtra("sportsId", -1L)
        if (seasonId == -1L || sportId == -1L) {
            finish()
            return
        }
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.etStartDate.setOnClickListener {
            showDatePicker(binding.etStartDate as AppCompatEditText)
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker(binding.etEndDate as AppCompatEditText)
        }

        binding.btnSubmit.setOnClickListener {
            if (validateForm()) {
                createTournament()
            }
        }
    }
    private fun showDatePicker(editText: AppCompatEditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(year, month, day)
                editText.setText(dateFormat.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    private fun validateForm(): Boolean {
        val name = binding.etTournamentName.text.toString().trim()
        val organizer = binding.etOrganizerEmail.text.toString().trim()
        val startDateStr = binding.etStartDate.text.toString().trim()
        val endDateStr = binding.etEndDate.text.toString().trim()

        if (name.isEmpty()) {
            showToast("Tournament name is required")
            return false
        }

        if (organizer.isEmpty()) {
            showToast("Organizer username is required")
            return false
        }

        if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
            showToast("Please select start and end date")
            return false
        }

        val startDate = dateFormat.parse(startDateStr)
        val endDate = dateFormat.parse(endDateStr)

        if (startDate == null || endDate == null) {
            showToast("Invalid date format")
            return false
        }

        if (startDate.after(endDate)) {
            showToast("Start date cannot be after end date")
            return false
        }

        if (binding.rgPaperType.checkedRadioButtonId == -1) {
            showToast("Select player type")
            return false
        }

        if (binding.rgTournamentType.checkedRadioButtonId == -1) {
            showToast("Select tournament type")
            return false
        }

        if (binding.rgTournamentStage.checkedRadioButtonId == -1) {
            showToast("Select tournament stage")
            return false
        }

        return true
    }
    private fun createTournament() {
        disableUi()

        val request = TournamentRequest(
            name = binding.etTournamentName.text.toString().trim(),
            seasonId = seasonId,
            sportsId = sportId,
            username = binding.etOrganizerEmail.text.toString().trim(),
            startDate = binding.etStartDate.text.toString().trim(),
            endDate = binding.etEndDate.text.toString().trim(),
            playerType = getPlayerType(),
            tournamentType = getTournamentType(),
            tournamentStage = getTournamentStage()
        )
        api.createTournament(request).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                enableUi()
                if (response.isSuccessful) {
                    showToast("Tournament created successfully")
                    finish()
                } else {
                    showToast(NetworkUi.userMessage(response, "Failed to create tournament"))
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                enableUi()
                showToast(NetworkUi.userMessage(t))
            }
        })
    }
    private fun getPlayerType(): String =
        if (binding.rbMen.isChecked) "male" else "female"

    private fun getTournamentType(): String =
        if (binding.rbTypeHard.isChecked) "hard" else "tennis"

    private fun getTournamentStage(): String =
        when {
            binding.rbRoundRobin.isChecked -> "round_robin"
            binding.rbRoundRobinKnock.isChecked -> "round_robin_knock"
            binding.rbKnockOut.isChecked -> "knock_out"
            else -> "league"
        }

    private fun disableUi() {
        binding.progressOverlay.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false
    }

    private fun enableUi() {
        binding.progressOverlay.visibility = View.GONE
        binding.btnSubmit.isEnabled = true
    }

    private fun showToast(msg: String) {
        toastShort(msg)
    }
}