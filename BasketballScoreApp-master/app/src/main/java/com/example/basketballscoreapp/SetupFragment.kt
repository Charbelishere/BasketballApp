package com.example.basketballscoreapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.basketballscoreapp.databinding.FragmentSetupBinding

// SetupFragment is responsible for setting up the initial game settings like team names, number of quarters, and time per quarter.
class SetupFragment : Fragment() {
    private var _binding: FragmentSetupBinding? = null  // Binding for accessing the views in the layout
    private val binding get() = _binding!!  // Safe access to the binding

    // Inflate the fragment layout and bind views
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)  // Bind the fragment layout
        return binding.root  // Return the root view
    }

    // Initialize the fragment's views and set up logic for the setup screen
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup the quarters spinner (dropdown) with options 1 to 4 quarters
        val quartersAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf(1, 2, 3, 4) // The number of quarters can be selected from 1 to 4
        )
        quartersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.quartersSpinner.adapter = quartersAdapter  // Set the adapter to the spinner

        // Logic for the Start Game button
        binding.startButton.setOnClickListener {
            // Retrieve the values entered by the user for team names, number of quarters, and time per quarter
            val teamAName = binding.teamAInput.text.toString().trim()  // Team A name input
            val teamBName = binding.teamBInput.text.toString().trim()  // Team B name input
            val quarters = binding.quartersSpinner.selectedItem.toString().toInt()  // Number of quarters selected
            val timePerQuarter = binding.timePerQuarterInput.text.toString().toIntOrNull() ?: 10  // Default to 10 minutes if input is invalid

            // Validate that both team names are entered
            if (teamAName.isEmpty() || teamBName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both team names.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener  // If validation fails, show a toast and exit
            }

            // Create a new Game object with the data provided
            val game = Game(
                id = null,  // Game ID is initially null; it will be set when the game is saved in the database
                teamA = teamAName,
                teamB = teamBName,
                totalQuarters = quarters,
                timePerQuarterInMinutes = timePerQuarter,
                totalTimePlayed = 0,  // Total time played is 0 initially
                quarterStats = generateInitialQuarterStats(quarters)  // Generate initial stats for each quarter
            )

            // Save the game to the database using the helper class
            val dbHelper = BasketballDatabaseHelper(requireContext())
            val gameId = dbHelper.insertGame(game)

            // Navigate to the GameFragment, passing the necessary data (team names, quarters, time per quarter)
            val action = SetupFragmentDirections.actionSetupFragmentToGameFragment(
                teamA = teamAName,
                teamB = teamBName,
                quarters = quarters,
                timePerQuarter = timePerQuarter
            )
            findNavController().navigate(action)  // Perform the navigation to the GameFragment
        }

        // Navigate to the statistics screen when the View Statistics button is clicked
        binding.viewStatisticsButton.setOnClickListener {
            findNavController().navigate(R.id.action_setupFragment_to_statisticsFragment)
        }
    }

    // Generate initial quarter stats based on the number of quarters selected
    private fun generateInitialQuarterStats(totalQuarters: Int): List<QuarterStats> {
        val quarterStats = mutableListOf<QuarterStats>()
        for (quarter in 1..totalQuarters) {
            // Add stats for each quarter with default values (0 scores and fouls)
            quarterStats.add(
                QuarterStats(
                    id = null,  // ID is initially null; it will be assigned later
                    gameId = -1,  // Placeholder game ID; will be updated when saved to the database
                    quarterNumber = quarter,  // Set the current quarter number
                    teamAScore = 0,  // Initial score for Team A
                    teamBScore = 0,  // Initial score for Team B
                    teamAFouls = 0,  // Initial fouls for Team A
                    teamBFouls = 0  // Initial fouls for Team B
                )
            )
        }
        return quarterStats  // Return the list of initialized quarter stats
    }

    // Clean up the binding when the view is destroyed to avoid memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Set the binding to null
    }
}
