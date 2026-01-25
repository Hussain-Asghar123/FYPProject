import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.TournamentStatsAdapter
import com.example.fypproject.DTO.TournamentStatsDto
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch

class StatsFragment : Fragment(R.layout.fragment_stats) {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private var tournamentId: Long = -1L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)

        tournamentId = arguments?.getLong("tournamentId") ?: -1L

        if (tournamentId != -1L) {
            fetchTournamentStats(tournamentId)
        } else {
            Toast.makeText(context, "Invalid Tournament ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchTournamentStats(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response =api.getTournamentStats(id)

                populateUI(response)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateUI(stats: TournamentStatsDto) {

        binding.tvManOfTournament.text = stats.manOfTournamentName ?: "N/A"

        binding.cardBestBatsman.apply {
            tvLabel.text = "Best Batsman"
            tvPlayerName.text = stats.bestBatsmanName ?: "N/A"
            tvValue.text = "${stats.bestBatsmanRuns ?: 0} runs"
        }

        binding.cardBestBowler.apply {
            tvLabel.text = "Best Bowler"
            tvPlayerName.text = stats.bestBowlerName ?: "N/A"
            tvValue.text = "${stats.bestBowlerWickets ?: 0} wickets"
        }

        binding.cardHighestScore.apply {
            tvLabel.text = "Highest Score"
            tvPlayerName.text = stats.highestScorerName ?: "N/A"
            tvValue.text = "${stats.highestRuns ?: 0} runs"
        }

        binding.rvTopBatsmen.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(stats.topBatsmen, isBatting = true)
        }

        binding.rvTopBowlers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentStatsAdapter(stats.topBowlers, isBatting = false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tournamentId: Long): StatsFragment {
            val fragment = StatsFragment()
            val args = Bundle()
            args.putLong("tournamentId", tournamentId)
            fragment.arguments = args
            return fragment
        }
    }
}