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
import com.example.fypproject.ScoringDTO.FutsalScoreDTO
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.databinding.ActivityFutsalScoringBinding
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class FutsalScoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFutsalScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>

    private var futsalScoringFragment: FutsalScoringFragment? = null
    private var futsalScoreCardFragment: FutsalScoreCardFragment? = null
    private var futsalHighLightsFragment: FutsalHighLightsFragment? = null
    private var futsalInfoFragment: FutsalInfoFragment? = null

    var latestScore: FutsalScoreDTO? = null
        private set

    private val ACTIVITY_SOCKET_KEY = "FutsalScoringActivity"

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
            futsalInfoFragment       = FutsalInfoFragment.newInstance(match)
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
            futsalInfoFragment       = fm.findFragmentByTag("FutsalEventsFragment")     as? FutsalInfoFragment       ?: futsalInfoFragment
            selectButton(binding.btnScoring)
        }

        matchResponse?.id?.let { WebSocketManager.connect(it) }

        WebSocketManager.addMessageListener(ACTIVITY_SOCKET_KEY) { jsonString ->
            val score = runCatching {
                Gson().fromJson(jsonString, FutsalScoreDTO::class.java)
            }.getOrNull() ?: return@addMessageListener

            latestScore = score

            runOnUiThread {
                futsalScoreCardFragment?.onScoreUpdated(score)
                futsalHighLightsFragment?.onScoreUpdated(score)
            }
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

    override fun onResume() {
        super.onResume()
        matchResponse?.id?.let { WebSocketManager.connect(it) }
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations) {
            WebSocketManager.disconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.removeMessageListener(ACTIVITY_SOCKET_KEY)
        WebSocketManager.disconnect()
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