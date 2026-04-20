package com.example.fypproject.ChessFragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R

class ChessHighlightsFragment: Fragment(R.layout.chess_highlight_fragment) {
    companion object {
        fun newInstance(match: MatchResponse) = ChessHighlightsFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}