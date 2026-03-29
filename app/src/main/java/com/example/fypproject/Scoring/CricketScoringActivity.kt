package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.CricketFragment.BallsFragment
import com.example.fypproject.CricketFragment.InfoFragment
import com.example.fypproject.CricketFragment.ScoreCardFragment
import com.example.fypproject.CricketFragment.ScoringFragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.databinding.ActivityCricketScoringBinding
import com.google.android.material.button.MaterialButton

class CricketScoringActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCricketScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>

    private var scoringFragment: ScoringFragment? = null
    private var scoreCardFragment: ScoreCardFragment? = null
    private var ballsFragment: BallsFragment? = null
    private var infoFragment: InfoFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCricketScoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScoring.text = "Summary"
        binding.btnBack.setOnClickListener { finish() }

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("match", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("match") as? MatchResponse
        }

        buttons = listOf(binding.btnScoring, binding.btnScoreCard, binding.btnBalls, binding.btnInfo)

        // ✅ Fragments hamesha banao
        matchResponse?.let { match ->
            scoringFragment   = ScoringFragment.newInstance(match)
            scoreCardFragment = ScoreCardFragment.newInstance(match)
            ballsFragment     = BallsFragment.newInstance(match)
            infoFragment      = InfoFragment.newInstance(match)
        }

        // ✅ Sirf fresh launch mein fragment add karo — recreate pe skip karo
        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            showFragment(scoringFragment ?: return)
        } else {
            // ✅ Recreate ke baad existing fragments wapas lo
            val fm = supportFragmentManager
            scoringFragment   = fm.findFragmentByTag("ScoringFragment")   as? ScoringFragment   ?: scoringFragment
            scoreCardFragment = fm.findFragmentByTag("ScoreCardFragment") as? ScoreCardFragment ?: scoreCardFragment
            ballsFragment     = fm.findFragmentByTag("BallsFragment")     as? BallsFragment     ?: ballsFragment
            infoFragment      = fm.findFragmentByTag("InfoFragment")      as? InfoFragment      ?: infoFragment
            selectButton(binding.btnScoring)
        }

        // ✅ Button listeners hamesha set honge — COMPLETED ya LIVE dono ke liye
        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(scoringFragment ?: return@setOnClickListener)
        }
        binding.btnScoreCard.setOnClickListener {
            selectButton(binding.btnScoreCard)
            showFragment(scoreCardFragment ?: return@setOnClickListener)
        }
        binding.btnBalls.setOnClickListener {
            selectButton(binding.btnBalls)
            showFragment(ballsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(infoFragment ?: return@setOnClickListener)
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
        }.commitAllowingStateLoss() // ✅ crash se bachao
    }

    private fun selectButton(active: MaterialButton) {
        buttons.forEach {
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.DKGRAY)
        }
        active.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#E31212"))
    }
}