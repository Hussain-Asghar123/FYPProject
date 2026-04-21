package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.LudoFragment.LudoHighlightsFragment
import com.example.fypproject.LudoFragment.LudoInfoFragment
import com.example.fypproject.LudoFragment.LudoScoringFragment
import com.example.fypproject.ScoringDTO.LudoScoreDTO
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.databinding.ActivityLudoScoringBinding
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class LudoScoringActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLudoScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>
    var latestScore: LudoScoreDTO? = null
        private set
    private val ACTIVITY_SOCKET_KEY = "LudoScoringActivity"

    private var ludoScoringFragment: LudoScoringFragment? = null
    private var ludoHighlightsFragment: LudoHighlightsFragment? = null
    private var ludoInfoFragment: LudoInfoFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLudoScoringBinding.inflate(layoutInflater)
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
            ludoScoringFragment    = LudoScoringFragment.newInstance(match)
            ludoHighlightsFragment = LudoHighlightsFragment.newInstance(match)
            ludoInfoFragment       = LudoInfoFragment.newInstance(match)
        }

        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            if (ludoScoringFragment == null) {
                finish()
                return
            }
            showFragment(ludoScoringFragment!!)
        } else {
            val fm = supportFragmentManager
            ludoScoringFragment    = fm.findFragmentByTag("LudoScoringFragment")    as? LudoScoringFragment    ?: ludoScoringFragment
            ludoHighlightsFragment = fm.findFragmentByTag("LudoHighlightsFragment") as? LudoHighlightsFragment ?: ludoHighlightsFragment
            ludoInfoFragment       = fm.findFragmentByTag("LudoInfoFragment")       as? LudoInfoFragment       ?: ludoInfoFragment
            selectButton(binding.btnScoring)
        }
        matchResponse?.id?.let { WebSocketManager.connect(it) }
        WebSocketManager.addMessageListener(ACTIVITY_SOCKET_KEY) { jsonString ->
            val score = runCatching {
                Gson().fromJson(jsonString, LudoScoreDTO::class.java)
            }.getOrNull() ?: return@addMessageListener
            latestScore = score
            runOnUiThread {
                ludoHighlightsFragment?.onScoreUpdated(score)
            }
        }
        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(ludoScoringFragment ?: return@setOnClickListener)
        }
        binding.btnHighlights.setOnClickListener {
            selectButton(binding.btnHighlights)
            showFragment(ludoHighlightsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(ludoInfoFragment ?: return@setOnClickListener)
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
        WebSocketManager.disconnect()
        WebSocketManager.removeMessageListener(ACTIVITY_SOCKET_KEY)
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