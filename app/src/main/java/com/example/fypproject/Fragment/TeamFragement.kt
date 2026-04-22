package com.example.fypproject.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.R
import com.example.fypproject.databinding.FragmentTeamBinding
import com.google.android.material.button.MaterialButton

class TeamFragement: Fragment(R.layout.fragment_team) {
    private var _binding: FragmentTeamBinding? = null
    private val binding get() = _binding!!

    private var tournamentId: Long = -1L
    private var sportId: Long=-1L
    private lateinit var buttons: List<MaterialButton>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTeamBinding.bind(view)
        tournamentId = arguments?.getLong("tournamentId") ?: -1L
        sportId=arguments?.getLong("sportId")?: -1L

        buttons = listOf(
            binding.btnTeams,
            binding.btnMyTeam
        )

        selectButton(binding.btnMyTeam)
        loadFragment(MyTeamFragment.newInstance(tournamentId,sportId))

        binding.btnMyTeam.setOnClickListener {
            selectButton(binding.btnMyTeam)
            loadFragment(MyTeamFragment.newInstance(tournamentId,sportId))
        }

        binding.btnTeams.setOnClickListener {
            selectButton(binding.btnTeams)
            loadFragment(AllTeamsFragement.newInstance(tournamentId))
        }
    }

    private fun loadFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
        binding.fragmentContainer.postDelayed({
        }, 300)
    }

    private fun selectButton(active: MaterialButton) {
        buttons.forEach {
            it.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.DKGRAY)
        }
        active.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#E31212"))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tournamentId: Long,sportId:Long): TeamFragement {
            val fragment = TeamFragement()
            val args = Bundle()
            args.putLong("tournamentId", tournamentId)
            args.putLong("sportId",sportId)
            fragment.arguments = args
            return fragment
        }
    }
}