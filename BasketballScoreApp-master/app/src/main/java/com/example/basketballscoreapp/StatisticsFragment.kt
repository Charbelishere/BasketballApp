package com.example.basketballscoreapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.basketballscoreapp.databinding.FragmentStatisticsBinding

// StatisticsFragment displays game statistics and allows users to view details of previous games.
class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null  // Binding for the fragment's views
    private val binding get() = _binding!!  // Safe access to the binding
    private lateinit var viewModel: GameViewModel  // ViewModel to handle game data
    private lateinit var dbHelper: BasketballDatabaseHelper  // Database helper to retrieve games from the database

    // Inflate the fragment layout and bind the views
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)  // Bind the fragment's layout
        return binding.root  // Return the root view
    }

    // Initialize the fragment's views and set up logic to display game statistics
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the ViewModel and Database Helper
        viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        dbHelper = BasketballDatabaseHelper(requireContext())

        // Load games from the database
        val games = dbHelper.getAllGames()
        Log.d("StatisticsFragment", "Fetched Games: $games")

        if (games.isNotEmpty()) {
            // If games are available, populate the dropdown (spinner) with game titles
            setupGamesDropdown(games)
        } else {
            // If no games are found, display a message
            showNoGamesMessage()
        }

        // Back button logic to navigate back to the Setup screen
        binding.backToSetupButton.setOnClickListener {
            findNavController().navigate(R.id.action_statisticsFragment_to_setupFragment)
        }
    }

    // Set up the games dropdown with game titles (team names)
    private fun setupGamesDropdown(games: List<Game>) {
        // Create a list of game titles for the dropdown (e.g., "Team A vs Team B")
        val gameTitles = games.map { "${it.teamA} vs ${it.teamB}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, gameTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.gamesDropdown.adapter = adapter  // Set the adapter to the spinner

        // By default, display the statistics of the first game in the list
        if (games.isNotEmpty()) {
            displayGameStatistics(games.first())  // Display statistics of the first game
        }

        // When a new game is selected, update the displayed statistics
        binding.gamesDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Display the selected game's statistics
                displayGameStatistics(games[position])
                displayAverageStats(games[position])  // Also display average stats for the selected game
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing if no item is selected
            }
        }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    // Display the full statistics for a selected game
    private fun displayGameStatistics(game: Game) {
        Log.d("StatisticsFragment", "Displaying statistics for game: $game")

        if (game.quarterStats.isEmpty()) {
            // If no quarter stats are available, show a message
            showNoGamesMessage()
            return
        }

        // Calculate total scores, fouls, and point difference for the game
        val teamAScoreTotal = game.quarterStats.sumOf { it.teamAScore }
        val teamBScoreTotal = game.quarterStats.sumOf { it.teamBScore }
        val teamAFoulsTotal = game.quarterStats.sumOf { it.teamAFouls }
        val teamBFoulsTotal = game.quarterStats.sumOf { it.teamBFouls }
        val pointDifference = kotlin.math.abs(teamAScoreTotal - teamBScoreTotal)

        // Determine the winner of the game
        val winner = when {
            teamAScoreTotal > teamBScoreTotal -> "${game.teamA} Wins!"
            teamBScoreTotal > teamAScoreTotal -> "${game.teamB} Wins!"
            else -> "It's a Draw!"
        }

        // Update UI with the winner of the game
        binding.winnerTextView.text = winner

        // Display full game statistics (scores, fouls, and point difference)
        binding.cumulativeStatsTextView.text = getString(
            R.string.full_game_stats_placeholder,
            game.teamA,
            teamAScoreTotal,
            teamAFoulsTotal,
            game.teamB,
            teamBScoreTotal,
            teamBFoulsTotal,
            pointDifference
        )

        // Display total time played in minutes and seconds
        val totalTimePlayedInSeconds = game.totalTimePlayed / 1000  // Convert time to seconds
        val minutes = totalTimePlayedInSeconds / 60
        val seconds = totalTimePlayedInSeconds % 60
        binding.totalTimePlayedTextView.text = String.format("%02d:%02d", minutes, seconds)

        // Set up the toggle button to show/hide quarter details
        binding.viewDetailsButton.setOnClickListener {
            toggleQuarterDetails(game.quarterStats)  // Toggle details visibility
        }
    }

    // Toggle the visibility of quarter details (points and fouls for each quarter)
    private fun toggleQuarterDetails(quarterStats: List<QuarterStats>) {
        if (quarterStats.isEmpty()) {
            binding.teamAStatsTextView.text = getString(R.string.no_stats_available)
            binding.teamBStatsTextView.text = getString(R.string.no_stats_available)
            return
        }

        // If quarter details are hidden, show them
        if (binding.quarterDetailsLayout.visibility == View.GONE) {
            binding.quarterDetailsLayout.visibility = View.VISIBLE
            binding.viewDetailsButton.text = getString(R.string.hide_details)

            // Populate the quarter details dynamically
            binding.teamAStatsTextView.text = quarterStats.joinToString("\n") { quarter ->
                "Quarter ${quarter.quarterNumber}: Points: ${quarter.teamAScore}, Fouls: ${quarter.teamAFouls}"
            }
            binding.teamBStatsTextView.text = quarterStats.joinToString("\n") { quarter ->
                "Quarter ${quarter.quarterNumber}: Points: ${quarter.teamBScore}, Fouls: ${quarter.teamBFouls}"
            }
        } else {
            // Hide quarter details and update the button text
            binding.quarterDetailsLayout.visibility = View.GONE
            binding.viewDetailsButton.text = getString(R.string.view_details)
        }
    }

    // Show a message when no games are found in the database
    private fun showNoGamesMessage() {
        binding.winnerTextView.text = getString(R.string.no_games_found)
        binding.cumulativeStatsTextView.text = ""
        binding.totalTimePlayedTextView.text = ""
        binding.quarterDetailsLayout.visibility = View.GONE
        binding.viewDetailsButton.visibility = View.GONE
    }

    // Display average statistics (points and fouls per game) for the selected game
    private fun displayAverageStats(game: Game) {
        viewModel.updateGameData(game)
        val averages = viewModel.getAverageStats()
        val teamAAverages = averages.first
        val teamBAverages = averages.second

        val averageStatsText = """
            Team A - Avg Points: ${teamAAverages.first}, Avg Fouls: ${teamAAverages.second}
            Team B - Avg Points: ${teamBAverages.first}, Avg Fouls: ${teamBAverages.second}
        """.trimIndent()

        binding.averageStatsTextView.text = averageStatsText  // Display the average stats
    }

    // Clean up binding when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Set the binding to null to avoid memory leaks
    }
}
