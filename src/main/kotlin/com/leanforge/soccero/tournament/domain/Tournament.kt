package com.leanforge.soccero.tournament.domain

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.result.domain.MatchResult
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.data.annotation.Id
import java.util.*

data class Tournament(
        val name: String,
        val competition: Competition,
        val winners : List<LeagueTeam>,
        val losers : List<LeagueTeam>,
        @Id val uuid : UUID = UUID.randomUUID()) {


    fun competitors() : List<Set<LeagueTeam>> {
        var players = winners + losers

        if (winners.size == 1 && losers.size > 1) {
            players = losers
        }

        return players.windowed(2, 2, false)
                .map { it.toSet() }
                .toList()
    }

    fun nextRound(results : List<MatchResult>, competitors : List<Set<LeagueTeam>>) : Tournament {
        val actualWinners : MutableList<LeagueTeam> = winners.toMutableList()
        val actualLosers : MutableList<LeagueTeam> = losers.toMutableList()

        competitors.forEach { battle ->
            val battleResult = results.filter { result -> result.hasTeams(battle) }.firstOrNull()
            if (battleResult != null) {
                if (actualLosers.contains(battleResult.loser)) {
                    actualLosers.remove(battleResult.loser)
                } else {
                    actualLosers.add(battleResult.loser)
                    actualWinners.remove(battleResult.loser)
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

    fun hasAllResults(results: List<MatchResult>) : Boolean {
        return competitors()
                .all { c -> results.any { r -> r.hasTeams(c) } }
    }

    private fun filterByLoses(looses: Int, teams : List<LeagueTeam>, results : List<MatchResult>) : List<LeagueTeam> {
        return teams.filter { w -> countLoses(w, results) == looses }
                .toList()
    }

    private fun countLoses(team : LeagueTeam, results : List<MatchResult>) : Int {
        return results.count { it.loser == team }
    }

    fun filterCurrentResults(results: List<MatchResult>) : List<MatchResult> {
        return competitors()
                .map { c -> results.firstOrNull { r -> r.hasTeams(c) } }
                .filter { it != null }
                .map { it ?: throw NullPointerException()}
                .toList()
    }

    fun isFinalRound(): Boolean {
        return winners.size <= 1 && (winners.size + losers.size) <= 2
    }
}