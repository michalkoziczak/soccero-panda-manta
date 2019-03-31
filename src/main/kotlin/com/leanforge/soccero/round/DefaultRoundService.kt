package com.leanforge.soccero.round

import com.leanforge.soccero.round.repo.RoundRepository
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.domain.Tournament
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Collectors

@Service
class DefaultRoundService @Autowired constructor(
        private val roundRepository: RoundRepository
) : RoundService {
    override fun getAllRoundsForTournament(tournament: Tournament): List<Round> {
        return roundRepository.findAllByCompetitionAndLeague(tournament.competition, tournament.name).collect(Collectors.toList())
    }

    override fun createNewRoundForTournament(tournament: Tournament, winners: List<LeagueTeam>, losers: List<LeagueTeam>, number: Int): Round {
        val pairs = getPairs(winners.shuffled()) + getPairs(losers.shuffled())
        return roundRepository.save(Round(UUID.randomUUID(), number,  tournament.competition, tournament.name, pairs))
    }

    override fun createSimpleNewRoundForTournament(tournament: Tournament, winners: List<LeagueTeam>, losers: List<LeagueTeam>, number: Int): Round {
        val pairs = getPairs(winners) + getPairs(losers)
        return roundRepository.save(Round(UUID.randomUUID(), number,  tournament.competition, tournament.name, pairs))
    }

    private fun getPairs(teams: List<LeagueTeam>) : List<Pair<LeagueTeam, LeagueTeam>> {
        return teams.windowed(2,2,false)
                .map { Pair(it[0], it[1]) }
                .toList()
    }
}