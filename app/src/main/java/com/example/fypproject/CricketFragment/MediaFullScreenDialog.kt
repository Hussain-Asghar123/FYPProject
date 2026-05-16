package com.example.fypproject.CricketFragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.fypproject.R


class MediaFullScreenDialog : DialogFragment() {

    companion object {
        private const val ARG_URL = "url"

        fun newInstance(url: String) = MediaFullScreenDialog().apply {
            arguments = Bundle().apply { putString(ARG_URL, url) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_media_fullscreen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = arguments?.getString(ARG_URL) ?: return

        val ivFull: ImageView   = view.findViewById(R.id.ivFullScreen)
        val btnClose: ImageButton = view.findViewById(R.id.btnFullClose)

        // Make dialog truly full screen
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        Glide.with(this)
            .load(url)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(ivFull)

        btnClose.setOnClickListener { dismiss() }

        // Also dismiss on tap outside the image
        view.setOnClickListener { dismiss() }
        ivFull.setOnClickListener { /* consume — don't dismiss on image tap */ }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}