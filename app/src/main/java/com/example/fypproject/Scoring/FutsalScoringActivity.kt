package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.FutsalFragment.FutsalScoringFragment
import com.example.fypproject.FutsalFragment.FutsalScoreCardFragment
import com.example.fypproject.FutsalFragment.FutsalHighLightsFragment
import com.example.fypproject.FutsalFragment.FutsalInfoFragment
import com.example.fypproject.databinding.ActivityFutsalScoringBinding
import com.google.android.material.button.MaterialButton

class FutsalScoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFutsalScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>

    private var futsalScoringFragment: FutsalScoringFragment? = null
    private var futsalScoreCardFragment: FutsalScoreCardFragment? = null
    private var futsalHighLightsFragment: FutsalHighLightsFragment? = null
    private var futsalInfoFragment: FutsalInfoFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFutsalScoringBinding.inflate(layoutInflater)
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
            futsalScoringFragment    = FutsalScoringFragment.newInstance(match)
            futsalScoreCardFragment  = FutsalScoreCardFragment.newInstance(match)
            futsalHighLightsFragment = FutsalHighLightsFragment.newInstance(match)
            futsalInfoFragment     =   FutsalInfoFragment.newInstance(match)
        }

        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            if (futsalScoringFragment == null) {
                finish()
                return
            }
            showFragment(futsalScoringFragment!!)
        } else {
            val fm = supportFragmentManager
            futsalScoringFragment    = fm.findFragmentByTag("FutsalScoringFragment")    as? FutsalScoringFragment    ?: futsalScoringFragment
            futsalScoreCardFragment  = fm.findFragmentByTag("FutsalScoreCardFragment")  as? FutsalScoreCardFragment  ?: futsalScoreCardFragment
            futsalHighLightsFragment = fm.findFragmentByTag("FutsalHighLightsFragment") as? FutsalHighLightsFragment ?: futsalHighLightsFragment
            futsalInfoFragment     = fm.findFragmentByTag("FutsalEventsFragment")     as? FutsalInfoFragment     ?: futsalInfoFragment
            selectButton(binding.btnScoring)
        }

        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(futsalScoringFragment ?: return@setOnClickListener)
        }
        binding.btnScoreCard.setOnClickListener {
            selectButton(binding.btnScoreCard)
            showFragment(futsalScoreCardFragment ?: return@setOnClickListener)
        }
        binding.btnBalls.setOnClickListener {
            selectButton(binding.btnBalls)
            showFragment(futsalHighLightsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(futsalInfoFragment ?: return@setOnClickListener)
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