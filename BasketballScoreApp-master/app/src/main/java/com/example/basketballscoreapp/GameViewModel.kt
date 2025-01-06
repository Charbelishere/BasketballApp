package com.example.basketballscoreapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.basketballscoreapp.BasketballDatabaseHelper.Companion.COLUMN_GAME_ID_FOREIGN
import com.example.basketballscoreapp.BasketballDatabaseHelper.Companion.COLUMN_TEAM_A_FOULS
import com.example.basketballscoreapp.BasketballDatabaseHelper.Companion.COLUMN_TEAM_A_SCORE
import com.example.basketballscoreapp.BasketballDatabaseHelper.Companion.COLUMN_TEAM_B_FOULS
import com.example.basketballscoreapp.BasketballDatabaseHelper.Companion.COLUMN_TEAM_B_SCORE
import com.example.basketballscoreapp.BasketballDatabaseHelper.Companion.TABLE_QUARTER_STATS

// ViewModel class for managing the game's state and providing data to the UI.
class GameViewModel : ViewModel() {

    // MutableLiveData variables to hold the scores, fouls, and current quarter
    private val _teamAScore = MutableLiveData(0)
    val teamAScore: LiveData<Int> get() = _teamAScore

    private val _teamBScore = MutableLiveData(0)
    val teamBScore: LiveData<Int> get() = _teamBScore

    private val _teamAFouls = MutableLiveData(0)
    val teamAFouls: LiveData<Int> get() = _teamAFouls

    private val _teamBFouls = MutableLiveData(0)
    val teamBFouls: LiveData<Int> get() = _teamBFouls

    private val _currentQuarter = MutableLiveData(1)
    val currentQuarter: LiveData<Int> get() = _currentQuarter

    // MutableLiveData to store the total number of quarters
    private val _totalQuarters = MutableLiveData(4)
    val totalQuarters: LiveData<Int> get() = _totalQuarters

    // Function to set the total number of quarters
    fun setTotalQuarters(value: Int) {
        _totalQuarters.value = value
    }

    // Total time played (in milliseconds)
    private val _totalTimePlayed = MutableLiveData(0L)
    private val totalTimePlayed: LiveData<Long> get() = _totalTimePlayed

    // List to store quarter stats during the game
    private val quarterStats = mutableListOf<QuarterStats>()

    // MutableLiveData variables for team names
    private val teamAName = MutableLiveData("Team A")
    private val teamBName = MutableLiveData("Team B")

    // Function to increment the score of a team by a specified number of points
    fun incrementScore(team: String, points: Int) {
        when (team.uppercase()) {
            "A" -> _teamAScore.value = (_teamAScore.value ?: 0) + points
            "B" -> _teamBScore.value = (_teamBScore.value ?: 0) + points
        }
    }

    // Function to increment the fouls of a team by 1
    fun incrementFouls(team: String) {
        when (team.uppercase()) {
            "A" -> _teamAFouls.value = (_teamAFouls.value ?: 0) + 1
            "B" -> _teamBFouls.value = (_teamBFouls.value ?: 0) + 1
        }
    }

    // Function to update the total time played (accumulating the elapsed time)
    fun updateTimePlayed(elapsedTime: Long) {
        _totalTimePlayed.value = (_totalTimePlayed.value ?: 0L) + elapsedTime
    }

    // Function to save the stats of the current quarter to the database
    fun saveQuarterStats(db: SQLiteDatabase, gameId: Int, quarterStat: QuarterStats) {
        if (gameId <= 0) {
            Log.e("GameViewModel", "Invalid gameId provided: $gameId")
            return
        }

        // Check if the stats for the current quarter already exist in the database
        val existingStats = db.rawQuery(
            "SELECT * FROM $TABLE_QUARTER_STATS WHERE $COLUMN_GAME_ID_FOREIGN = ? AND ${BasketballDatabaseHelper.COLUMN_QUARTER_NUMBER} = ?",
            arrayOf(gameId.toString(), quarterStat.quarterNumber.toString())
        )
        if (existingStats.moveToFirst()) {
            Log.w("GameViewModel", "Quarter ${quarterStat.quarterNumber} for gameId $gameId already exists. Skipping save.")
            existingStats.close()
            return
        }
        existingStats.close()

        // Save the quarter stats if they do not already exist
        val contentValues = ContentValues().apply {
            put(COLUMN_GAME_ID_FOREIGN, gameId)
            put(BasketballDatabaseHelper.COLUMN_QUARTER_NUMBER, quarterStat.quarterNumber)
            put(COLUMN_TEAM_A_SCORE, quarterStat.teamAScore)
            put(COLUMN_TEAM_B_SCORE, quarterStat.teamBScore)
            put(COLUMN_TEAM_A_FOULS, quarterStat.teamAFouls)
            put(COLUMN_TEAM_B_FOULS, quarterStat.teamBFouls)
        }

        db.insert(TABLE_QUARTER_STATS, null, contentValues)
        Log.d("GameViewModel", "Quarter stats saved: $quarterStat")
    }

