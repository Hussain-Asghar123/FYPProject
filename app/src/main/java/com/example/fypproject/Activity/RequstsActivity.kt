package com.example.fypproject.Activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.RequestAdapter
import com.example.fypproject.DTO.PlayerRequestDto
import com.example.fypproject.DTO.TeamRequestDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.databinding.ActivityRequstsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RequstsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequstsBinding
    private lateinit var playerAdapter: RequestAdapter<PlayerRequestDto>
    private lateinit var teamAdapter: RequestAdapter<TeamRequestDto>

    private val playerRequests = mutableListOf<PlayerRequestDto>()
    private val teamRequests = mutableListOf<TeamRequestDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequstsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val role = prefs.getString("role", "") ?: ""
        val playerId = prefs.getLong("playerId", -1L)

        setupAdapters()
        setupRecyclerView()
        fetchRequests(role, playerId)  // same as useEffect on mount

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupAdapters() {
        playerAdapter = RequestAdapter(
            playerRequests,
            onApprove = { item, pos -> handleAction(item, "APPROVE", pos) },
            onReject  = { item, pos -> handleAction(item, "REJECT",  pos) }
        )
        teamAdapter = RequestAdapter(
            teamRequests,
            onApprove = { item, pos -> handleAction(item, "APPROVE", pos) },
            onReject  = { item, pos -> handleAction(item, "REJECT",  pos) }
        )
    }

    private fun setupRecyclerView() {
        binding.requestsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchRequests(role: String, userId: Long) {
        showLoading(true)

        when (role) {
            "ADMIN" -> {
                api.getAllTeamRequests().enqueue(object : Callback<List<TeamRequestDto>> {
                    override fun onResponse(
                        call: Call<List<TeamRequestDto>>,
                        response: Response<List<TeamRequestDto>>
                    ) {
                        showLoading(false)
                        val data = response.body() ?: emptyList()
                        teamRequests.clear()
                        teamRequests.addAll(data)
                        binding.requestsRecyclerView.adapter = teamAdapter
                        teamAdapter.notifyDataSetChanged()
                        checkEmptyState()
                    }
                    override fun onFailure(call: Call<List<TeamRequestDto>>, t: Throwable) {
                        showLoading(false)
                        showError("Failed to load team requests.")
                    }
                })
            }

            else -> {
                api.getPlayerRequests(userId).enqueue(object : Callback<List<PlayerRequestDto>> {
                    override fun onResponse(
                        call: Call<List<PlayerRequestDto>>,
                        response: Response<List<PlayerRequestDto>>
                    ) {
                        showLoading(false)
                        val data = response.body() ?: emptyList()
                        playerRequests.clear()
                        playerRequests.addAll(data)
                        binding.requestsRecyclerView.adapter = playerAdapter
                        playerAdapter.notifyDataSetChanged()
                        checkEmptyState()
                    }
                    override fun onFailure(call: Call<List<PlayerRequestDto>>, t: Throwable) {
                        showLoading(false)
                        showError("Failed to load requests.")
                    }
                })
            }
        }
    }

    private fun <T> handleAction(item: T, action: String, position: Int) {
        val id = when (item) {
            is PlayerRequestDto -> item.requestId
            is TeamRequestDto   -> item.requestId
            else -> return
        }

        val call = when (item) {
            is PlayerRequestDto -> if (action == "APPROVE") api.approvePlayerRequest(id) else api.rejectPlayerRequest(id)
            is TeamRequestDto   -> if (action == "APPROVE") api.approveTeamRequest(id)   else api.rejectTeamRequest(id)
            else -> return
        }

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    when (item) {
                        is PlayerRequestDto -> { playerRequests.removeAt(position); playerAdapter.notifyItemRemoved(position) }
                        is TeamRequestDto   -> { teamRequests.removeAt(position);   teamAdapter.notifyItemRemoved(position) }
                    }
                    checkEmptyState()
                    val msg = if (action == "APPROVE") "Request approved!" else "Request rejected."
                    Toast.makeText(this@RequstsActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                showError("Action failed.")  // same as toast.error
            }
        })
    }

    private fun checkEmptyState() {
        val isEmpty = playerRequests.isEmpty() && teamRequests.isEmpty()
        binding.emptyRequestsView.visibility  = if (isEmpty) View.VISIBLE else View.GONE
        binding.requestsRecyclerView.visibility = if (isEmpty) View.GONE   else View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}