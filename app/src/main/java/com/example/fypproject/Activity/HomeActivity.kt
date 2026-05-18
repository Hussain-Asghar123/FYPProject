package com.example.fypproject.Activity

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.Adapter.MatchAdapter
import com.example.fypproject.Network.RetrofitInstance
import com.example.fypproject.R
import com.example.fypproject.Utils.MatchNavigator
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.Utils.toastShort
import com.example.fypproject.databinding.ActivityHomeBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.example.fypproject.Dialog.NotificationPanelFragment
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.fypproject.BottomSheet.PlayerInfoBottomSheetFragment
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var liveAdapter: MatchAdapter
    private lateinit var upcomingAdapter: MatchAdapter
    private val notifHandler = Handler(Looper.getMainLooper())
    private var unreadCount = 0
    private lateinit var sportButtons: List<MaterialButton>
    private var currentSportFilter = "All Sports"
    private var loadingCount: Int = 0
    private var previousUnreadCount = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupNavigationDrawer()
        setupSportsButtons()
        setupSearchFunctionality()

        binding.btnEdit.setOnClickListener { showEditNameDialog() }

        binding.txtViewAllLive.setOnClickListener {
            startActivity(
                Intent(this, MatchesDetailActivity::class.java)
                    .putExtra("status", "LIVE")
            )
        }

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        binding.txtUserName.text = sharedPreferences.getString("name", "")

        fetchAllForCurrentSport()
        setupNotificationBell()
    }

    // ── Notification bell ─────────────────────────────────────────────────────

    private fun setupNotificationBell() {
        val prefs  = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val role   = prefs.getString("role", "") ?: ""

        // ── Admin ka koi notification nahi, bell bhi nahi ────────────
        if (role == "ADMIN") {
            binding.btnNotificationBell.visibility = View.GONE
            binding.tvNotifBadge.visibility        = View.GONE
            return
        }

        val accountId = prefs.getLong("id", -1L)
        if (accountId == -1L) return

        val tvBadge = binding.tvNotifBadge

        // First run se pehle -1 rakho taake app start par toast na aaye
        var previousUnreadCount = -1

        // ── Poll har 30s mein ─────────────────────────────────────────
        val fetchNotifs = object : Runnable {
            override fun run() {
                lifecycleScope.launch {
                    try {
                        val res = RetrofitInstance.api.getAccountNotifications(accountId)
                        if (res.isSuccessful) {
                            val newUnread = res.body()?.count { !it.isRead } ?: 0

                            // Sirf tab toast show karo jab actually naya notification aaya ho
                            if (previousUnreadCount == -1 && newUnread > 0) {
                                showCustomNotifToast(
                                    "You have $newUnread unread notification${if (newUnread > 1) "s" else ""}! 🔔"
                                )
                            } else if (previousUnreadCount >= 0 && newUnread > previousUnreadCount) {
                                val diff = newUnread - previousUnreadCount
                                showCustomNotifToast(
                                    "You have $diff new notification${if (diff > 1) "s" else ""}! 🔔"
                                )
                            }
                            previousUnreadCount = newUnread
                            unreadCount         = newUnread
                            updateBadge(tvBadge, unreadCount)
                        }
                    } catch (_: Exception) {}
                }
                notifHandler.postDelayed(this, 30_000L)
            }
        }
        notifHandler.post(fetchNotifs)

        // ── Bell click ────────────────────────────────────────────────
        binding.btnNotificationBell.setOnClickListener {

            // 1. Badge turant clear karo (UX feel)
            unreadCount         = 0
            previousUnreadCount = 0          // ← reset taake panel open ke baad toast na aaye
            updateBadge(tvBadge, 0)

            // 2. Backend par saare unread mark as read karo
            lifecycleScope.launch {
                try {
                    val res = RetrofitInstance.api.getAccountNotifications(accountId)
                    if (res.isSuccessful) {
                        val unreadIds = res.body()
                            ?.filter { !it.isRead }
                            ?.map { it.id } ?: emptyList()

                        unreadIds.forEach { notifId ->
                            try { RetrofitInstance.api.markNotificationAsRead(notifId) }
                            catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Panel open karo
            NotificationPanelFragment.show(supportFragmentManager, accountId)

            // 4. 2s baad verify karo
            notifHandler.postDelayed({
                lifecycleScope.launch {
                    try {
                        val res = RetrofitInstance.api.getAccountNotifications(accountId)
                        if (res.isSuccessful) {
                            val count = res.body()?.count { !it.isRead } ?: 0
                            unreadCount         = count
                            previousUnreadCount = count   // ← sync karo
                            updateBadge(tvBadge, count)
                        }
                    } catch (_: Exception) {}
                }
            }, 2000L)
        }
    }

    /** Badge visibility ek jagah se control hoti hai */
    private fun updateBadge(tvBadge: android.widget.TextView, count: Int) {
        tvBadge.text       = if (count > 0) count.toString() else ""
        tvBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    // ── Custom Notification Toast ─────────────────────────────────────────────

    /**
     * Red background wala custom toast jisme chhota sa ✕ button hota hai.
     * Programmatically banta hai — koi extra layout file zaroorat nahi.
     * 4 seconds baad automatically gayab ho jaata hai.
     */
    private fun showCustomNotifToast(message: String) {
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val dp       = resources.displayMetrics.density

        // Pehle se exist karta ho to animate out kar ke remove karo
        rootView.findViewWithTag<View>("custom_notif_toast")?.let { old ->
            old.animate()
                .translationX(old.width.toFloat() + (16 * dp))
                .alpha(0f)
                .setDuration(250)
                .withEndAction { if (old.parent != null) rootView.removeView(old) }
                .start()
        }

        // ── Outer container (red rounded card) ───────────────────────────
        val container = FrameLayout(this).apply {
            tag        = "custom_notif_toast"
            elevation  = 10 * dp
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#C62828"))
                cornerRadius = 14 * dp
            }
            val padH = (14 * dp).toInt()
            val padV = (10 * dp).toInt()
            setPadding(padH, padV, padH, padV)
            // Shuru mein screen ke bahar right side par rakho
            translationX = 800f
            alpha        = 0f
        }

        // ── Emoji / icon ──────────────────────────────────────────────────
        val tvEmoji = TextView(this).apply {
            text     = "🔔"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (8 * dp).toInt() }
        }

        // ── Message text ──────────────────────────────────────────────────
        val tvMsg = TextView(this).apply {
            text      = message
            textSize  = 13f
            setTextColor(Color.WHITE)
            maxLines  = 2
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginEnd = (8 * dp).toInt() }
        }

        // ── Close (✕) button ──────────────────────────────────────────────
        val btnClose = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            background = null
            val size = (20 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
        }

        // ── Horizontal row ────────────────────────────────────────────────
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            addView(tvEmoji)
            addView(tvMsg)
            addView(btnClose)
        }
        container.addView(row)

        // ── Position: top-right corner ────────────────────────────────────
        val containerParams = FrameLayout.LayoutParams(
            (280 * dp).toInt(),                          // fixed width — right side card
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END         // right side
            val margin = (12 * dp).toInt()
            setMargins(0, (88 * dp).toInt(), margin, 0)
        }

        rootView.addView(container, containerParams)

        // ── Slide-in animation (right se left) ───────────────────────────
        container.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(350)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
            .start()

        // ── Dismiss helper (slide-out + remove) ───────────────────────────
        fun dismiss() {
            notifHandler.removeCallbacksAndMessages("toast_dismiss")
            container.animate()
                .translationX(container.width.toFloat() + (16 * dp))
                .alpha(0f)
                .setDuration(280)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { if (container.parent != null) rootView.removeView(container) }
                .start()
        }

        // ── Auto-dismiss after 4 seconds ──────────────────────────────────
        val dismissRunnable = Runnable { dismiss() }
        notifHandler.postDelayed(dismissRunnable, 4000L)

        // ── Manual close ──────────────────────────────────────────────────
        btnClose.setOnClickListener {
            notifHandler.removeCallbacks(dismissRunnable)
            dismiss()
        }
    }
    // ── Dots indicator ────────────────────────────────────────────────────────

    private fun setupDotsIndicator(count: Int) {
        val container = binding.dotsIndicator
        container.removeAllViews()
        if (count <= 1) return

        val dots = Array(count) { ImageView(this) }
        dots.forEachIndexed { index, dot ->
            dot.setImageResource(R.drawable.dot_selector)
            val params = ViewGroup.MarginLayoutParams(20, 20)
            params.setMargins(6, 0, 6, 0)
            dot.layoutParams = params
            dot.setColorFilter(
                if (index == 0) Color.parseColor("#E31212")
                else Color.parseColor("#BDBDBD")
            )
            container.addView(dot)
        }

        binding.recyclerLiveMatches.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val pos = (recyclerView.layoutManager as LinearLayoutManager)
                    .findFirstVisibleItemPosition()
                dots.forEachIndexed { index, dot ->
                    dot.setColorFilter(
                        if (index == pos) Color.parseColor("#E31212")
                        else Color.parseColor("#BDBDBD")
                    )
                }
            }
        })
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private fun setupSearchFunctionality() {
        val searchView = binding.searchViewTop
        searchView.isIconified = false
        searchView.queryHint   = "Search matches..."
        searchView.clearFocus()

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val q = query?.trim()
                if (q.isNullOrBlank()) fetchAllForCurrentSport()
                else fetchAllForCurrentSport(searchQuery = q.lowercase())
                searchView.clearFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val q = newText?.trim()
                if (q.isNullOrBlank()) fetchAllForCurrentSport()
                else fetchAllForCurrentSport(searchQuery = q.lowercase())
                return true
            }
        })
    }

    // ── Edit name dialog ──────────────────────────────────────────────────────

    private fun showEditNameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_player, null)
        val etName     = dialogView.findViewById<EditText>(R.id.etNewName)
        val btnSave    = dialogView.findViewById<MaterialButton>(R.id.btnSaveName)

        etName.setText(binding.txtUserName.text.toString())

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isNotEmpty()) updateName(newName, dialog)
            else etName.error = "Name cannot be empty"
        }
        dialog.show()
    }

    private fun updateName(newName: String, dialog: AlertDialog) {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val id    = prefs.getLong("id", -1L)
        if (id == -1L) { toastShort("User ID not found"); return }

        lifecycleScope.launch {
            showLoading(true)
            try {
                val updateRequest = com.example.fypproject.DTO.PlayerDto(
                    id             = id,
                    name           = newName,
                    playerRole     = prefs.getString("role", "Player"),
                    username       = prefs.getString("username", ""),
                    playerRequests = emptyList()
                )
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.updatePlayer(id, updateRequest)
                }
                if (response.isSuccessful) {
                    binding.txtUserName.text = newName
                    prefs.edit().putString("name", newName).apply()
                    toastShort("Name updated successfully")
                    dialog.dismiss()
                } else {
                    toastLong(NetworkUi.userMessage(response, "Failed to update name"))
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
            } finally {
                showLoading(false)
            }
        }
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private fun setupRecyclerViews() {
        binding.recyclerLiveMatches.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerUpcomingMatches.layoutManager = LinearLayoutManager(this)

        liveAdapter = MatchAdapter(mutableListOf(), true) { match ->
            MatchNavigator.navigate(this@HomeActivity, match)
        }
        upcomingAdapter = MatchAdapter(mutableListOf(), false) { match ->
            MatchNavigator.navigate(this@HomeActivity, match)
        }

        binding.recyclerLiveMatches.adapter     = liveAdapter
        binding.recyclerUpcomingMatches.adapter = upcomingAdapter
    }

    // ── Navigation drawer ─────────────────────────────────────────────────────

    private fun setupNavigationDrawer() {
        val prefs     = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val accountId = prefs.getLong("id", -1L)
        val userName  = prefs.getString("name", "") ?: ""

        val photoUrl   = prefs.getString("profilePhotoUrl", null)
        val ivNavPhoto = binding.ivNavProfilePhoto
        val tvInitial  = binding.tvNavInitial

        if (!photoUrl.isNullOrBlank()) {
            Glide.with(this).load(photoUrl).into(ivNavPhoto)
            ivNavPhoto.visibility = View.VISIBLE
            tvInitial.visibility  = View.GONE
        } else {
            ivNavPhoto.visibility = View.GONE
            tvInitial.text        = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            tvInitial.visibility  = View.VISIBLE
        }

        val photoLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri ?: return@registerForActivityResult
            lifecycleScope.launch {
                try {
                    val stream      = contentResolver.openInputStream(uri) ?: return@launch
                    val bytes       = stream.readBytes()
                    val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    val body        = MultipartBody.Part.createFormData("file", "photo.jpg", requestFile)
                    val res         = RetrofitInstance.api.uploadProfilePhoto(accountId, body)
                    if (res.isSuccessful) {
                        val newUrl = res.body()?.profilePhotoUrl ?: return@launch
                        prefs.edit().putString("profilePhotoUrl", newUrl).apply()
                        Glide.with(this@HomeActivity).load(newUrl).into(ivNavPhoto)
                        ivNavPhoto.visibility = View.VISIBLE
                        tvInitial.visibility  = View.GONE
                        toastShort("Profile photo updated!")
                    }
                } catch (e: Exception) {
                    toastLong("Failed to upload photo: ${e.message}")
                }
            }
        }

        binding.btnEditNavPhoto.setOnClickListener { photoLauncher.launch("image/*") }
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.END) }

        binding.menuSports.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SportsActivity::class.java))
        }
        binding.menuManageAccount.setOnClickListener {
            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val role = sharedPreferences.getString("role", "")
            if (role != "ADMIN") {
                toastShort("You are not authorized to access this page")
                return@setOnClickListener
            }
            startActivity(Intent(this, ManageAccountActivity::class.java))
        }
        binding.menuSeasons.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SeasonsActivity::class.java))
        }
        binding.menuMatches.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, MatchesDetailActivity::class.java))
        }
        binding.menuMyScorer.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, ScrorerActivity::class.java))
        }
        binding.menuRequests.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, RequstsActivity::class.java))
        }
        binding.menuStats.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, HeavyStatsActivity::class.java))
        }
        binding.menuMyFavouriteMedia.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, MyFavouriteMediaActivity::class.java))
        }
        binding.menuCompare.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, CompareActivity::class.java))
        }

        val playerId = prefs.getLong("playerId", -1L)
        if (playerId > 0) {
            binding.navMyPlayerInfo.visibility = View.VISIBLE
            binding.navMyPlayerInfo.setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
                PlayerInfoBottomSheetFragment.newInstance(playerId)
                    .show(supportFragmentManager, "PlayerInfo")
            }
        } else {
            binding.navMyPlayerInfo.visibility = View.GONE
        }

        binding.menuLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, _ ->
                    prefs.edit().clear().apply()
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    // ── Sports buttons ────────────────────────────────────────────────────────

    private fun setupSportsButtons() {
        sportButtons = listOf(
            binding.btnAllSports, binding.btnCricket, binding.btnFutsal,
            binding.btnVolleyBall, binding.btnBadminton, binding.btnTugOfWar,
            binding.btnLudo, binding.btnChess, binding.btnTableTennis
        )
        updateButtonSelection(binding.btnAllSports)
        currentSportFilter = "All Sports"

        sportButtons.forEach { button ->
            button.setOnClickListener {
                updateButtonSelection(button)
                currentSportFilter = button.text.toString()
                fetchAllForCurrentSport()
            }
        }
    }

    private fun updateButtonSelection(selectedButton: MaterialButton) {
        sportButtons.forEach { button ->
            val tint = if (button == selectedButton) Color.parseColor("#E31212") else Color.DKGRAY
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(tint)
        }
    }

    // ── Fetch ─────────────────────────────────────────────────────────────────

    private fun fetchAllForCurrentSport(searchQuery: String? = null) {
        val sportParam = if (currentSportFilter == "All Sports") null else currentSportFilter
        fetchMatches("LIVE", sportParam, searchQuery)
        fetchMatches("UPCOMING", sportParam, searchQuery)
    }

    private fun fetchMatches(status: String, sport: String?, searchQuery: String? = null) {
        if (status == "LIVE") binding.recyclerLiveMatches.visibility = View.INVISIBLE
        else binding.recyclerUpcomingMatches.visibility = View.INVISIBLE

        lifecycleScope.launch {
            showLoading(true)
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getLiveMatches(status = status, sport = sport)
                }
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val filtered = if (!searchQuery.isNullOrBlank()) {
                        list.filter { m ->
                            val t1 = m.team1Name?.lowercase() ?: ""
                            val t2 = m.team2Name?.lowercase() ?: ""
                            t1.contains(searchQuery) || t2.contains(searchQuery) ||
                                    (m.tournamentName?.lowercase()?.contains(searchQuery) ?: false)
                        }
                    } else list

                    if (status == "LIVE") {
                        liveAdapter.updateData(filtered)
                        binding.recyclerLiveMatches.visibility = View.VISIBLE
                        setupDotsIndicator(filtered.size)
                    } else {
                        upcomingAdapter.updateData(filtered)
                        binding.recyclerUpcomingMatches.visibility = View.VISIBLE
                    }
                    checkEmptyState()
                } else {
                    toastLong(NetworkUi.userMessage(response, "Failed to load matches"))
                    if (status == "LIVE") binding.recyclerLiveMatches.visibility = View.VISIBLE
                    else binding.recyclerUpcomingMatches.visibility = View.VISIBLE
                    checkEmptyState()
                }
            } catch (e: Exception) {
                toastLong(NetworkUi.userMessage(e))
                if (status == "LIVE") binding.recyclerLiveMatches.visibility = View.VISIBLE
                else binding.recyclerUpcomingMatches.visibility = View.VISIBLE
                checkEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun showLoading(show: Boolean) {
        if (show) loadingCount++ else loadingCount = (loadingCount - 1).coerceAtLeast(0)
        binding.progressOverlay.visibility = if (loadingCount > 0) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        binding.tvLiveEmptyState.visibility =
            if (liveAdapter.itemCount == 0) View.VISIBLE else View.GONE
        binding.tvUpcomingEmptyState.visibility =
            if (upcomingAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        fetchAllForCurrentSport()
        previousUnreadCount = -1  // ← yeh add karo — har resume par toast trigger hoga
    }

    override fun onDestroy() {
        super.onDestroy()
        notifHandler.removeCallbacksAndMessages(null)
    }
}