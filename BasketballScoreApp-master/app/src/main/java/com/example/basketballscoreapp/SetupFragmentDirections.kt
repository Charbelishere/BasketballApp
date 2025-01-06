package com.example.basketballscoreapp

import android.os.Bundle
import androidx.navigation.NavDirections

class SetupFragmentDirections private constructor() {

    companion object {
        fun actionSetupFragmentToGameFragment(
            teamA: String,
            teamB: String,
            quarters: Int,
            timePerQuarter: Int
        ): NavDirections {
            return object : NavDirections {
                override val actionId: Int
                    get() = R.id.action_setupFragment_to_gameFragment

                override val arguments: Bundle
                    get() = Bundle().apply {
                        putString("teamA", teamA)
                        putString("teamB", teamB)
                        putInt("quarters", quarters)
                        putInt("timePerQuarter", timePerQuarter)
                    }
            }
        }

        fun actionSetupFragmentToStatisticsFragment(): NavDirections {
            return object : NavDirections {
                override val actionId: Int
                    get() = R.id.action_setupFragment_to_statisticsFragment

                override val arguments: Bundle
                    get() = Bundle()
            }
        }
    }
}
