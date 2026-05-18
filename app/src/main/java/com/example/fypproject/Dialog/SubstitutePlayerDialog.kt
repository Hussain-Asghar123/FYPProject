package com.example.fypproject.Dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.fypproject.DTO.SubstituteRequest
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubstitutePlayerDialog : DialogFragment() {

    interface OnSubstituteSuccess {
        fun onSuccess(updatedScore: com.example.fypproject.ScoringDTO.ScoreDTO?)
    }

    companion object {
        fun newInstance(
            matchId: Long, inningsId: Long?,
            team1Id: Long, team2Id: Long,
            team1Name: String, team2Name: String,
            battingTeamId: Long,
            availableBatters: List<TeamPlayerDto>,   // bench batters (WS)
            availableBowlers: List<TeamPlayerDto>,   // bench bowlers (WS)
            activeBatterIds: List<Long>,             // striker + non-striker
            activeBowlerIds: List<Long>              // current bowler
        ): SubstitutePlayerDialog {
            val frag = SubstitutePlayerDialog()
            frag.arguments = Bundle().apply {
                putLong("matchId",       matchId)
                putLong("inningsId",     inningsId ?: -1L)
                putLong("team1Id",       team1Id)
                putLong("team2Id",       team2Id)
                putString("team1Name",   team1Name)
                putString("team2Name",   team2Name)
                putLong("battingTeamId", battingTeamId)
                putLongArray("batterIds", activeBatterIds.toLongArray())
                putLongArray("bowlerIds", activeBowlerIds.toLongArray())
            }
            return frag
        }
    }

    var onSuccess: OnSubstituteSuccess? = null

    private val RED   = Color.parseColor("#E31212")
    private val GREY  = Color.parseColor("#6B7280")
    private val WHITE = Color.WHITE

    // Full squads fetched from API
    private var battingSquad: List<TeamPlayerDto> = emptyList()
    private var bowlingSquad: List<TeamPlayerDto> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_substitute_player, null)

        val args          = requireArguments()
        val matchId       = args.getLong("matchId")
        val inningsId     = args.getLong("inningsId").takeIf { it != -1L }
        val team1Id       = args.getLong("team1Id")
        val team2Id       = args.getLong("team2Id")
        val battingTeamId = args.getLong("battingTeamId")
        val bowlingTeamId = if (battingTeamId == team1Id) team2Id else team1Id

        val batterIds = args.getLongArray("batterIds")?.toHashSet() ?: hashSetOf()
        val bowlerIds = args.getLongArray("bowlerIds")?.toHashSet() ?: hashSetOf()

        val btnBatting  = view.findViewById<Button>(R.id.btnSubBatting)
        val btnBowling  = view.findViewById<Button>(R.id.btnSubBowling)
        val spinnerOut  = view.findViewById<Spinner>(R.id.spinnerPlayerOut)
        val spinnerIn   = view.findViewById<Spinner>(R.id.spinnerPlayerIn)
        val btnConfirm  = view.findViewById<Button>(R.id.btnConfirmSub)
        val btnCancel   = view.findViewById<Button>(R.id.btnCancelSub)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressSubstitute)

        var subTeam = "batting"

        // ── Helper: spinner adapter with hint at position 0 ───────────────────
        fun makeAdapter(hint: String, names: List<String>): ArrayAdapter<String> =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listOf(hint) + names
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // ── Populate spinners ─────────────────────────────────────────────────
        fun populate() {
            val squad   = if (subTeam == "batting") battingSquad else bowlingSquad
            val active  = if (subTeam == "batting") batterIds    else bowlerIds

            // OUT = active players on field
            val outList = squad.filter { active.contains(it.id) }

            // IN  = bench players (not active)
            val inList  = squad.filter { !active.contains(it.id) }

            spinnerOut.adapter = makeAdapter("Select player to remove…", outList.map { it.name ?: "?" })
            spinnerIn.adapter  = makeAdapter("Select substitute player…", inList.map { it.name ?: "?" })

            spinnerOut.tag = outList
            spinnerIn.tag  = inList
        }

        // ── Tab toggle ────────────────────────────────────────────────────────
        fun selectTab(isBatting: Boolean) {
            subTeam = if (isBatting) "batting" else "bowling"
            listOf(btnBatting to isBatting, btnBowling to !isBatting).forEach { (btn, active) ->
                btn.setBackgroundColor(if (active) RED else GREY)
                btn.setTextColor(WHITE)
            }
            if (battingSquad.isNotEmpty() || bowlingSquad.isNotEmpty()) populate()
        }

        btnBatting.setOnClickListener { selectTab(true) }
        btnBowling.setOnClickListener { selectTab(false) }
        selectTab(true)

        // ── Fetch full squads from API ─────────────────────────────────────────
        progressBar?.visibility = View.VISIBLE
        btnConfirm.isEnabled    = false

        lifecycleScope.launch {
            try {
                val resp1 = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getPlayersByTeam(battingTeamId)
                }
                val resp2 = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getPlayersByTeam(bowlingTeamId)
                }

                battingSquad = if (resp1.isSuccessful) resp1.body() ?: emptyList() else emptyList()
                bowlingSquad = if (resp2.isSuccessful) resp2.body() ?: emptyList() else emptyList()

                progressBar?.visibility = View.GONE
                btnConfirm.isEnabled    = true
                populate()  // ab data hai to spinners fill karo

            } catch (e: Exception) {
                progressBar?.visibility = View.GONE
                toastLong("Failed to load players: ${e.message}")
            }
        }

        // ── Confirm ───────────────────────────────────────────────────────────
        btnConfirm.setOnClickListener {
            @Suppress("UNCHECKED_CAST")
            val outList = spinnerOut.tag as? List<TeamPlayerDto> ?: return@setOnClickListener
            @Suppress("UNCHECKED_CAST")
            val inList  = spinnerIn.tag  as? List<TeamPlayerDto> ?: return@setOnClickListener

            // position 0 hint, so -1
            val outPos = spinnerOut.selectedItemPosition - 1
            val inPos  = spinnerIn.selectedItemPosition  - 1

            if (outPos < 0 || inPos < 0) {
                toastShort("Please select both players")
                return@setOnClickListener
            }

            if (outList.isEmpty()) {
                toastShort("No active player to remove")
                return@setOnClickListener
            }
            if (inList.isEmpty()) {
                toastShort("No substitute player available")
                return@setOnClickListener
            }

            val outPlayerId = outList[outPos].id ?: return@setOnClickListener
            val inPlayerId  = inList[inPos].id   ?: return@setOnClickListener
            val teamId      = if (subTeam == "batting") battingTeamId else bowlingTeamId

            btnConfirm.isEnabled = false

            lifecycleScope.launch {
                try {
                    val res = RetrofitInstance.api.substitutePlayer(
                        matchId,
                        SubstituteRequest(inningsId, outPlayerId, inPlayerId, teamId)
                    )
                    if (res.isSuccessful) {
                        toastShort("Player substituted successfully")
                        val updatedScore = res.body()
                        onSuccess?.onSuccess(updatedScore)
                        dismissAllowingStateLoss()
                    } else {
                        toastLong("Failed: ${res.errorBody()?.string() ?: "Unknown error"}")
                        btnConfirm.isEnabled = true
                    }
                } catch (e: Exception) {
                    toastLong("Error: ${e.message}")
                    btnConfirm.isEnabled = true
                }
            }
        }

        btnCancel.setOnClickListener { dismissAllowingStateLoss() }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
            .also { it.window?.setBackgroundDrawableResource(android.R.color.transparent) }
    }
}