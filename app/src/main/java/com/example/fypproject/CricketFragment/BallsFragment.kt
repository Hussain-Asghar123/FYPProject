package com.example.fypproject.CricketFragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.Adapter.BallByBallAdapter
import com.example.fypproject.ScoringDTO.Ball
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.Sockets.SocketState
import com.example.fypproject.Sockets.WebSocketManager
import com.example.fypproject.Utils.toastShort
import kotlinx.coroutines.launch

class BallsFragment : Fragment(R.layout.balls_fragment) {

    private var _binding: com.example.fypproject.databinding.BallsFragmentBinding? = null
    private val binding get() = _binding!!

    private var matchResponse: MatchResponse? = null
    private val SOCKET_KEY = "BallsFragment"
    private var activeTeamId: Long? = null

    private lateinit var ballAdapter: BallByBallAdapter

    // All balls fetched from API (source of truth)
    private var allBalls = mutableListOf<Ball>()

    private lateinit var layoutEmptyFiltered: LinearLayout

    // ── Filter state (same as JS) ─────────────────────────────────────────────
    private var showFilters = false
    private val activeEventFilters = mutableSetOf<String>() // "boundary","wicket","extra","dot"
    private var selectedBatsman = "all"
    private var selectedBowler  = "all"

    // ── Filter UI refs (inflated from filter_panel include) ───────────────────
    private lateinit var layoutFilter: View
    private lateinit var tvBallCount: TextView
    private lateinit var btnToggleFilter: TextView
    private lateinit var btnClearFilter: TextView

    // Event pills
    private lateinit var btnAll: TextView
    private lateinit var btnBoundary: TextView
    private lateinit var btnWicket: TextView
    private lateinit var btnExtra: TextView
    private lateinit var btnDot: TextView

    // Player spinners
    private lateinit var spinnerBatsman: Spinner
    private lateinit var spinnerBowler: Spinner

    // ─────────────────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = com.example.fypproject.databinding.BallsFragmentBinding.bind(view)

        matchResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("match_response", MatchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("match_response") as? MatchResponse
        }

        setupRecyclerView()
        setupTeamTabs()
        setupFilterUI(view)

        matchResponse?.let {
            activeTeamId = it.team1Id
            binding.tvTeam1.text = it.team1Name?.uppercase() ?: "TEAM 1"
            binding.tvTeam2.text = it.team2Name?.uppercase() ?: "TEAM 2"
            fetchBalls()
        }

