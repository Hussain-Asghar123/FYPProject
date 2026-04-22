package com.example.fypproject.Activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.fypproject.DTO.TournamentUpdateRequest
import com.example.fypproject.Network.ApiService
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityEditTournamentBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class EditTournamentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTournamentBinding
    private lateinit var api: ApiService

    private var tournamentId = -1L
    private var seasonId = -1L
    private var sportId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditTournamentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = RetrofitInstance.api

        tournamentId = intent.getLongExtra("tournamentId", -1L)
        seasonId = intent.getLongExtra("seasonId", -1L)
        sportId = intent.getLongExtra("sportId", -1L)
        updateTournamentTypeUI(sportId)

        if (tournamentId == -1L) finish()

        binding.ivBack.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { validateAndSubmit() }

        setupDatePickers()
        loadTournament()
    }
    private fun updateTournamentTypeUI(sportId: Long) {
        val isVisible = sportId == 1L
        binding.tvType.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.rgTournamentType.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            showDatePicker { binding.etStartDate.setText(it) }
        }
        binding.etEndDate.setOnClickListener {
            showDatePicker { binding.etEndDate.setText(it) }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, y, m, d ->
                val date = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    y,
                    m + 1,
                    d
                )
                onDateSelected(date)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadTournament() {
        showLoading(true)
        binding.btnSubmit.isEnabled = false
        api.getTournamentById(tournamentId).enqueue(object : Callback<TournamentUpdateRequest> {
                override fun onResponse(
                    call: Call<TournamentUpdateRequest>,
                    response: Response<TournamentUpdateRequest>
                ) {
                    showLoading(false)
                    binding.btnSubmit.isEnabled = true
                    if (!response.isSuccessful || response.body() == null) {
                        toastLong(NetworkUi.userMessage(response, "Failed to load tournament"))
                        return
                    }
                    val t = response.body() ?: return

                    binding.etTournamentName.setText(t.name)
                    binding.etStartDate.setText(t.startDate)
                    binding.etEndDate.setText(t.endDate)
                    binding.etOrganizerEmail.setText(t.username)

                    if (t.playerType.equals("Male", true))
                        binding.rbMen.isChecked = true
                    else
                        binding.rbWomen.isChecked = true

                    if (t.tournamentType.equals("Hard", true))
                        binding.rbTypeHard.isChecked = true
                    else
                        binding.rbTypeTennis.isChecked = true

                    when (t.tournamentStage) {
                        "Round Robin" -> binding.rbRoundRobin.isChecked = true
                        "Round Robin + Knock Out" -> binding.rbRoundRobinKnock.isChecked = true
                        "Knock Out" -> binding.rbKnockOut.isChecked = true
                        "League" -> binding.rbLeague.isChecked = true
                    }
                    checkEmptyState()
                }

                override fun onFailure(call: Call<TournamentUpdateRequest>, t: Throwable) {
                    showLoading(false)
                    binding.btnSubmit.isEnabled = true
                    toastLong(NetworkUi.userMessage(t))
                    checkEmptyState()
                }
            })
    }

    private fun validateAndSubmit() {

        val name = binding.etTournamentName.text.toString().trim()
        val startDate = binding.etStartDate.text.toString()
        val endDate = binding.etEndDate.text.toString()
        val username = binding.etOrganizerEmail.text.toString().trim()

        when {
            name.isEmpty() -> return toast("Tournament name required")
            startDate.isEmpty() -> return toast("Start date required")
            endDate.isEmpty() -> return toast("End date required")
            username.isEmpty() -> return toast("Organizer email required")
            !binding.rbMen.isChecked && !binding.rbWomen.isChecked ->
                return toast("Select player type")
            !binding.rbTypeHard.isChecked && !binding.rbTypeTennis.isChecked ->
                return toast("Select tournament type")
        }

        if (startDate > endDate) {
            toast("End date must be after start date")
            return
        }

        submitUpdate()
    }

    private fun submitUpdate() {

        val dto = TournamentUpdateRequest(
            name = binding.etTournamentName.text.toString().trim(),
            username = binding.etOrganizerEmail.text.toString().trim(),
            startDate = binding.etStartDate.text.toString(),
            endDate = binding.etEndDate.text.toString(),
            seasonId = seasonId,
            sportsId = sportId,
            playerType = if (binding.rbMen.isChecked) "Male" else "Female",
            tournamentType = if (binding.rbTypeHard.isChecked) "Hard" else "Tennis",
            tournamentStage = when {
                binding.rbRoundRobin.isChecked -> "Round Robin"
                binding.rbRoundRobinKnock.isChecked -> "Round Robin + Knock Out"
                binding.rbKnockOut.isChecked -> "Knock Out"
                binding.rbLeague.isChecked ->"League"
                else -> ""
            }
        )

        showLoading(true)
        binding.btnSubmit.isEnabled = false
        binding.ivBack.isEnabled = false

        api.updateTournament(tournamentId, dto).enqueue(object : Callback<TournamentUpdateRequest> {

                override fun onResponse(
                    call: Call<TournamentUpdateRequest>,
                    response: Response<TournamentUpdateRequest>
                ) {
                    showLoading(false)
                    binding.btnSubmit.isEnabled = true
                    binding.ivBack.isEnabled = true
                    if (response.isSuccessful) finish()
                    else toastLong(NetworkUi.userMessage(response, "Update failed"))
                }

                override fun onFailure(call: Call<TournamentUpdateRequest>, t: Throwable) {
                    showLoading(false)
                    binding.btnSubmit.isEnabled = true
                    binding.ivBack.isEnabled = true
                    toastLong(NetworkUi.userMessage(t))
                }
            })
    }

    private fun toast(msg: String) {
        toastShort(msg)
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        // Add empty state logic here if needed
        // Example: if (formData.isEmpty()) { showEmptyStateView() }
    }
}
