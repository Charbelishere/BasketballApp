package com.example.basketballscoreapp

import android.database.Cursor

// Data class representing statistics for a particular quarter of a basketball game
data class QuarterStats(
    val id: Int? = null, // The unique ID of the quarter stats (nullable because it may not be set initially)
    val gameId: Int, // Foreign key referencing the Game table, identifying which game this quarter belongs to
    val quarterNumber: Int, // The quarter number (e.g., 1, 2, 3, or 4)
    val teamAScore: Int = 0, // The score of Team A in this quarter, defaulting to 0
    val teamBScore: Int = 0, // The score of Team B in this quarter, defaulting to 0
    val teamAFouls: Int = 0, // The number of fouls by Team A in this quarter, defaulting to 0
    val teamBFouls: Int = 0  // The number of fouls by Team B in this quarter, defaulting to 0
) {
    companion object {
        // Companion object contains the function for creating a QuarterStats object from a Cursor
        fun fromCursor(cursor: Cursor): QuarterStats {
            return QuarterStats(
                // Fetching values from the cursor and mapping them to the appropriate fields
                id = cursor.getColumnIndexOrNull("id")?.let { cursor.getInt(it) }, // The ID column (nullable)
                gameId = cursor.getInt(cursor.getColumnIndexOrThrow("game_id")), // Game ID from the cursor
                quarterNumber = cursor.getInt(cursor.getColumnIndexOrThrow("quarter_number")), // Quarter number
                teamAScore = cursor.getInt(cursor.getColumnIndexOrThrow("team_a_score")), // Team A's score for this quarter
                teamBScore = cursor.getInt(cursor.getColumnIndexOrThrow("team_b_score")), // Team B's score for this quarter
                teamAFouls = cursor.getInt(cursor.getColumnIndexOrThrow("team_a_fouls")), // Team A's fouls for this quarter
                teamBFouls = cursor.getInt(cursor.getColumnIndexOrThrow("team_b_fouls")) // Team B's fouls for this quarter
            )
        }

        // Helper function to safely retrieve the column index, returning null if the column doesn't exist
        private fun Cursor.getColumnIndexOrNull(columnName: String): Int? {
            return try {
                getColumnIndexOrThrow(columnName) // Try to get the index of the column
            } catch (e: IllegalArgumentException) {
                null // Return null if the column doesn't exist
            }
        }
    }
}
