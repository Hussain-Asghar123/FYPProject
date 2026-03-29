package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.FutsalFragment.FutsalScoringFragment
import com.example.fypproject.databinding.ActivityFutsalScoringBinding
import com.google.android.material.button.MaterialButton

class FutsalScoringActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFutsalScoringBinding
    private var matchResponse: MatchResponse? = null

    private var futsalScoringFragment: FutsalScoringFragment? = null

    private lateinit var buttons: List<MaterialButton>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFutsalScoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val role = intent.getStringExtra("role")
            ?: getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("role", "USER")
            ?: "USER"

        val isViewerOnly = role.uppercase() == "USER"
        if (isViewerOnly) {
            binding.btnScoring.text = "Summary"
        }

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("match", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("match") as? MatchResponse
        }

        binding.btnBack.setOnClickListener { finish() }

        buttons = listOf(
            binding.btnScoring,
            binding.btnScoreCard,
            binding.btnBalls,
            binding.btnInfo
        )
        selectButton(binding.btnScoring)

        matchResponse?.let { match ->
            futsalScoringFragment = FutsalScoringFragment.newInstance(match)
        }

        selectButton(binding.btnScoring)
        showFragment(futsalScoringFragment ?: return)


        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(futsalScoringFragment ?: return@setOnClickListener)
        }

    }

    private fun showFragment(fragment: Fragment) {
        val fm = supportFragmentManager
        val tag = fragment::class.java.simpleName

        fm.beginTransaction().apply {
            fm.fragments.forEach { hide(it) }

            if (fm.findFragmentByTag(tag) == null) {
                add(binding.fragmentContainer.id, fragment, tag)
            } else {
                show(fragment)
            }
        }.commit()
    }

    private fun selectButton(active: MaterialButton) {
        buttons.forEach {
            it.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.DKGRAY)
        }
        active.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#E31212"))
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}