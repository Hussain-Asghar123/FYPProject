package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.HockeyFragment.HockeyEventsFragment
import com.example.fypproject.HockeyFragment.HockeyInfoFragment
import com.example.fypproject.HockeyFragment.HockeyScoreCardFragment
import com.example.fypproject.HockeyFragment.HockeyScoringFragment
import com.example.fypproject.ScoringDTO.HockeyScoringDto
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.databinding.ActivityHockeyScoringBinding
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class HockeyScoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHockeyScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>

    private var hockeyScoringFragment: HockeyScoringFragment? = null
    private var hockeyScoreCardFragment: HockeyScoreCardFragment? = null
    private var hockeyEventsFragment: HockeyEventsFragment? = null
    private var hockeyInfoFragment: HockeyInfoFragment? = null

    var latestScore: HockeyScoringDto? = null
        private set

    private val ACTIVITY_SOCKET_KEY = "HockeyScoringActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHockeyScoringBinding.inflate(layoutInflater)
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
            binding.btnEvents,
            binding.btnInfo
        )

        matchResponse?.let { match ->
            hockeyScoringFragment   = HockeyScoringFragment.newInstance(match)
            hockeyScoreCardFragment = HockeyScoreCardFragment.newInstance(match)
            hockeyEventsFragment    = HockeyEventsFragment.newInstance(match)
            hockeyInfoFragment      = HockeyInfoFragment.newInstance(match)
        }

        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            if (hockeyScoringFragment == null) {
                finish()
                return
            }
            showFragment(hockeyScoringFragment!!)
        } else {
            val fm = supportFragmentManager
            hockeyScoringFragment   = fm.findFragmentByTag("HockeyScoringFragment")   as? HockeyScoringFragment   ?: hockeyScoringFragment
            hockeyScoreCardFragment = fm.findFragmentByTag("HockeyScoreCardFragment") as? HockeyScoreCardFragment ?: hockeyScoreCardFragment
            hockeyEventsFragment    = fm.findFragmentByTag("HockeyEventsFragment")    as? HockeyEventsFragment    ?: hockeyEventsFragment
            hockeyInfoFragment      = fm.findFragmentByTag("HockeyInfoFragment")      as? HockeyInfoFragment      ?: hockeyInfoFragment
            selectButton(binding.btnScoring)
        }

        matchResponse?.id?.let { WebSocketManager.connect(it) }

        WebSocketManager.addMessageListener(ACTIVITY_SOCKET_KEY) { jsonString ->
            val score = runCatching {
                Gson().fromJson(jsonString, HockeyScoringDto::class.java)
            }.getOrNull() ?: return@addMessageListener

            latestScore = score

            runOnUiThread {
                hockeyScoreCardFragment?.onScoreUpdated(score)
                hockeyEventsFragment?.onScoreUpdated(score)
            }
        }

        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(hockeyScoringFragment ?: return@setOnClickListener)
        }
        binding.btnScoreCard.setOnClickListener {
            selectButton(binding.btnScoreCard)
            showFragment(hockeyScoreCardFragment ?: return@setOnClickListener)
        }
        binding.btnEvents.setOnClickListener {
            selectButton(binding.btnEvents)
            showFragment(hockeyEventsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(hockeyInfoFragment ?: return@setOnClickListener)
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