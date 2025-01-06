package com.example.basketballscoreapp

import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.basketballscoreapp.databinding.FragmentGameBinding

// The GameFragment is responsible for managing the game UI and game logic.
class GameFragment : Fragment() {
    // Private properties for binding and the ViewModel
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GameViewModel

    // Timer related variables
    private var shotClockTimer: CountDownTimer? = null  // Timer for shot clock (24 seconds)
    private var startTime: Long = 0L  // The start time for the main timer
    private var pausedTime: Long = 0L  // Time when the timer is paused
    private var isTimerRunning = false  // Flag to check if the timer is running
    private var totalQuarters = 4  // The total number of quarters in the game
    private var timePerQuarter = 60000L // Time per quarter in milliseconds (default 1 minute)

    // Inflate the fragment layout and bind views to variables
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Initialize the ViewModel and UI components when the fragment view is created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the ViewModel to manage the game state
        viewModel = ViewModelProvider(this)[GameViewModel::class.java]

        // Get game details (team names, number of quarters, and time per quarter) from the arguments
        val teamA = arguments?.getString("teamA") ?: "Team A"
        val teamB = arguments?.getString("teamB") ?: "Team B"
        totalQuarters = arguments?.getInt("quarters") ?: 4
        timePerQuarter = (arguments?.getInt("timePerQuarter") ?: 10) * 60000L // Convert to milliseconds

        // Set initial UI values (team names, current quarter, and timer)
        binding.teamAName.text = teamA
        binding.teamBName.text = teamB
        binding.currentQuarter.text = "Quarter: ${viewModel.currentQuarter.value ?: 1}"
        binding.timer.text = formatTime(timePerQuarter) // Display the initial timer
        binding.shotClockTimer.text = "24" // Display the shot clock

        // Observe LiveData from ViewModel to update the UI in real-time
        observeLiveData()

        // Timer control buttons for pausing and continuing the main timer
        binding.pauseTimerButton.setOnClickListener { pauseMainTimer() }
        binding.continueTimerButton.setOnClickListener { continueMainTimer() }
        startMainTimer()  // Start the main timer

        // Shot clock logic (reset and start the shot clock)
        binding.resetShotClockButton.setOnClickListener { resetShotClock() }
        startShotClock()

        // Set up Team A buttons for score and foul manipulation
        setupTeamAButtons()

        // Set up Team B buttons for score and foul manipulation
        setupTeamBButtons()

        // Logic for transitioning to the next quarter
        binding.startNextQuarterButton.setOnClickListener {
            startNextQuarter()
        }

        // End the game and save data when clicked
        binding.endGameButton.setOnClickListener {
            endGame(teamA, teamB)
        }

