package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.fypproject.BadmintionFragment.BadmintionHighLightsFragment
import com.example.fypproject.BadmintionFragment.BadmintionInfoFragment
import com.example.fypproject.BadmintionFragment.BadmintionScoreCardFragment
import com.example.fypproject.BadmintionFragment.BadmintionScoringFragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.databinding.ActivityBadmintionScoringBinding
import com.example.fypproject.databinding.ActivityTableTennisScoringBinding
import com.google.android.material.button.MaterialButton

class BadmintionScoringActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBadmintionScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>

    private var badmintonScoringFragment: BadmintionScoringFragment? = null
    private var badmintonScoreCardFragment: BadmintionScoreCardFragment? = null
    private var badmintonHighLightsFragment: BadmintionHighLightsFragment? = null
    private var badmintonInfoFragment: BadmintionInfoFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBadmintionScoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val role = intent.getStringExtra("role")
            ?: getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("role", "USER")
            ?: "USER"

        if (role.uppercase() == "USER") {
            binding.btnScoring.text = "Summary"
        }

        binding.btnBack.setOnClickListener { finish() }

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("match", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("match") as? MatchResponse
        }

        buttons = listOf(
            binding.btnScoring,
            binding.btnScoreCard,
            binding.btnBalls,
            binding.btnInfo
        )
        matchResponse?.let { match ->
            badmintonScoringFragment = BadmintionScoringFragment.newInstance(match)
            badmintonScoreCardFragment = BadmintionScoreCardFragment.newInstance(match)
            badmintonHighLightsFragment = BadmintionHighLightsFragment.newInstance(match)
            badmintonInfoFragment = BadmintionInfoFragment.newInstance(match)
        }
        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            if (badmintonScoringFragment == null) {
                finish()
                return
            }
            showFragment(badmintonScoringFragment!!)
        } else {
            val fm = supportFragmentManager
            badmintonScoringFragment = fm.findFragmentByTag("scoring") as? BadmintionScoringFragment
                ?: badmintonScoringFragment
            badmintonScoreCardFragment =
                fm.findFragmentByTag("scorecard") as? BadmintionScoreCardFragment
                    ?: badmintonScoreCardFragment
            badmintonHighLightsFragment =
                fm.findFragmentByTag("highlights") as? BadmintionHighLightsFragment
                    ?: badmintonHighLightsFragment
            badmintonInfoFragment =
                fm.findFragmentByTag("info") as? BadmintionInfoFragment ?: badmintonInfoFragment
            selectButton(binding.btnScoring)
        }
        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(badmintonScoringFragment ?: return@setOnClickListener)
        }
        binding.btnScoreCard.setOnClickListener {
            selectButton(binding.btnScoreCard)
            showFragment(badmintonScoreCardFragment ?: return@setOnClickListener)
        }
        binding.btnBalls.setOnClickListener {
            selectButton(binding.btnBalls)
            showFragment(badmintonHighLightsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(badmintonInfoFragment ?: return@setOnClickListener)
        }
    }
    private fun showFragment(fragment: Fragment) {
        val fm  = supportFragmentManager
        val tag = fragment::class.java.simpleName
        val existing = fm.findFragmentByTag(tag)

        fm.beginTransaction().apply {
            fm.fragments.forEach { hide(it) }
            if (existing == null) {
                add(binding.fragmentContainer.id, fragment, tag)
            } else {
                show(existing)
            }
        }.commitAllowingStateLoss()
    }

    private fun selectButton(active: MaterialButton) {
        buttons.forEach {
            it.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.DKGRAY)
        }
        active.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#E31212"))
    }
}