package com.example.basketballscoreapp

import android.os.Bundle
import androidx.navigation.NavDirections

class GameFragmentDirections private constructor() {
    companion object {
        fun actionGameFragmentToStatisticsFragment(
            teamAStats: String,
            teamBStats: String,
            winner: String
        ): NavDirections {
            return object : NavDirections {
                override val actionId: Int
                    get() = R.id.action_gameFragment_to_statisticsFragment

                override val arguments: Bundle
                    get() = Bundle().apply {
                        putString("teamAStats", teamAStats)
                        putString("teamBStats", teamBStats)
                    }
            }
        }
    }
}
