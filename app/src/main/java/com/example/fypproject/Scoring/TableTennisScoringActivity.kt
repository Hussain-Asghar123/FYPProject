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
import com.example.fypproject.TableTennisFragment.TableTennisHighLightsFragment
import com.example.fypproject.TableTennisFragment.TableTennisInfoFragment
import com.example.fypproject.TableTennisFragment.TableTennisScoreCardFragment
import com.example.fypproject.TableTennisFragment.TableTennisScoringFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallHighLightsFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallInfoFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallScoreCardFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallScoringFragment
import com.example.fypproject.databinding.ActivityTableTennisScoringBinding
import com.example.fypproject.databinding.ActivityVolleyBallScoringBinding
import com.google.android.material.button.MaterialButton
import kotlin.collections.forEach

class TableTennisScoringActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTableTennisScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>

    private var tableTennisScoringFragment: TableTennisScoringFragment? = null
    private var tableTennisScoreCardFragment: TableTennisScoreCardFragment? = null
    private var tableTennisHighLightsFragment: TableTennisHighLightsFragment? = null
    private var tableTennisInfoFragment: TableTennisInfoFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTableTennisScoringBinding.inflate(layoutInflater)
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
            tableTennisScoringFragment    = TableTennisScoringFragment.newInstance(match)
            tableTennisScoreCardFragment  = TableTennisScoreCardFragment.newInstance(match)
            tableTennisHighLightsFragment = TableTennisHighLightsFragment.newInstance(match)
            tableTennisInfoFragment       = TableTennisInfoFragment.newInstance(match)
        }

        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            if (tableTennisScoringFragment == null) {
                finish()
                return
            }
            showFragment(tableTennisScoringFragment!!)
        } else {
            val fm = supportFragmentManager
            tableTennisScoringFragment    = fm.findFragmentByTag("TableTennisScoringFragment")    as? TableTennisScoringFragment    ?: tableTennisScoringFragment
            tableTennisScoreCardFragment  = fm.findFragmentByTag("TableTennisScoreCardFragment")  as? TableTennisScoreCardFragment  ?: tableTennisScoreCardFragment
            tableTennisHighLightsFragment = fm.findFragmentByTag("TableTennisHighLightsFragment") as? TableTennisHighLightsFragment ?: tableTennisHighLightsFragment
            tableTennisInfoFragment       = fm.findFragmentByTag("TableTennisInfoFragment")       as? TableTennisInfoFragment       ?: tableTennisInfoFragment
            selectButton(binding.btnScoring)
        }

        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(tableTennisScoringFragment ?: return@setOnClickListener)
        }
        binding.btnScoreCard.setOnClickListener {
            selectButton(binding.btnScoreCard)
            showFragment(tableTennisScoreCardFragment ?: return@setOnClickListener)
        }
        binding.btnBalls.setOnClickListener {
            selectButton(binding.btnBalls)
            showFragment(tableTennisHighLightsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(tableTennisInfoFragment ?: return@setOnClickListener)
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