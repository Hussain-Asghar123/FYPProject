package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.CricketFragment.ScoringFragment
import com.example.fypproject.DTO.MatchDTO
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.databinding.ActivityCricketScoringBinding
import com.google.android.material.button.MaterialButton

class CricketScoringActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCricketScoringBinding
    private var matchResponse: MatchResponse? = null

    private lateinit var buttons: List<MaterialButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCricketScoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        matchResponse?.let {
            loadFragment(ScoringFragment.newInstance(it))
        }


        // CricketScoringActivity.kt mein yeh line change karo:

        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            matchResponse?.let {
                loadFragment(ScoringFragment.newInstance(it))
            }
        }

        binding.btnScoreCard.setOnClickListener {
            selectButton(binding.btnScoreCard)
            matchResponse?.let {
                // loadFragment(ScoreCardFragment.newInstance(it))
            }
        }

        binding.btnBalls.setOnClickListener {
            selectButton(binding.btnBalls)
            matchResponse?.let {
                // loadFragment(BallsFragment.newInstance(it))
            }
        }

        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            matchResponse?.let {
                // loadFragment(InfoFragment.newInstance(it))
            }
        }
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