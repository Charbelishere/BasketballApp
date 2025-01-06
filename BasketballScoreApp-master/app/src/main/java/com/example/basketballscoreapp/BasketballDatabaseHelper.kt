package com.example.basketballscoreapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// Database helper class to manage the basketball game database.
class BasketballDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    // Companion object to define constants for database names, versions, and table columns.
    companion object {
        const val DATABASE_NAME = "basketball.db"
        const val DATABASE_VERSION = 3 // Increment for database migrations

        // Game Table Columns
        const val TABLE_GAME = "games"
        const val COLUMN_GAME_ID = "id"
        const val COLUMN_TEAM_A = "team_a"
        const val COLUMN_TEAM_B = "team_b"
        const val COLUMN_TOTAL_QUARTERS = "total_quarters"
        const val COLUMN_TIME_PER_QUARTER = "time_per_quarter"
        const val COLUMN_TOTAL_TIME_PLAYED = "total_time_played"

        // QuarterStats Table Columns
        const val TABLE_QUARTER_STATS = "quarter_stats"
        const val COLUMN_QUARTER_STATS_ID = "id"
        const val COLUMN_GAME_ID_FOREIGN = "game_id"
        const val COLUMN_QUARTER_NUMBER = "quarter_number"
        const val COLUMN_TEAM_A_SCORE = "team_a_score"
        const val COLUMN_TEAM_B_SCORE = "team_b_score"
        const val COLUMN_TEAM_A_FOULS = "team_a_fouls"
        const val COLUMN_TEAM_B_FOULS = "team_b_fouls"
    }

    // onCreate method is called when the database is created for the first time.
    @SuppressLint("LongLogTag")
    override fun onCreate(db: SQLiteDatabase) {
        Log.d("BasketballDatabaseHelper", "onCreate called: Creating tables")

        // Creating the "games" table in the database
        db.execSQL(
            """
            CREATE TABLE $TABLE_GAME (
                $COLUMN_GAME_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEAM_A TEXT NOT NULL,
                $COLUMN_TEAM_B TEXT NOT NULL,
                $COLUMN_TOTAL_QUARTERS INTEGER NOT NULL,
                $COLUMN_TIME_PER_QUARTER INTEGER NOT NULL,
                $COLUMN_TOTAL_TIME_PLAYED INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Creating the "quarter_stats" table in the database
        db.execSQL(
            """
            CREATE TABLE $TABLE_QUARTER_STATS (
                $COLUMN_QUARTER_STATS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_GAME_ID_FOREIGN INTEGER NOT NULL,
                $COLUMN_QUARTER_NUMBER INTEGER NOT NULL,
                $COLUMN_TEAM_A_SCORE INTEGER DEFAULT 0,
                $COLUMN_TEAM_B_SCORE INTEGER DEFAULT 0,
                $COLUMN_TEAM_A_FOULS INTEGER DEFAULT 0,
                $COLUMN_TEAM_B_FOULS INTEGER DEFAULT 0,
                FOREIGN KEY ($COLUMN_GAME_ID_FOREIGN) REFERENCES $TABLE_GAME($COLUMN_GAME_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    // onUpgrade method is called when the database version is upgraded. It handles migration logic.
    @SuppressLint("LongLogTag")
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("BasketballDatabaseHelper", "onUpgrade called: Upgrading database from $oldVersion to $newVersion")

        if (oldVersion < 2) {
            // Adding the "quarter_number" column if upgrading from version 1 to 2
            db.execSQL("ALTER TABLE $TABLE_QUARTER_STATS ADD COLUMN $COLUMN_QUARTER_NUMBER INTEGER NOT NULL DEFAULT 0")
        }

        if (oldVersion < 3) {
            // Future upgrades for version 3 and beyond can be added here
        }
    }

    // Insert a new game and its associated quarter stats
    @SuppressLint("LongLogTag")
    fun insertGame(game: Game): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEAM_A, game.teamA)
            put(COLUMN_TEAM_B, game.teamB)
            put(COLUMN_TOTAL_QUARTERS, game.totalQuarters)
            put(COLUMN_TIME_PER_QUARTER, game.timePerQuarterInMinutes)
            put(COLUMN_TOTAL_TIME_PLAYED, game.totalTimePlayed)
        }

        // Insert the game into the "games" table
        val gameId = db.insert(TABLE_GAME, null, values)

        // Insert the quarter stats for the game
        game.quarterStats.forEach { stats ->
            insertQuarterStats(db, gameId.toInt(), stats)
        }

        Log.d("BasketballDatabaseHelper", "Game inserted with ID: $gameId")
        return gameId
    }

    // Insert or update quarter stats for a game
    @SuppressLint("LongLogTag")
    private fun insertQuarterStats(db: SQLiteDatabase, gameId: Int, stats: QuarterStats) {
        val values = ContentValues().apply {
            put(COLUMN_GAME_ID_FOREIGN, gameId)
            put(COLUMN_QUARTER_NUMBER, stats.quarterNumber)
            put(COLUMN_TEAM_A_SCORE, stats.teamAScore)
            put(COLUMN_TEAM_B_SCORE, stats.teamBScore)
            put(COLUMN_TEAM_A_FOULS, stats.teamAFouls)
            put(COLUMN_TEAM_B_FOULS, stats.teamBFouls)
        }

        // Update the quarter stats if they already exist, otherwise insert new record
        val updatedRows = db.update(
            TABLE_QUARTER_STATS, values,
            "$COLUMN_GAME_ID_FOREIGN = ? AND $COLUMN_QUARTER_NUMBER = ?",
            arrayOf(gameId.toString(), stats.quarterNumber.toString())
        )

        if (updatedRows == 0) {
            // Insert new stats if no update occurred
            db.insert(TABLE_QUARTER_STATS, null, values)
            Log.d("BasketballDatabaseHelper", "QuarterStats inserted: $values")
        } else {
            Log.d("BasketballDatabaseHelper", "QuarterStats updated: $values")
        }
    }

    // Retrieve all games along with their quarter stats
    @SuppressLint("LongLogTag")
    fun getAllGames(): List<Game> {
        val db = readableDatabase
        val games = mutableListOf<Game>()

        // Query to select games with non-zero scores in quarter stats
        val query = """
        SELECT * 
        FROM $TABLE_GAME 
        WHERE EXISTS (
            SELECT 1 
            FROM $TABLE_QUARTER_STATS 
            WHERE $TABLE_QUARTER_STATS.$COLUMN_GAME_ID_FOREIGN = $TABLE_GAME.$COLUMN_GAME_ID
              AND $COLUMN_TEAM_A_SCORE != 0
              AND $COLUMN_TEAM_B_SCORE != 0
        )
    """
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val gameId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_ID))
                val quarterStats = getQuarterStatsByGameId(db, gameId)

                val game = Game(
                    id = gameId,
                    teamA = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_A)),
                    teamB = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEAM_B)),
                    totalQuarters = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_QUARTERS)),
                    timePerQuarterInMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TIME_PER_QUARTER)),
                    totalTimePlayed = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_TIME_PLAYED)),
                    quarterStats = quarterStats
                )
                games.add(game)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return games
    }

    // Retrieve quarter stats for a specific game by its ID
    private fun getQuarterStatsByGameId(db: SQLiteDatabase, gameId: Int): List<QuarterStats> {
        val quarterStats = mutableListOf<QuarterStats>()
        val cursor = db.query(
            TABLE_QUARTER_STATS,
            null, // Select all columns
            "$COLUMN_GAME_ID_FOREIGN = ?",
            arrayOf(gameId.toString()),
            null,
            null,
            "$COLUMN_QUARTER_NUMBER ASC"
        )
        if (cursor.moveToFirst()) {
            do {
                quarterStats.add(QuarterStats.fromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return quarterStats
    }
}