        setupSocketConnection()
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        ballAdapter = BallByBallAdapter()
        binding.rvBallByBall.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                reverseLayout = true
                stackFromEnd  = false
            }
            adapter = ballAdapter
        }
    }

    // ── Team tabs ─────────────────────────────────────────────────────────────

    private fun setupTeamTabs() {
        binding.tvTeam1.setOnClickListener {
            activeTeamId = matchResponse?.team1Id
            highlightTab(true)
            clearFiltersAndFetch()
        }
        binding.tvTeam2.setOnClickListener {
            activeTeamId = matchResponse?.team2Id
            highlightTab(false)
            clearFiltersAndFetch()
        }
        highlightTab(true)
    }

    private fun highlightTab(isTeam1: Boolean) {
        if (isTeam1) {
            binding.tvTeam1.setBackgroundResource(R.drawable.tab_selected)
            binding.tvTeam1.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.tvTeam2.setBackgroundResource(R.drawable.tab_unselected)
            binding.tvTeam2.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
        } else {
            binding.tvTeam2.setBackgroundResource(R.drawable.tab_selected)
            binding.tvTeam2.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.tvTeam1.setBackgroundResource(R.drawable.tab_unselected)
            binding.tvTeam1.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
        }
    }

    // ── Filter UI setup ───────────────────────────────────────────────────────

    private fun setupFilterUI(root: View) {
        // These views are in the layout — see balls_fragment.xml additions
        layoutFilter     = root.findViewById(R.id.layoutFilterPanel)
        tvBallCount      = root.findViewById(R.id.tvBallCount)
        btnToggleFilter  = root.findViewById(R.id.btnToggleFilter)
        btnClearFilter   = root.findViewById(R.id.btnClearFilter)

        btnAll      = root.findViewById(R.id.btnFilterAll)
        btnBoundary = root.findViewById(R.id.btnFilterBoundary)
        btnWicket   = root.findViewById(R.id.btnFilterWicket)
        btnExtra    = root.findViewById(R.id.btnFilterExtra)
        btnDot      = root.findViewById(R.id.btnFilterDot)

        layoutEmptyFiltered = root.findViewById(R.id.layoutEmptyFiltered)
        root.findViewById<TextView>(R.id.btnClearFromEmpty)?.setOnClickListener { clearFilters() }

        spinnerBatsman = root.findViewById(R.id.spinnerBatsman)
        spinnerBowler  = root.findViewById(R.id.spinnerBowler)

        // Toggle filter panel (same as JS showFilters state)
        btnToggleFilter.setOnClickListener {
            showFilters = !showFilters
            btnToggleFilter.text = if (showFilters) "▲ Hide Filters" else "▼ Show Filters"
            animateFilterPanel(showFilters)
        }



        // Clear all filters
        btnClearFilter.setOnClickListener { clearFilters() }

        // Event pill clicks
        btnAll.setOnClickListener {
            activeEventFilters.clear()
            updatePillUI()
            applyFilters()
        }
        btnBoundary.setOnClickListener { toggleEventFilter("boundary") }
        btnWicket.setOnClickListener   { toggleEventFilter("wicket")   }
        btnExtra.setOnClickListener    { toggleEventFilter("extra")    }
        btnDot.setOnClickListener      { toggleEventFilter("dot")      }
    }

    private fun toggleEventFilter(key: String) {
        if (activeEventFilters.contains(key)) activeEventFilters.remove(key)
        else activeEventFilters.add(key)
        updatePillUI()
        applyFilters()
    }
    private fun animateFilterPanel(show: Boolean) {
        if (show) {
            layoutFilter.visibility = View.VISIBLE
            layoutFilter.alpha = 0f
            layoutFilter.translationY = -30f
            layoutFilter.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else {
            layoutFilter.animate()
                .alpha(0f)
                .translationY(-30f)
                .setDuration(180)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { layoutFilter.visibility = View.GONE }
                .start()
        }
    }

    /** Mirrors JS getBallBgColor pill colors */
    private fun updatePillUI() {
        val isAllActive = activeEventFilters.isEmpty()

        setPillActive(btnAll,      isAllActive,     0xFF1F2937.toInt(), 0xFFFFFFFF.toInt())
        setPillActive(btnBoundary, activeEventFilters.contains("boundary"), 0xFF2563EB.toInt(), 0xFFFFFFFF.toInt())
        setPillActive(btnWicket,   activeEventFilters.contains("wicket"),   0xFFDC2626.toInt(), 0xFFFFFFFF.toInt())
        setPillActive(btnExtra,    activeEventFilters.contains("extra"),    0xFFF97316.toInt(), 0xFFFFFFFF.toInt())
        setPillActive(btnDot,      activeEventFilters.contains("dot"),      0xFF4B5563.toInt(), 0xFFFFFFFF.toInt())

        // Show/hide clear button
        val hasActive = activeEventFilters.isNotEmpty() ||
                selectedBatsman != "all" || selectedBowler != "all"
        btnClearFilter.visibility = if (hasActive) View.VISIBLE else View.GONE
    }

    private fun setPillActive(tv: TextView, active: Boolean, activeColor: Int, activeText: Int) {
        if (active) {
            tv.setBackgroundColor(activeColor)
            tv.setTextColor(activeText)
        } else {
            tv.setBackgroundColor(0xFFE5E7EB.toInt())
            tv.setTextColor(0xFF374151.toInt())
        }
    }

    // ── Spinner population (called after balls loaded) ─────────────────────

    private fun populateSpinners() {
        val batsmen = listOf("All Batsmen") +
                allBalls.mapNotNull { it.batsmanName }.distinct().sorted()
        val bowlers = listOf("All Bowlers") +
                allBalls.mapNotNull { it.bowlerName }.distinct().sorted()

        val bAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, batsmen)
        bAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBatsman.adapter = bAdapter

        val wAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, bowlers)
        wAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBowler.adapter = wAdapter

        spinnerBatsman.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedBatsman = if (pos == 0) "all" else batsmen[pos]
                updatePillUI()
                applyFilters()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spinnerBowler.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedBowler = if (pos == 0) "all" else bowlers[pos]
                updatePillUI()
                applyFilters()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ── Filter logic — mirrors JS exactly ─────────────────────────────────────

    private fun applyFilters() {
        val filtered = allBalls.filter { ball ->
            if (selectedBatsman != "all" && ball.batsmanName != selectedBatsman) return@filter false
            if (selectedBowler  != "all" && ball.bowlerName  != selectedBowler)  return@filter false
            if (activeEventFilters.isEmpty()) return@filter true

            val isBoundary = ball.event == "4" || ball.event == "6" || ball.isBoundary == true
            val isWicket   = ball.isWicket == true
            val isExtra    = ball.eventType == "wide" || ball.eventType == "noball" ||
                    ball.eventType == "legbye" || ball.eventType == "bye"
            val isDot      = ball.event == "0" && ball.isWicket != true && ball.eventType == "run"

            (activeEventFilters.contains("boundary") && isBoundary) ||
                    (activeEventFilters.contains("wicket")   && isWicket)   ||
                    (activeEventFilters.contains("extra")    && isExtra)     ||
                    (activeEventFilters.contains("dot")      && isDot)
        }

        ballAdapter.submitList(filtered)
        tvBallCount.text = "Showing ${filtered.size} of ${allBalls.size} balls"

        when {
            // Data exists but filters yield zero results
            filtered.isEmpty() && allBalls.isNotEmpty() -> {
                binding.rvBallByBall.visibility  = View.GONE
                binding.emptyLayout.visibility   = View.GONE
                layoutEmptyFiltered.visibility   = View.VISIBLE
            }
            filtered.isNotEmpty() -> {
                binding.rvBallByBall.visibility  = View.VISIBLE
                binding.emptyLayout.visibility   = View.GONE
                layoutEmptyFiltered.visibility   = View.GONE
            }
            else -> showEmpty()   // allBalls was empty
        }
    }

    private fun clearFilters() {
        activeEventFilters.clear()
        selectedBatsman = "all"
        selectedBowler  = "all"
        spinnerBatsman.setSelection(0)
        spinnerBowler.setSelection(0)
        updatePillUI()
        applyFilters()
    }

    private fun clearFiltersAndFetch() {
        allBalls.clear()
        ballAdapter.submitList(emptyList())
        clearFilters()
        fetchBalls()
    }

    // ── Data fetching ─────────────────────────────────────────────────────────

    private fun fetchBalls() {
        val matchId = matchResponse?.id ?: return
        val teamId  = activeTeamId      ?: return

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = api.getMatchBalls(matchId, teamId)
                if (response.isSuccessful) {
                    val balls = response.body() ?: emptyList()
                    allBalls.clear()
                    allBalls.addAll(balls)

                    // Show filter header only when data exists
                    val filterHeader = view?.findViewById<View>(R.id.layoutFilterHeader)
                    filterHeader?.visibility = if (allBalls.isNotEmpty()) View.VISIBLE else View.GONE

                    if (allBalls.isNotEmpty()) {
                        populateSpinners()
                        applyFilters()
                        binding.rvBallByBall.scrollToPosition(0)
                    } else {
                        showEmpty()
                    }
                } else {
                    showEmpty()
                }
            } catch (e: Exception) {
                requireContext().toastShort("Error: ${e.localizedMessage}")
                showEmpty()
            } finally {
                showLoading(false)
            }
        }
    }

    // ── UI state helpers ──────────────────────────────────────────────────────

    private fun showLoading(show: Boolean) {
        binding.loadingLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvBallByBall.visibility  = if (show) View.GONE   else View.VISIBLE
        binding.emptyLayout.visibility   = View.GONE
    }

    private fun showEmpty() {
        binding.emptyLayout.visibility   = View.VISIBLE
        binding.rvBallByBall.visibility  = View.GONE
        binding.loadingLayout.visibility = View.GONE
    }

    // ── Socket ────────────────────────────────────────────────────────────────

    private fun setupSocketConnection() {
        WebSocketManager.addStateListener(SOCKET_KEY) { state ->
            activity?.runOnUiThread {
                if (state is SocketState.Error)
                    requireContext().toastShort("Socket Error: ${state.message}")
            }
        }
        WebSocketManager.addMessageListener(SOCKET_KEY) { /* handled by Activity */ }
    }

    fun onSocketUpdate() {
        if (_binding == null) return
        fetchBalls()
    }

    private fun registerSocketListeners()   = setupSocketConnection()
    private fun unregisterSocketListeners() {
        WebSocketManager.removeStateListener(SOCKET_KEY)
        WebSocketManager.removeMessageListener(SOCKET_KEY)
    }

    override fun onResume() { super.onResume(); registerSocketListeners() }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) registerSocketListeners() else unregisterSocketListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterSocketListeners()
        _binding = null
    }

    companion object {
        fun newInstance(match: MatchResponse) = BallsFragment().apply {
            arguments = Bundle().apply { putSerializable("match_response", match) }
        }
    }
}