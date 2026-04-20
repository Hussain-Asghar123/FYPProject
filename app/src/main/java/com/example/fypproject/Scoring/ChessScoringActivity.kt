package com.example.fypproject.Scoring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fypproject.ChessFragment.ChessHighlightsFragment
import com.example.fypproject.ChessFragment.ChessInfoFragment
import com.example.fypproject.ChessFragment.ChessScoringFragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.databinding.ActivityChessScoringBinding
import com.google.android.material.button.MaterialButton

class ChessScoringActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChessScoringBinding
    private var matchResponse: MatchResponse? = null
    private lateinit var buttons: List<MaterialButton>

    private var chessScoringFragment: ChessScoringFragment? = null
    private var chessHighlightsFragment: ChessHighlightsFragment? = null
    private var chessInfoFragment: ChessInfoFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChessScoringBinding.inflate(layoutInflater)
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
            chessScoringFragment    = ChessScoringFragment.newInstance(match)
            chessHighlightsFragment = ChessHighlightsFragment.newInstance(match)
            chessInfoFragment       = ChessInfoFragment.newInstance(match)
        }

        if (savedInstanceState == null) {
            selectButton(binding.btnScoring)
            if (chessScoringFragment == null) {
                finish()
                return
            }
            showFragment(chessScoringFragment!!)
        } else {
            val fm = supportFragmentManager
            chessScoringFragment    = fm.findFragmentByTag("ChessScoringFragment")    as? ChessScoringFragment    ?: chessScoringFragment
            chessHighlightsFragment = fm.findFragmentByTag("ChessHighlightsFragment") as? ChessHighlightsFragment ?: chessHighlightsFragment
            chessInfoFragment       = fm.findFragmentByTag("ChessInfoFragment")       as? ChessInfoFragment       ?: chessInfoFragment
            selectButton(binding.btnScoring)
        }

        binding.btnScoring.setOnClickListener {
            selectButton(binding.btnScoring)
            showFragment(chessScoringFragment ?: return@setOnClickListener)
        }
        binding.btnHighlights.setOnClickListener {
            selectButton(binding.btnHighlights)
            showFragment(chessHighlightsFragment ?: return@setOnClickListener)
        }
        binding.btnInfo.setOnClickListener {
            selectButton(binding.btnInfo)
            showFragment(chessInfoFragment ?: return@setOnClickListener)
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