package com.example.fypproject.Dialog

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.Adapter.NotificationAdapter
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import kotlinx.coroutines.launch

class NotificationPanelFragment : DialogFragment() {

    companion object {
        private const val TAG            = "NotificationPanel"
        private const val ARG_ACCOUNT_ID = "account_id"

        fun show(fm: androidx.fragment.app.FragmentManager, accountId: Long) {
            (fm.findFragmentByTag(TAG) as? NotificationPanelFragment)
                ?.dismissAllowingStateLoss()
            NotificationPanelFragment().apply {
                arguments = Bundle().apply { putLong(ARG_ACCOUNT_ID, accountId) }
            }.show(fm, TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_notification_panel, container, false)

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setGravity(Gravity.TOP or Gravity.END)
            setLayout(
                resources.getDimensionPixelSize(R.dimen.notification_panel_width),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
            val params = attributes
            params.y = 120
            attributes = params
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val accountId      = requireArguments().getLong(ARG_ACCOUNT_ID)
        val rv             = view.findViewById<RecyclerView>(R.id.rvNotifications)
        val layoutEmpty    = view.findViewById<android.widget.LinearLayout>(R.id.layoutEmpty)
        val tvUnread       = view.findViewById<TextView>(R.id.tvUnreadCount)
        val btnMarkAll     = view.findViewById<ImageView>(R.id.btnMarkAllRead)
        val tvFooter       = view.findViewById<TextView>(R.id.tvFooter)

        rv.layoutManager = LinearLayoutManager(requireContext())

        val adapterHolder = arrayOfNulls<NotificationAdapter>(1)
        val adapter = NotificationAdapter(emptyList()) { notif ->
            if (!notif.isRead) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        RetrofitInstance.api.markNotificationAsRead(notif.id)
                        adapterHolder[0]?.markItemRead(notif.id)
                        adapterHolder[0]?.let { refreshUnreadBadge(it, tvUnread, btnMarkAll) }
                    } catch (_: Exception) {}
                }
            }
        }
        adapterHolder[0] = adapter
        rv.adapter = adapter

        // ── Fetch notifications ───────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitInstance.api.getAccountNotifications(accountId)
                if (res.isSuccessful) {
                    val list = res.body() ?: emptyList()

                    if (list.isEmpty()) {
                        layoutEmpty.visibility = View.VISIBLE
                        rv.visibility          = View.GONE
                        tvFooter.visibility    = View.GONE
                        tvUnread.visibility    = View.GONE
                        btnMarkAll.visibility  = View.GONE
                    } else {
                        layoutEmpty.visibility = View.GONE
                        rv.visibility          = View.VISIBLE
                        adapter.update(list)

                        val unread = list.count { !it.isRead }
                        updateUnreadUI(unread, tvUnread, btnMarkAll)

                        tvFooter.visibility = if (unread == 0) View.VISIBLE else View.GONE
                    }
                }
            } catch (_: Exception) {
                layoutEmpty.visibility = View.VISIBLE
                rv.visibility          = View.GONE
            }
        }

        // ── Mark All as Read button ───────────────────────────────────
        btnMarkAll.setOnClickListener {
            btnMarkAll.isEnabled = false
            btnMarkAll.alpha     = 0.4f

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val unreadIds = adapter.getUnreadIds()
                    unreadIds.forEach { id ->
                        try { RetrofitInstance.api.markNotificationAsRead(id) }
                        catch (_: Exception) {}
                    }
                    // Sab ko read mark karo UI mein
                    adapter.markAllRead()
                    updateUnreadUI(0, tvUnread, btnMarkAll)
                    tvFooter.visibility = View.VISIBLE

                } catch (_: Exception) {
                    btnMarkAll.isEnabled = true
                    btnMarkAll.alpha     = 1f
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun updateUnreadUI(
        unread: Int,
        tvUnread: TextView,
        btnMarkAll: ImageView
    ) {
        if (unread > 0) {
            tvUnread.text       = unread.toString()
            tvUnread.visibility = View.VISIBLE
            btnMarkAll.visibility = View.VISIBLE
            btnMarkAll.isEnabled  = true
            btnMarkAll.alpha      = 1f
        } else {
            tvUnread.visibility   = View.GONE
            btnMarkAll.visibility = View.GONE
        }
    }

    private fun refreshUnreadBadge(
        adapter: NotificationAdapter,
        tvUnread: TextView,
        btnMarkAll: ImageView
    ) {
        val remaining = adapter.getUnreadCount()
        updateUnreadUI(remaining, tvUnread, btnMarkAll)
    }
}