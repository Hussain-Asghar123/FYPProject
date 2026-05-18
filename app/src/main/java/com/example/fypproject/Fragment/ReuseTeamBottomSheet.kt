package com.example.fypproject.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.ReuseTeamRequest
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Utils.toastShort
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ReuseTeamBottomSheet : BottomSheetDialogFragment() {

    private var tournamentId: Long = -1L
    private var onSuccess: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.bottom_sheet_reuse_team, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tournamentId = arguments?.getLong("tournamentId") ?: -1L

        val prefs = requireContext()
            .getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)
        val playerId = prefs.getLong("playerId", -1L)

        val listContainer  = view.findViewById<LinearLayout>(R.id.llHistoryList)
        val btnConfirm     = view.findViewById<MaterialButton>(R.id.btnConfirmReuse)
        val btnBack        = view.findViewById<MaterialButton>(R.id.btnBackReuse)
        val tvConfirmInfo  = view.findViewById<TextView>(R.id.tvConfirmInfo)
        val progressBar    = view.findViewById<ProgressBar>(R.id.progressReuse)
        val tvEmpty        = view.findViewById<TextView>(R.id.tvReuseEmpty)
        val layoutConfirm  = view.findViewById<View>(R.id.layoutConfirm)
        val layoutList     = view.findViewById<View>(R.id.layoutList)

        var selectedTeamId: Long? = null

        // Load history
        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = api.getPlayerTeamHistory(playerId)
                progressBar.visibility = View.GONE
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val history = res.body()!!
                    layoutList.visibility = View.VISIBLE
                    history.forEach { team ->
                        val item = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_team_history, listContainer, false)
                        item.findViewById<TextView>(R.id.tvHistoryTeamName).text = team.teamName
                        item.findViewById<TextView>(R.id.tvHistoryTournament).text = team.tournamentName
                        item.findViewById<TextView>(R.id.tvHistorySport).text = team.sport
                        item.findViewById<TextView>(R.id.tvHistoryCount).text = "${team.playerCount} members"
                        item.setOnClickListener {
                            selectedTeamId = team.teamId
                            tvConfirmInfo.text =
                                "${team.teamName}\n${team.tournamentName}\n${team.playerCount} players will be invited"
                            layoutList.visibility    = View.GONE
                            layoutConfirm.visibility = View.VISIBLE
                        }
                        listContainer.addView(item)
                    }
                } else {
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            }
        }

        btnBack.setOnClickListener {
            layoutConfirm.visibility = View.GONE
            layoutList.visibility    = View.VISIBLE
        }

        btnConfirm.setOnClickListener {
            val teamId = selectedTeamId ?: return@setOnClickListener
            btnConfirm.isEnabled = false
            btnConfirm.text = "Reusing..."

            val prefs2 = requireContext()
                .getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val res = api.reuseTeam(
                        ReuseTeamRequest(
                            sourceTeamId      = teamId,
                            targetTournamentId = tournamentId,
                            creatorPlayerId   = prefs2.getLong("playerId", -1L)
                        )
                    )
                    if (res.isSuccessful) {
                        val sent = res.body()?.invitesSent ?: 0
                        toastShort("Team reused! $sent players invited.")
                        onSuccess?.invoke()
                        dismiss()
                    } else {
                        toastShort("Failed to reuse team")
                        btnConfirm.isEnabled = true
                        btnConfirm.text = "Confirm"
                    }
                } catch (e: Exception) {
                    toastShort(e.message ?: "Error")
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Confirm"
                }
            }
        }
    }

    companion object {
        fun newInstance(tournamentId: Long, onSuccess: () -> Unit) =
            ReuseTeamBottomSheet().apply {
                arguments = Bundle().apply { putLong("tournamentId", tournamentId) }
                this.onSuccess = onSuccess
            }
    }
}