        // Navigate to statistics screen when clicked
        binding.viewStatisticsButton.setOnClickListener {
            navigateToStatistics()
        }
    }

    // Observe LiveData from the ViewModel to update the UI with current game data
    private fun observeLiveData() {
        viewModel.teamAScore.observe(viewLifecycleOwner) { score ->
            binding.teamAScore.text = score?.toString() ?: "0"
        }
        viewModel.teamBScore.observe(viewLifecycleOwner) { score ->
            binding.teamBScore.text = score?.toString() ?: "0"
        }
        viewModel.teamAFouls.observe(viewLifecycleOwner) { fouls ->
            binding.teamAFouls.text = "Fouls: ${fouls ?: 0}"
        }
        viewModel.teamBFouls.observe(viewLifecycleOwner) { fouls ->
            binding.teamBFouls.text = "Fouls: ${fouls ?: 0}"
        }
        viewModel.currentQuarter.observe(viewLifecycleOwner) { quarter ->
            binding.currentQuarter.text = "Quarter: ${quarter ?: 1}"
        }
    }

    // Setup buttons for Team A's score and fouls
    private fun setupTeamAButtons() {
        binding.plusOneTeamA.setOnClickListener { viewModel.incrementScore("A", 1) }
        binding.plusTwoTeamA.setOnClickListener { viewModel.incrementScore("A", 2) }
        binding.plusThreeTeamA.setOnClickListener { viewModel.incrementScore("A", 3) }
        binding.minusOneTeamA.setOnClickListener { viewModel.incrementScore("A", -1) }
        binding.incrementTeamAFoul.setOnClickListener { viewModel.incrementFouls("A") }
    }

    // Setup buttons for Team B's score and fouls
    private fun setupTeamBButtons() {
        binding.plusOneTeamB.setOnClickListener { viewModel.incrementScore("B", 1) }
        binding.plusTwoTeamB.setOnClickListener { viewModel.incrementScore("B", 2) }
        binding.plusThreeTeamB.setOnClickListener { viewModel.incrementScore("B", 3) }
        binding.minusOneTeamB.setOnClickListener { viewModel.incrementScore("B", -1) }
        binding.incrementTeamBFoul.setOnClickListener { viewModel.incrementFouls("B") }
    }

    // Start the main game timer
    private fun startMainTimer() {
        startTime = SystemClock.elapsedRealtime()
        isTimerRunning = true
        binding.timer.post(updateTimerRunnable)
    }

    // Pause the main timer and stop the shot clock
    private fun pauseMainTimer() {
        pausedTime += SystemClock.elapsedRealtime() - startTime
        isTimerRunning = false
        binding.timer.removeCallbacks(updateTimerRunnable)
        shotClockTimer?.cancel()
    }

    // Continue the main timer after being paused
    private fun continueMainTimer() {
        startTime = SystemClock.elapsedRealtime()
        isTimerRunning = true
        binding.timer.post(updateTimerRunnable)
        startShotClock()
    }

    // Runnable for updating the main timer every second
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val elapsed = SystemClock.elapsedRealtime() - startTime + pausedTime
                val remaining = timePerQuarter - elapsed
                if (remaining > 0) {
                    binding.timer.text = formatTime(remaining)
                    binding.timer.postDelayed(this, 1000) // Update every second
                } else {
                    binding.timer.text = formatTime(0)
                    isTimerRunning = false
                    endQuarter()  // End the quarter when time runs out
                }
            }
        }
    }

    // End the current quarter and either transition to the next quarter or end the game
    private fun endQuarter() {
        viewModel.updateTimePlayed(SystemClock.elapsedRealtime() - startTime + pausedTime)
        pausedTime = 0L
        isTimerRunning = false
        shotClockTimer?.cancel()

        // Check if the game is over, or if there are more quarters to play
        if ((viewModel.currentQuarter.value ?: 1) >= totalQuarters) {
            endGame(binding.teamAName.text.toString(), binding.teamBName.text.toString())
        } else {
            binding.nextQuarterText.visibility = View.VISIBLE
            binding.startNextQuarterButton.visibility = View.VISIBLE
        }
    }

    // Start the next quarter, reset the timer, and shot clock
    private fun startNextQuarter() {
        binding.nextQuarterText.visibility = View.GONE
        binding.startNextQuarterButton.visibility = View.GONE

        if ((viewModel.currentQuarter.value ?: 1) < totalQuarters) {
            viewModel.nextQuarter(SystemClock.elapsedRealtime() - startTime + pausedTime)
            startMainTimer()
            resetShotClock()
        } else {
            endGame(binding.teamAName.text.toString(), binding.teamBName.text.toString())
        }
    }

    // End the game, save data, and navigate to the statistics screen
    private fun endGame(teamA: String, teamB: String) {
        viewModel.updateTimePlayed(SystemClock.elapsedRealtime() - startTime + pausedTime)
        viewModel.saveGameToDatabase(requireContext(), teamA, teamB)
        Toast.makeText(requireContext(), "Game saved successfully!", Toast.LENGTH_SHORT).show()

        binding.nextQuarterText.visibility = View.GONE
        binding.startNextQuarterButton.visibility = View.GONE

        navigateToStatistics()
    }

    // Navigate to the statistics screen, passing relevant stats
    private fun navigateToStatistics() {
        val winner = viewModel.getWinner()
        val action = GameFragmentDirections.actionGameFragmentToStatisticsFragment(
            teamAStats = viewModel.getTeamAQuarterStats(),
            teamBStats = viewModel.getTeamBQuarterStats(),
            winner = winner
        )
        findNavController().navigate(action)
    }

    // Reset the shot clock to 24 seconds
    private fun resetShotClock() {
        shotClockTimer?.cancel()
        binding.shotClockTimer.text = "24"
        startShotClock()
    }

    // Start the shot clock with a countdown of 24 seconds
    private fun startShotClock() {
        shotClockTimer?.cancel()
        shotClockTimer = object : CountDownTimer(24000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.shotClockTimer.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                binding.shotClockTimer.text = "24"
                startShotClock()
            }
        }.start()
    }

    // Format the time in MM:SS format
    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // Clean up resources when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        isTimerRunning = false
        binding.timer.removeCallbacks(updateTimerRunnable)
        shotClockTimer?.cancel()
        _binding = null
    }
}
