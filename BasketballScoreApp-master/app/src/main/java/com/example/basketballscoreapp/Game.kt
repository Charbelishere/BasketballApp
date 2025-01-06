package com.example.basketballscoreapp

import android.database.Cursor

// Data class representing a Game with its attributes
data class Game(
    val id: Int? = null,  // ID of the game (nullable, because it might not be set initially)
    val teamA: String,  // Name of Team A
    val teamB: String,  // Name of Team B
    val totalQuarters: Int,  // Total number of quarters in the game
    val timePerQuarterInMinutes: Int,  // Duration of each quarter in minutes
    val totalTimePlayed: Long,  // Total time played in the game (in milliseconds)
    val quarterStats: List<QuarterStats> = mutableListOf()  // List of stats for each quarter in the game
) {
    companion object {
        // Companion object contains a function to create a Game instance from a Cursor
        fun fromCursor(cursor: Cursor): Game {
            return Game(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),  // Get the game ID
                teamA = cursor.getString(cursor.getColumnIndexOrThrow("team_a")),  // Get Team A name
                teamB = cursor.getString(cursor.getColumnIndexOrThrow("team_b")),  // Get Team B name
                totalQuarters = cursor.getInt(cursor.getColumnIndexOrThrow("total_quarters")),  // Get total quarters
                timePerQuarterInMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("time_per_quarter")),  // Get time per quarter
                totalTimePlayed = cursor.getLong(cursor.getColumnIndexOrThrow("total_time_played"))  // Get total time played
            )
        }
    }
}
