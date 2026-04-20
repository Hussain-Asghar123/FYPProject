package com.example.fypproject.ChessFragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.TableTennisFragment.TableTennisScoringFragment

class ChessScoringFragment: Fragment(R.layout.chess_scoring_fragment) {
    companion object {
        fun newInstance(match: MatchResponse) = ChessScoringFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}