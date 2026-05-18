package com.example.fypproject.BottomSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.Utils.toastShort
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch


class PlayerInfoBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_PLAYER_ID = "player_id"

        fun newInstance(playerId: Long): PlayerInfoBottomSheetFragment {
            return PlayerInfoBottomSheetFragment().apply {
                arguments = Bundle().apply { putLong(ARG_PLAYER_ID, playerId) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_player_info, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val playerId = requireArguments().getLong(ARG_PLAYER_ID)

        val progressBar    = view.findViewById<View>(R.id.progressPlayerInfo)
        val contentLayout  = view.findViewById<View>(R.id.layoutPlayerInfoContent)
        val errorText      = view.findViewById<TextView>(R.id.tvPlayerInfoError)
        val ivPhoto        = view.findViewById<CircleImageView>(R.id.ivPlayerInfoPhoto)
        val tvInitial      = view.findViewById<TextView>(R.id.tvPlayerInfoInitial)
        val tvName         = view.findViewById<TextView>(R.id.tvPlayerInfoName)
        val tvJersey       = view.findViewById<TextView>(R.id.tvPlayerInfoJersey)
        val tvSports       = view.findViewById<TextView>(R.id.tvPlayerInfoSports)
        val rvTeams        = view.findViewById<RecyclerView>(R.id.rvPlayerInfoTeams)
        val rvTournaments  = view.findViewById<RecyclerView>(R.id.rvPlayerInfoTournaments)
        val tvMatches      = view.findViewById<TextView>(R.id.tvPlayerInfoMatches)
        val tvTeamsHeader  = view.findViewById<TextView>(R.id.tvTeamsHeader)
        val tvTournsHeader = view.findViewById<TextView>(R.id.tvTournamentsHeader)

        progressBar.visibility   = View.VISIBLE
        contentLayout.visibility = View.GONE
        errorText.visibility     = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitInstance.api.getPlayerInfo(playerId)
                if (res.isSuccessful && res.body() != null) {
                    val info = res.body()!!

                    // Photo
                    if (!info.profilePhotoUrl.isNullOrBlank()) {
                        Glide.with(requireContext())
                            .load(info.profilePhotoUrl)
                            .into(ivPhoto)
                        ivPhoto.visibility  = View.VISIBLE
                        tvInitial.visibility = View.GONE
                    } else {
                        ivPhoto.visibility  = View.GONE
                        tvInitial.text      = info.playerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                        tvInitial.visibility = View.VISIBLE
                    }

                    tvName.text    = info.playerName
                    tvJersey.text  = "Jersey #${info.jerseyNumber ?: "N/A"}"
                    tvSports.text  = "Sports: ${info.sports?.joinToString(", ") ?: "None"}"
                    tvMatches.text = "${info.totalMatchesPlayed}"

                    // Teams
                    tvTeamsHeader.text = "TEAMS (${info.teams?.size ?: 0})"
                    rvTeams.layoutManager = LinearLayoutManager(requireContext())
                    rvTeams.adapter = PlayerTeamAdapter(info.teams ?: emptyList())

                    // Tournaments
                    tvTournsHeader.text = "TOURNAMENTS (${info.tournaments?.size ?: 0})"
                    rvTournaments.layoutManager = LinearLayoutManager(requireContext())
                    rvTournaments.adapter = PlayerTournamentAdapter(info.tournaments ?: emptyList())

                    progressBar.visibility   = View.GONE
                    contentLayout.visibility = View.VISIBLE
                } else {
                    showError(progressBar, errorText)
                }
            } catch (e: Exception) {
                showError(progressBar, errorText)
            }
        }
    }

    private fun showError(progress: View, errorText: TextView) {
        progress.visibility   = View.GONE
        errorText.visibility  = View.VISIBLE
        errorText.text        = "Failed to load player info."
    }

    // ── Inline adapters ────────────────────────────────────────────────────────

    inner class PlayerTeamAdapter(
        private val teams: List<com.example.fypproject.DTO.PlayerTeamInfo>
    ) : RecyclerView.Adapter<PlayerTeamAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTeamName:       TextView = v.findViewById(R.id.tvPiTeamName)
            val tvTournamentName: TextView = v.findViewById(R.id.tvPiTeamTournament)
            val tvSport:          TextView = v.findViewById(R.id.tvPiTeamSport)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_player_info_team, parent, false)
        )

        override fun getItemCount() = teams.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = teams[position]
            holder.tvTeamName.text       = t.teamName
            holder.tvTournamentName.text = t.tournamentName
            holder.tvSport.text          = t.sport
        }
    }

    inner class PlayerTournamentAdapter(
        private val tournaments: List<com.example.fypproject.DTO.PlayerTournamentInfo>
    ) : RecyclerView.Adapter<PlayerTournamentAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName:   TextView = v.findViewById(R.id.tvPiTournamentName)
            val tvStatus: TextView = v.findViewById(R.id.tvPiTournamentStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_player_info_tournament, parent, false)
        )

        override fun getItemCount() = tournaments.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = tournaments[position]
            holder.tvName.text   = t.name
            holder.tvStatus.text = t.status
            holder.tvStatus.setBackgroundResource(
                if (t.status == "ONGOING") R.drawable.badge_status
                else R.drawable.bg_gray_rounded
            )
        }
    }
}