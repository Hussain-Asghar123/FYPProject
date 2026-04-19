package com.example.fypproject.TableTennisFragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.R
import com.example.fypproject.Sockets.JsonConverter
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.TabletennisScoringFragmentBinding

class TableTennisScoringFragment: Fragment(R.layout.tabletennis_scoring_fragment) {
    private var _binding: TabletennisScoringFragmentBinding? = null
    private val binding get() = _binding!!
    private var matchResponse: MatchResponse? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = TabletennisScoringFragmentBinding.bind(view)
        arguments?.let { bundle ->
            matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable("match_response", MatchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable("match_response") as? MatchResponse
            }
        }
        setupSocketConnection()
    }
    override fun onResume() {
        super.onResume()
        matchResponse?.id?.let { id ->
            matchResponse?.id?.toLong()?.let { WebSocketManager.connect(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        WebSocketManager.disconnect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        WebSocketManager.socketStateListener = null
        WebSocketManager.messageListener = null
        WebSocketManager.disconnect()
        _binding = null
    }

    private fun setupSocketConnection() {
        matchResponse?.id?.let { id ->
            WebSocketManager.socketStateListener = { state ->
                activity?.runOnUiThread {
                    when (state) {
                        is SocketState.Connected -> requireContext().toastShort("")
                        is SocketState.Error -> requireContext().toastShort("Socket Error: ${state.message}")
                        is SocketState.Disconnected -> {}
                    }
                }
            }
            WebSocketManager.messageListener = { jsonString ->
                val updatedScore = JsonConverter.fromJson(jsonString)
                updatedScore?.let {
                    activity?.runOnUiThread {
                    }
                }
            }
            matchResponse?.id?.toLong()?.let { WebSocketManager.connect(it) }
        }
    }

    companion object {
        fun newInstance(match: MatchResponse): TableTennisScoringFragment {
            return TableTennisScoringFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("match_response", match)
                }
            }
        }
    }

}