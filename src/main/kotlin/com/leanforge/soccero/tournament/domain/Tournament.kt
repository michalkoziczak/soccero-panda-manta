package com.leanforge.soccero.tournament.domain

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.data.annotation.Id
import com.leanforge.soccero.match.MatchResult
import java.util.*

data class Tournament(
        val name: String,
        val competition: Competition,
        val winners : List<LeagueTeam>,
        val losers : List<LeagueTeam>,
        @Id val uuid : UUID = UUID.randomUUID()) {


    fun competitors() : List<Set<LeagueTeam>> {
        return (winners + losers).windowed(2, 2, true)
                .map { it.toSet() }
                .toList()
    }

    fun nextRound(results : List<MatchResult>, competitors : List<Set<LeagueTeam>>) : Tournament {
        val actualWinners : MutableList<LeagueTeam> = winners.toMutableList()
        val actualLosers : MutableList<LeagueTeam> = losers.toMutableList()

        competitors.forEach { battle ->
            val battleResult : MatchResult? = results.filter { result -> result.competitors == battle }.firstOrNull()
            if (battleResult != null) {
                if (actualLosers.contains(battleResult.looser())) {
                    actualLosers.remove(battleResult.looser())
                } else {
                    actualLosers.add(battleResult.looser())
                    actualWinners.remove(battleResult.looser())
                }
            }
        }
        return Tournament(name, competition, actualWinners, actualLosers, uuid)
    }


    fun currentState(results : List<MatchResult>) : Tournament {
        val newWinners = filterByLoses(0, winners, results)

        val newLosers = filterByLoses(0, losers, results) + filterByLoses(1, winners, results)

        return Tournament(name, competition, newWinners, newLosers, uuid)
    }

    private fun filterByLoses(looses: Int, teams : List<LeagueTeam>, results : List<MatchResult>) : List<LeagueTeam> {
        return teams.filter { w -> countLoses(w, results) == looses }
                .toList()
    }

    private fun countLoses(team : LeagueTeam, results : List<MatchResult>) : Int {
        return results.count { it.looser() == team }
    }
}