    // Function to handle the transition to the next quarter
    fun nextQuarter(elapsedTime: Long) {
        updateTimePlayed(elapsedTime)

        // Create and save stats for the current quarter
        val currentStats = QuarterStats(
            gameId = 0, // Placeholder; updated when saving to DB
            quarterNumber = _currentQuarter.value ?: 1,
            teamAScore = _teamAScore.value ?: 0,
            teamBScore = _teamBScore.value ?: 0,
            teamAFouls = _teamAFouls.value ?: 0,
            teamBFouls = _teamBFouls.value ?: 0
        )

        // Add the stats to the list and log for debugging
        quarterStats.add(currentStats)
        Log.d("GameViewModel", "Quarter Stats Added: $currentStats")

        // Increment the quarter number
        _currentQuarter.value = (_currentQuarter.value ?: 1) + 1

        // Reset scores and fouls for the next quarter
        _teamAScore.value = 0
        _teamBScore.value = 0
        _teamAFouls.value = 0
        _teamBFouls.value = 0
    }

    // Function to save the stats of the final quarter if needed
    fun saveFinalQuarterIfNeeded() {
        if (_teamAScore.value != 0 || _teamBScore.value != 0 || _teamAFouls.value != 0 || _teamBFouls.value != 0) {
            val currentStats = QuarterStats(
                gameId = 0,
                quarterNumber = _currentQuarter.value ?: 1,
                teamAScore = _teamAScore.value ?: 0,
                teamBScore = _teamBScore.value ?: 0,
                teamAFouls = _teamAFouls.value ?: 0,
                teamBFouls = _teamBFouls.value ?: 0
            )
            quarterStats.add(currentStats)
            Log.d("GameViewModel", "Final Quarter Stats Added: $currentStats")
        }
    }

    // Function to save the game and all its quarter stats to the database
    fun saveGameToDatabase(context: Context, teamA: String, teamB: String) {
        saveFinalQuarterIfNeeded() // Save any unsaved quarter data
        val dbHelper = BasketballDatabaseHelper(context)
        val game = Game(
            teamA = teamA,
            teamB = teamB,
            totalQuarters = _totalQuarters.value ?: 4,
            timePerQuarterInMinutes = 10,
            totalTimePlayed = totalTimePlayed.value ?: 0L,
            quarterStats = quarterStats
        )

        Log.d("GameViewModel", "Saving game: $game")
        val gameId = dbHelper.insertGame(game).toInt()

        val db = dbHelper.writableDatabase
        quarterStats.forEach { stats ->
            saveQuarterStats(db, gameId, stats.copy(gameId = gameId))
        }
        Log.d("GameViewModel", "All quarter stats saved for gameId: $gameId")
    }

    // Function to determine the winner of the game based on the total scores
    fun getWinner(): String {
        val teamATotalScore = quarterStats.sumOf { it.teamAScore }
        val teamBTotalScore = quarterStats.sumOf { it.teamBScore }
        return when {
            teamATotalScore > teamBTotalScore -> "${teamAName.value} Wins! (+${teamATotalScore - teamBTotalScore})"
            teamATotalScore < teamBTotalScore -> "${teamBName.value} Wins! (+${teamBTotalScore - teamATotalScore})"
            else -> "It's a Draw!"
        }
    }

    // Function to retrieve Team A's stats by quarter
    fun getTeamAQuarterStats(): String {
        return quarterStats.joinToString("\n") {
            "Quarter ${it.quarterNumber}: ${it.teamAScore} pts, ${it.teamAFouls} fouls"
        }
    }

    // Function to retrieve Team B's stats by quarter
    fun getTeamBQuarterStats(): String {
        return quarterStats.joinToString("\n") {
            "Quarter ${it.quarterNumber}: ${it.teamBScore} pts, ${it.teamBFouls} fouls"
        }
    }

    // Function to calculate average stats for both teams
    fun getAverageStats(): Pair<Pair<Any, Any>, Pair<Any, Any>> {
        Log.d("GameViewModel", "QuarterStats: $quarterStats")
        if (quarterStats.isEmpty()) {
            return Pair(Pair(0, 0), Pair(0, 0))
        }

        val teamAAverage = Pair(
            (quarterStats.sumOf { it.teamAScore }.toDouble() / quarterStats.size).toInt(),
            (quarterStats.sumOf { it.teamAFouls }.toDouble() / quarterStats.size).toInt()
        )
        val teamBAverage = Pair(
            (quarterStats.sumOf { it.teamBScore }.toDouble() / quarterStats.size).toInt(),
            (quarterStats.sumOf { it.teamBFouls }.toDouble() / quarterStats.size).toInt()
        )

        return Pair(teamAAverage, teamBAverage)
    }

    // Function to update the game data (e.g., when resuming a game from saved data)
    fun updateGameData(game: Game) {
        // Update team names, total time played, current quarter, and stats
        teamAName.value = game.teamA
        teamBName.value = game.teamB
        _totalTimePlayed.value = game.totalTimePlayed
        _currentQuarter.value = game.quarterStats.maxOfOrNull { it.quarterNumber } ?: 1

        // Update scores and fouls for the first quarter (optional)
        if (game.quarterStats.isNotEmpty()) {
            val firstQuarterStats = game.quarterStats.first()
            _teamAScore.value = firstQuarterStats.teamAScore
            _teamBScore.value = firstQuarterStats.teamBScore
            _teamAFouls.value = firstQuarterStats.teamAFouls
            _teamBFouls.value = firstQuarterStats.teamBFouls
        }

        // Clear and re-add all quarter stats
        quarterStats.clear()
        quarterStats.addAll(game.quarterStats)
    }
}
