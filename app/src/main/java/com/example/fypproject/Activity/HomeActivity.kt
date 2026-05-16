package com.example.fypproject.Activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.widget.ImageView
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var liveAdapter: MatchAdapter
    private lateinit var upcomingAdapter: MatchAdapter
    private lateinit var sportButtons: List<MaterialButton>
    private var currentSportFilter = "All Sports"
    private var loadingCount: Int = 0

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
            val intent = Intent(this, MatchesDetailActivity::class.java)
            intent.putExtra("status", "LIVE")
            startActivity(intent)
        }

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val name = sharedPreferences.getString("name", "")
        binding.txtUserName.text = name

        fetchAllForCurrentSport()
    }

    // ✅ Dots setup — RecyclerView ke scroll pe active dot update hoga
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
            dot.setColorFilter(if (index == 0) Color.parseColor("#E31212") else Color.parseColor("#BDBDBD"))
            container.addView(dot)
        }

        binding.recyclerLiveMatches.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val pos = lm.findFirstVisibleItemPosition()
                dots.forEachIndexed { index, dot ->
                    dot.setColorFilter(
                        if (index == pos) Color.parseColor("#E31212")
                        else Color.parseColor("#BDBDBD")
                    )
                }
            }
        })
    }

    private fun setupSearchFunctionality() {
        val searchView = binding.searchViewTop
        searchView.isIconified = false
        searchView.queryHint = "Search matches..."
        searchView.clearFocus()

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val q = query?.trim()
                if (q.isNullOrBlank()) fetchAllForCurrentSport()
                else fetchAllForCurrentSport(searchQuery = q.lowercase())
                searchView.clearFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
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

    private fun showEditNameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_player, null)
        val etName = dialogView.findViewById<EditText>(R.id.etNewName)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSaveName)

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
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val id = sharedPreferences.getLong("id", -1L)

        if (id == -1L) { toastShort("User ID not found"); return }

        val currentRole = sharedPreferences.getString("role", "Player")
        val currentUsername = sharedPreferences.getString("username", "")

        lifecycleScope.launch {
            showLoading(true)
            try {
                val updateRequest = com.example.fypproject.DTO.PlayerDto(
                    id = id,
                    name = newName,
                    playerRole = currentRole,
                    username = currentUsername,
                    playerRequests = emptyList()
                )
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.updatePlayer(id, updateRequest)
                }
                if (response.isSuccessful) {
                    binding.txtUserName.text = newName
                    sharedPreferences.edit().putString("name", newName).apply()
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

        binding.recyclerLiveMatches.adapter = liveAdapter
        binding.recyclerUpcomingMatches.adapter = upcomingAdapter
    }

    private fun setupNavigationDrawer() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
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
        binding.menuCompare.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, CompareActivity::class.java))
        }
        binding.menuLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, _ ->
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

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
                        // ✅ Dots update karo data aane ke baad
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

    private fun showLoading(show: Boolean) {
        if (show) loadingCount++ else loadingCount = (loadingCount - 1).coerceAtLeast(0)
        binding.progressOverlay.visibility = if (loadingCount > 0) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        binding.tvLiveEmptyState.visibility = if (liveAdapter.itemCount == 0) View.VISIBLE else View.GONE
        binding.tvUpcomingEmptyState.visibility = if (upcomingAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        fetchAllForCurrentSport()
    }
}