package com.example.fypproject.Dialog

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import com.example.fypproject.DTO.MilestoneDto
import com.example.fypproject.R

/**
 * Auto-dismissing milestone popup — shown on top of any scoring Activity.
 *
 * Usage:
 *   MilestoneDialog.show(supportFragmentManager, milestone)
 *
 * It auto-dismisses after AUTO_MS ms and is pointer-events-none equivalent
 * (it has no buttons; tap anywhere dismisses it).
 */
class MilestoneDialog : DialogFragment() {

    companion object {
        private const val AUTO_MS = 2500L
        private const val TAG     = "MilestoneDialog"
        private const val ARG_TITLE    = "title"
        private const val ARG_SUBTITLE = "subtitle"
        private const val ARG_EMOJI    = "emoji"
        private const val ARG_COLOR    = "color"

        fun show(fm: androidx.fragment.app.FragmentManager, milestone: MilestoneDto) {
            // Remove any existing milestone popup first
            (fm.findFragmentByTag(TAG) as? MilestoneDialog)?.dismissAllowingStateLoss()

            val dialog = MilestoneDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE,    milestone.title)
                    putString(ARG_SUBTITLE, milestone.subtitle)
                    putString(ARG_EMOJI,    milestone.emoji)
                    putString(ARG_COLOR,    milestone.color)
                }
            }
            dialog.show(fm, TAG)
        }
    }

    override fun onStart() {
        super.onStart()
        // Position at top-center, full width
        dialog?.window?.apply {
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
            // Push it just below the status bar
            val params = attributes
            params.y = 48
            attributes = params
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_milestone, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title    = requireArguments().getString(ARG_TITLE,    "")
        val subtitle = requireArguments().getString(ARG_SUBTITLE)
        val emoji    = requireArguments().getString(ARG_EMOJI,    "🏆")
        val color    = requireArguments().getString(ARG_COLOR,    "gold")

        view.findViewById<TextView>(R.id.tvMilestoneEmoji).text    = emoji
        view.findViewById<TextView>(R.id.tvMilestoneTitle).text    = title
        val tvSubtitle = view.findViewById<TextView>(R.id.tvMilestoneSubtitle)
        if (!subtitle.isNullOrBlank()) {
            tvSubtitle.text = subtitle
            tvSubtitle.visibility = View.VISIBLE
        } else {
            tvSubtitle.visibility = View.GONE
        }

        // Color the left border stripe
        val stripe = view.findViewById<View>(R.id.viewMilestoneStripe)
        val stripeColor = when (color) {
            "gold"  -> "#F59E0B".toColorInt()    // yellow-400
            "red"   -> "#EF4444".toColorInt()    // red-500
            "blue"  -> "#3B82F6".toColorInt()    // blue-500
            "green" -> "#22C55E".toColorInt()    // green-500
            else    -> "#F59E0B".toColorInt()
        }
        stripe.setBackgroundColor(stripeColor)

        // Tap anywhere to dismiss
        view.setOnClickListener { dismissAllowingStateLoss() }

        // Auto-dismiss
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) dismissAllowingStateLoss()
        }, AUTO_MS)
    }
}