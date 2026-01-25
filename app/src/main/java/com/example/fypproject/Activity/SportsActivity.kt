package com.example.fypproject.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fypproject.R
import com.example.fypproject.databinding.ActivitySportsBinding

class SportsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySportsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}