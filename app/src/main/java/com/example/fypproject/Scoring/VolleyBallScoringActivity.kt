package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.ScoringDTO.VollayBallScoreDTO
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.VolleyBallFragment.VolleyBallHighLightsFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallInfoFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallScoreCardFragment
import com.example.fypproject.VolleyBallFragment.VolleyBallScoringFragment
import com.example.fypproject.databinding.ActivityVolleyBallScoringBinding
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class VolleyBallScoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVolleyBallScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>
    var latestScore: VollayBallScoreDTO? = null
        private set
    private val ACTIVITY_SOCKET_KEY = "VolleyBallScoringActivity"

    private var volleyBallScoringFragment: VolleyBallScoringFragment? = null
    private var volleyBallScoreCardFragment: VolleyBallScoreCardFragment? = null
    private var volleyBallHighLightsFragment: VolleyBallHighLightsFragment? = null
    private var volleyBallInfoFragment: VolleyBallInfoFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVolleyBallScoringBinding.inflate(layoutInflater)
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
            binding.btnHighlights,
            binding.btnInfo
        )

        matchResponse?.let { match ->
            volleyBallScoringFragment    = VolleyBallScoringFragment.newInstance(match)
            volleyBallScoreCardFragment  = VolleyBallScoreCardFragment.newInstance(match)
            volleyBallHighLightsFragment = VolleyBallHighLightsFragment.newInstance(match)
            volleyBallInfoFragment       = VolleyBallInfoFragment.newInstance(match)
        }

        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            if (volleyBallScoringFragment == null) {
                finish()
                return
            }
            showFragment(volleyBallScoringFragment!!)
        } else {
            val fm = supportFragmentManager
            volleyBallScoringFragment    = fm.findFragmentByTag("VolleyBallScoringFragment")    as? VolleyBallScoringFragment    ?: volleyBallScoringFragment
            volleyBallScoreCardFragment  = fm.findFragmentByTag("VolleyBallScoreCardFragment")  as? VolleyBallScoreCardFragment  ?: volleyBallScoreCardFragment
            volleyBallHighLightsFragment = fm.findFragmentByTag("VolleyBallHighLightsFragment") as? VolleyBallHighLightsFragment ?: volleyBallHighLightsFragment
            volleyBallInfoFragment       = fm.findFragmentByTag("VolleyBallInfoFragment")       as? VolleyBallInfoFragment       ?: volleyBallInfoFragment
            selectButton(binding.btnScoring)
        }

        matchResponse?.id?.let { WebSocketManager.connect(it) }
        WebSocketManager.addMessageListener(ACTIVITY_SOCKET_KEY) { jsonString ->
            val score = runCatching {
                Gson().fromJson(jsonString, VollayBallScoreDTO::class.java)
            }.getOrNull() ?: return@addMessageListener
            latestScore = score
            runOnUiThread {
                volleyBallScoreCardFragment?.onScoreUpdated(score)
                volleyBallHighLightsFragment?.onScoreUpdated(score)
            }
        }
        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(volleyBallScoringFragment ?: return@setOnClickListener)
        }
        binding.btnScoreCard.setOnClickListener {
            selectButton(binding.btnScoreCard)
            showFragment(volleyBallScoreCardFragment ?: return@setOnClickListener)
        }
        binding.btnHighlights.setOnClickListener {
            selectButton(binding.btnHighlights)
            showFragment(volleyBallHighLightsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(volleyBallInfoFragment ?: return@setOnClickListener)
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