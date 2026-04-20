package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.TugOfWarFragment.TugOfWarHighlightsFragment
import com.example.fypproject.TugOfWarFragment.TugOfWarInfoFragment
import com.example.fypproject.TugOfWarFragment.TugOfWarScoringFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallHighLightsFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallInfoFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallScoreCardFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallScoringFragment
import com.example.fypproject.databinding.ActivityTugOfWarScoringBinding
import com.example.fypproject.databinding.ActivityVolleyBallScoringBinding
import com.google.android.material.button.MaterialButton

class TugOfWarScoringActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTugOfWarScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>

    private var tugOfWarScoringFragment: TugOfWarScoringFragment? = null
    private var tugOfWarHighLightsFragment: TugOfWarHighlightsFragment? = null
    private var tugOfWarInfoFragment: TugOfWarInfoFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTugOfWarScoringBinding.inflate(layoutInflater)
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
            binding.btnHighlights,
            binding.btnInfo
        )

        matchResponse?.let { match ->
            tugOfWarScoringFragment    = TugOfWarScoringFragment.newInstance(match)
            tugOfWarHighLightsFragment = TugOfWarHighlightsFragment.newInstance(match)
            tugOfWarInfoFragment       = TugOfWarInfoFragment.newInstance(match)
        }

        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            if (tugOfWarScoringFragment == null) {
                finish()
                return
            }
            showFragment(tugOfWarScoringFragment!!)
        } else {
            val fm = supportFragmentManager
            tugOfWarScoringFragment    = fm.findFragmentByTag("TugOfWarScoringFragment")    as? TugOfWarScoringFragment    ?: tugOfWarScoringFragment
            tugOfWarHighLightsFragment = fm.findFragmentByTag("TugOfWarHighlightsFragment") as? TugOfWarHighlightsFragment ?: tugOfWarHighLightsFragment
            tugOfWarInfoFragment       = fm.findFragmentByTag("TugOfWarInfoFragment")       as? TugOfWarInfoFragment       ?: tugOfWarInfoFragment
            selectButton(binding.btnScoring)
        }

        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(tugOfWarScoringFragment ?: return@setOnClickListener)
        }
        binding.btnHighlights.setOnClickListener {
            selectButton(binding.btnHighlights)
            showFragment(tugOfWarHighLightsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(tugOfWarInfoFragment ?: return@setOnClickListener)
        }
    }

    private fun showFragment(fragment: Fragment) {
        val fm = supportFragmentManager
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