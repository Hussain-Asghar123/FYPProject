package com.example.fypproject.CricketFragment

import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.fypproject.R

class VideoPlayerDialog : DialogFragment() {

    private var player: ExoPlayer? = null

    companion object {
        private const val ARG_URL = "video_url"
        fun newInstance(url: String) = VideoPlayerDialog().apply {
            arguments = Bundle().apply { putString(ARG_URL, url) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_video_player, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val url = arguments?.getString(ARG_URL) ?: return
        val playerView = view.findViewById<PlayerView>(R.id.playerView)

        player = ExoPlayer.Builder(requireContext()).build().also { exo ->
            playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.playWhenReady = true
        }

        view.findViewById<ImageButton>(R.id.btnCloseVideo).setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
    }
}