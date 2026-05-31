package com.example.fypproject.Activity

import android.graphics.Color.DKGRAY
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.MatchesDetailAdapter
import com.example.fypproject.DTO.FixturesResponse
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.SeasonResponse
import com.example.fypproject.DTO.TournamentResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.Utils.MatchNavigator
import com.example.fypproject.Utils.NetworkUi
import com.example.fypproject.Utils.toastLong
import com.example.fypproject.databinding.ActivityMatchesDetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList
import kotlin.collections.map

class MatchesDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchesDetailBinding
    private lateinit var adapter: MatchesDetailAdapter

    // Filter state
    private var selectedSport: String? = null
    private var selectedSportId: Long? = null
    private var selectedStatus: String = "ALL"
    private var newestFirst: Boolean = true

    // Dropdown data
    private var seasonList    = mutableListOf<SeasonResponse>()
    private var tournamentList = mutableListOf<TournamentResponse>()

    private var selectedSeasonId:     Long? = null
    private var selectedTournamentId: Long? = null
    private var selectedTournamentSportId: Long? = null
    private var isTournamentLocked = false

    private val allSportIds = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)

    // Sport buttons list — easier to loop
    private val sportButtons by lazy {
        listOf(
            binding.btnAllSports,
            binding.btnCricket,
            binding.btnFootball,
            binding.btnVolleyball,
            binding.btnBadminton,
            binding.btnTableTennis,
            binding.btnTugOfWar,
            binding.btnLudo,
            binding.btnChess,
            binding.btnHockey
        )
    }

    // sportId → button mapping
    private val sportIdToButtonMap by lazy {
        mapOf(
            1L to binding.btnCricket,
            2L to binding.btnFootball,
            3L to binding.btnVolleyball,
            4L to binding.btnTableTennis,
            5L to binding.btnBadminton,
            6L to binding.btnLudo,
            7L to binding.btnTugOfWar,
            8L to binding.btnChess,
            9L to binding.btnHockey
        )
    }

    // sportId → sport string mapping
    private val sportIdToName = mapOf(
        1L to "Cricket",
        2L to "Futsal",
        3L to "VolleyBall",
        4L to "Table Tennis",
        5L to "Badminton",
        6L to "Ludo",
        7L to "TugOfWar",
        8L to "Chess",
        9L to "Hockey"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchesDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupAdapter()
        setupSportButtons()
        setupStatusButtons()
        setupSortButton()
        setupSeasonSpinner()

        // Initial load — no filters
        fetchMatches()
        fetchSeasons()
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = MatchesDetailAdapter(mutableListOf()) { match ->
            val prefs    = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val role     = prefs.getString("role", "")
            val username = prefs.getString("username", "")

            val isAdmin          = role == "ADMIN"
            val isAssignedScorer = !match.scorerId.isNullOrBlank() && match.scorerId == username

            if (match.status == "UPCOMING") {
                if (isAdmin || isAssignedScorer) {
                    MatchNavigator.navigate(this@MatchesDetailActivity, match)
                } else {
                    androidx.appcompat.app.AlertDialog.Builder(this@MatchesDetailActivity)
                        .setTitle("Access Restricted")
                        .setMessage("Only Admin or Assigned Scorer can start a match.")
                        .setPositiveButton("OK") { d, _ -> d.dismiss() }
                        .show()
                }
            } else {
                MatchNavigator.navigate(this@MatchesDetailActivity, match)
            }
        }
        binding.rvMatches.layoutManager = LinearLayoutManager(this)
        binding.rvMatches.adapter = adapter
    }

    // ─── Season Spinner ───────────────────────────────────────────────────────

    private fun setupSeasonSpinner() {
        // Default placeholder
        setSeasonSpinnerPlaceholder()

        binding.spinnerSeason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos == 0) {
                    // "All Seasons" selected → reset tournament, enable all sports
                    selectedSeasonId     = null
                    selectedTournamentId = null
                    selectedTournamentSportId = null
                    setTournamentSpinnerPlaceholder()
                    enableAllSportButtons()
                    fetchMatches()
                } else {
                    selectedSeasonId = seasonList.getOrNull(pos - 1)?.id
                    selectedTournamentId = null
                    selectedTournamentSportId = null
                    enableAllSportButtons()
                    val seasonId = selectedSeasonId ?: return
                    selectedSportId?.let { fetchTournamentsForSeason(seasonId, it) }
                        ?: fetchAllTournamentsForSeason(seasonId)
                    fetchMatches()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setSeasonSpinnerPlaceholder() {
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            listOf("All Seasons"))
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSeason.adapter = spinnerAdapter
    }

    private fun fetchSeasons() {
        lifecycleScope.launch {
            try {
                val response = api.getonlynames()
                if (!response.isSuccessful) return@launch

                seasonList.clear()
                seasonList.addAll(response.body().orEmpty())

                val names = mutableListOf("All Seasons") + seasonList.map { it.name }
                val spinnerAdapter = ArrayAdapter(
                    this@MatchesDetailActivity,
                    android.R.layout.simple_spinner_item,
                    names
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSeason.adapter = spinnerAdapter
            } catch (_: Exception) {
                toastLong("Failed to load seasons")
            }
        }
    }

    // ─── Tournament Spinner ───────────────────────────────────────────────────

    private fun setTournamentSpinnerPlaceholder() {
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            listOf("All Tournaments"))
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTournament.adapter = spinnerAdapter
        binding.spinnerTournament.onItemSelectedListener = null
    }

    private fun fetchTournamentsForSeason(seasonId: Long, sportId: Long) {
        api.getSeasonWiseTournaments(seasonId, sportId).enqueue(object : Callback<List<TournamentResponse>> {
            override fun onResponse(call: Call<List<TournamentResponse>>, response: Response<List<TournamentResponse>>) {
                if (!response.isSuccessful) return
                tournamentList.clear()
                tournamentList.addAll(response.body().orEmpty())

                val names = mutableListOf("All Tournaments") + tournamentList.map { it.name }
                val spinnerAdapter = ArrayAdapter(this@MatchesDetailActivity,
                    android.R.layout.simple_spinner_item, names)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                // 🔑 Step 1: Pehle listener null karo
                binding.spinnerTournament.onItemSelectedListener = null
                // 🔑 Step 2: Adapter set karo (ab koi listener nahi hai toh spurious callback safe hai)
                binding.spinnerTournament.adapter = spinnerAdapter
                // 🔑 Step 3: post{} se listener lagao — pending callbacks process hone ke BAAD
                binding.spinnerTournament.post {
                    binding.spinnerTournament.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                                if (pos == 0) {
                                    selectedTournamentId = null
                                    selectedTournamentSportId = null
                                    isTournamentLocked = false
                                    enableAllSportButtons()
                                    fetchMatches()
                                } else {
                                    val tournament = tournamentList.getOrNull(pos - 1)
                                    android.util.Log.d("DEBUG_TOURNAMENT",
                                        "id=${tournament?.id}, name=${tournament?.name}, sportsId=${tournament?.sportsId}")
                                    selectedTournamentId      = tournament?.id
                                    selectedTournamentSportId = tournament?.sportsId
                                    isTournamentLocked = true
                                    applyAutoSportFromTournament(tournament?.sportsId)
                                    fetchMatches()
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                }
            }
            override fun onFailure(call: Call<List<TournamentResponse>>, t: Throwable) {
                toastLong("Failed to load tournaments")
            }
        })
    }

    private fun fetchAllTournamentsForSeason(seasonId: Long) {
        lifecycleScope.launch {
            try {
                val merged = withContext(Dispatchers.IO) {
                    val results = mutableListOf<TournamentResponse>()
                    for (sid in allSportIds) {
                        val response = api.getSeasonWiseTournaments(seasonId, sid).execute()
                        if (response.isSuccessful) {
                            val tournaments = response.body().orEmpty().map { tournament ->
                                if (tournament.sportsId == null) tournament.copy(sportsId = sid)
                                else tournament
                            }
                            results.addAll(tournaments)
                        }
                    }
                    results
                }

                tournamentList.clear()
                tournamentList.addAll(merged.distinctBy { it.id })

                val names = mutableListOf("All Tournaments") + tournamentList.map { it.name }
                val spinnerAdapter = ArrayAdapter(
                    this@MatchesDetailActivity,
                    android.R.layout.simple_spinner_item,
                    names
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                // 🔑 Same 3-step fix
                binding.spinnerTournament.onItemSelectedListener = null
                binding.spinnerTournament.adapter = spinnerAdapter
                binding.spinnerTournament.post {
                    binding.spinnerTournament.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                                if (pos == 0) {
                                    selectedTournamentId = null
                                    selectedTournamentSportId = null
                                    isTournamentLocked = false
                                    enableAllSportButtons()
                                    fetchMatches()
                                } else {
                                    val tournament = tournamentList.getOrNull(pos - 1)
                                    selectedTournamentId      = tournament?.id
                                    selectedTournamentSportId = tournament?.sportsId
                                    isTournamentLocked = true
                                    applyAutoSportFromTournament(tournament?.sportsId)
                                    fetchMatches()
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                }
            } catch (_: Exception) {
                toastLong("Failed to load tournaments")
            }
        }
    }

    // ─── Auto Sport Selection ─────────────────────────────────────────────────

    private fun applyAutoSportFromTournament(sportId: Long?) {
        android.util.Log.d("DEBUG_FLOW", "applyAutoSport CALLED with sportId=$sportId")

        if (sportId == null) {
            enableAllSportButtons()
            return
        }

        selectedSportId = sportId
        sportButtons.forEach { btn ->
            btn.isEnabled = false
            btn.alpha     = 0.4f
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(DKGRAY)
        }

        val matchedButton = sportIdToButtonMap[sportId]
        android.util.Log.d("DEBUG_FLOW", "matchedButton = $matchedButton (null matlab map mein nahi mila)")

        matchedButton?.let {
            it.isEnabled = true
            it.alpha     = 1f
            it.backgroundTintList = android.content.res.ColorStateList.valueOf("#E31212".toColorInt())
            android.util.Log.d("DEBUG_FLOW", "✅ Button RED set kiya sportId=$sportId")
        }

        selectedSport = sportIdToName[sportId]
    }


    private fun enableAllSportButtons() {
        android.util.Log.d("DEBUG_FLOW", "⚠️ enableAllSportButtons CALLED", Exception("stack trace"))
        sportButtons.forEach { btn ->
            btn.isEnabled = true
            btn.alpha     = 1f
        }
        // Reset to previously selected sport highlight
        highlightSelectedSport(
            when (selectedSport) {
                "Cricket"      -> binding.btnCricket
                "Futsal"       -> binding.btnFootball
                "VolleyBall"   -> binding.btnVolleyball
                "Table Tennis" -> binding.btnTableTennis
                "Badminton"    -> binding.btnBadminton
                "Ludo"         -> binding.btnLudo
                "TugOfWar"     -> binding.btnTugOfWar
                "Chess"        -> binding.btnChess
                "Hockey"       -> binding.btnHockey
                else           -> binding.btnAllSports
            }
        )
    }

    // ─── Sport Buttons ────────────────────────────────────────────────────────

    private fun setupSportButtons() {
        binding.btnAllSports.setOnClickListener {
            if (selectedTournamentId != null) return@setOnClickListener // locked by tournament
            selectedSport = null
            selectedSportId = null
            highlightSelectedSport(it)
            fetchMatches()
        }
        binding.btnCricket.setOnClickListener      { onSportSelected("Cricket",      1L, it) }
        binding.btnFootball.setOnClickListener     { onSportSelected("Futsal",       2L, it) }
        binding.btnVolleyball.setOnClickListener   { onSportSelected("VolleyBall",   3L, it) }
        binding.btnBadminton.setOnClickListener    { onSportSelected("Badminton",    5L, it) }
        binding.btnTableTennis.setOnClickListener  { onSportSelected("Table Tennis", 4L, it) }
        binding.btnTugOfWar.setOnClickListener     { onSportSelected("TugOfWar",     7L, it) }
        binding.btnLudo.setOnClickListener         { onSportSelected("Ludo",         6L, it) }
        binding.btnChess.setOnClickListener        { onSportSelected("Chess",        8L, it) }
        binding.btnHockey.setOnClickListener       { onSportSelected("Hockey",       9L, it) }

        highlightSelectedSport(binding.btnAllSports)
    }

    private fun onSportSelected(sport: String, sportId: Long, view: View) {
        if (selectedTournamentId != null) return // sport locked by tournament
        selectedSport = sport
        selectedSportId = sportId
        highlightSelectedSport(view)
        selectedSeasonId?.let { fetchTournamentsForSeason(it, sportId) }
        fetchMatches()
    }

    // ─── Status Buttons ───────────────────────────────────────────────────────

    private fun setupStatusButtons() {
        binding.btnAllMatches.setOnClickListener { onStatusSelected("ALL", it) }
        binding.btnLive.setOnClickListener       { onStatusSelected("LIVE", it) }
        binding.btnUpcoming.setOnClickListener   { onStatusSelected("UPCOMING", it) }
        binding.btnCompleted.setOnClickListener  { onStatusSelected("COMPLETED", it) }
        highlightSelectedStatus(binding.btnAllMatches)
    }

    private fun onStatusSelected(status: String, view: View) {
        selectedStatus = status
        highlightSelectedStatus(view)
        fetchMatches()
    }

    // ─── Sort ─────────────────────────────────────────────────────────────────

    private fun setupSortButton() {
        updateSortButtonLabel()
        binding.btnSortDate.setOnClickListener {
            newestFirst = !newestFirst
            updateSortButtonLabel()
            fetchMatches()
        }
    }

    // ─── Fetch Matches ────────────────────────────────────────────────────────

    private fun fetchMatches() {
        showLoading(true)

        if (selectedTournamentId != null) {
            lifecycleScope.launch {
                try {
                    val response = api.getMatchesByTournament(selectedTournamentId!!)
                    if (response.isSuccessful) {
                        var list = response.body().orEmpty().map { it.toMatchResponse() }
                        if (selectedStatus != "ALL") {
                            list = list.filter { it.status?.uppercase() == selectedStatus }
                        }
                        adapter.setItems(sortMatchesByDate(list))
                    } else {
                        adapter.setItems(emptyList()) // ✅ Clear karo error pe
                        toastLong(NetworkUi.userMessage(response, "Failed to load matches"))
                    }
                } catch (t: Throwable) {
                    adapter.setItems(emptyList()) // ✅ Clear karo exception pe
                    toastLong(NetworkUi.userMessage(t))
                } finally {
                    showLoading(false)
                    if (isTournamentLocked) {
                        reapplyTournamentSportHighlight()
                    }
                    setEmptyStateMessage()
                    checkEmptyState()
                }
            }
            return
        }

        api.getMatchesBySport(selectedSport, selectedStatus).enqueue(matchCallback)
    }

    private val matchCallback = object : Callback<List<MatchResponse>> {
        override fun onResponse(
            call: Call<List<MatchResponse>>,
            response: Response<List<MatchResponse>>
        ) {
            showLoading(false)
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                adapter.setItems(sortMatchesByDate(list))
            } else {
                toastLong(NetworkUi.userMessage(response, "Failed to load matches"))
            }
            setEmptyStateMessage()
            checkEmptyState()
        }
        override fun onFailure(call: Call<List<MatchResponse>>, t: Throwable) {
            showLoading(false)
            toastLong(NetworkUi.userMessage(t))
            setEmptyStateMessage()
            checkEmptyState()
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun FixturesResponse.toMatchResponse(): MatchResponse {
        return MatchResponse(
            id = id,
            tournamentId = tournamentId,
            tournamentName = tournamentName,
            team1Id = team1Id,
            team1Name = team1Name,
            team2Id = team2Id,
            team2Name = team2Name,
            scorerId = scorerId,
            status = status,
            venue = venue,
            date = date,
            time = time,
            sportId = sportId,
            overs = overs
        )
    }

    private fun sortMatchesByDate(matches: List<MatchResponse>): List<MatchResponse> {
        val comparator = if (newestFirst) {
            compareByDescending<MatchResponse> { parseMatchDateTime(it) ?: Long.MIN_VALUE }
                .thenByDescending { it.id ?: Long.MIN_VALUE }
        } else {
            compareBy<MatchResponse> { parseMatchDateTime(it) ?: Long.MAX_VALUE }
                .thenBy { it.id ?: Long.MAX_VALUE }
        }
        return matches.sortedWith(comparator)
    }

    private fun parseMatchDateTime(match: MatchResponse): Long? {
        val dateText = match.date?.trim().orEmpty()
        if (dateText.isEmpty()) return null
        val timeText = match.time?.trim().orEmpty().ifEmpty { "00:00:00" }
        val candidates = listOf(
            "$dateText $timeText" to "yyyy-MM-dd HH:mm:ss",
            "$dateText $timeText" to "yyyy-MM-dd HH:mm",
            dateText              to "yyyy-MM-dd"
        )
        for ((value, pattern) in candidates) {
            val parsed = try {
                SimpleDateFormat(pattern, Locale.getDefault())
                    .apply { isLenient = false }.parse(value)
            } catch (_: Exception) { null }
            if (parsed != null) return parsed.time
        }
        return null
    }

    private fun updateSortButtonLabel() {
        binding.btnSortDate.text = if (newestFirst) "Newest First" else "Oldest First"
    }

    private fun showLoading(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun checkEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.rvMatches.visibility    = if (isEmpty) View.GONE    else View.VISIBLE
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setEmptyStateMessage() {
        binding.tvEmptyState.text = if (selectedTournamentId != null) {
            "No matches found for this tournament"
        } else {
            "No Matches Available"
        }
    }

    private fun reapplyTournamentSportHighlight() {
        if (isTournamentLocked && selectedTournamentSportId != null) {
            applyAutoSportFromTournament(selectedTournamentSportId)
        }
    }

    private fun highlightSelectedSport(selected: View) {
        sportButtons.forEach { btn ->
            if (btn.isEnabled) {
                btn.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(DKGRAY)
            }
        }
        (selected as? com.google.android.material.button.MaterialButton)
            ?.backgroundTintList =
            android.content.res.ColorStateList.valueOf("#E31212".toColorInt())
    }

    private fun highlightSelectedStatus(selected: View) {
        listOf(
            binding.btnAllMatches,
            binding.btnLive,
            binding.btnUpcoming,
            binding.btnCompleted
        ).forEach {
            it.backgroundTintList =
                android.content.res.ColorStateList.valueOf(DKGRAY)
        }
        (selected as? com.google.android.material.button.MaterialButton)
            ?.backgroundTintList =
            android.content.res.ColorStateList.valueOf("#E31212".toColorInt())
    }

    override fun onResume() {
        super.onResume()
        fetchMatches()
    }
}