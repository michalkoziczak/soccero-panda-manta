package com.leanforge.soccero.tournament

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.tournament.domain.Tournament
import com.leanforge.soccero.tournament.exception.TournamentAlreadyExistsException
import com.leanforge.soccero.tournament.repo.TournamentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TournamentService @Autowired constructor(private val tournamentRepository: TournamentRepository) {

    fun createTournaments(league: League) {
        val tournaments = mutableSetOf<Tournament>()
        league.competitions.onEach {
            tournaments.add(createTournament(tournaments, it, league))
        }

        verifyHasNoDuplicates(league, tournaments)
        tournamentRepository.save(tournaments)
    }

    private fun createTournament(existingTournaments: Set<Tournament>, competition: Competition, league: League) : Tournament {
        val teams = league.teams.filter { it.slackIds.size == competition.players }
                .toList()
        return List(100, { Tournament(league.name, competition, teams.shuffled(), emptyList()) })
                .minBy { generatedTournament -> countDuplicates(existingTournaments, generatedTournament) }
                ?: Tournament(league.name, competition, league.teams.shuffled(), emptyList())

    }

    private fun countDuplicates(existingTournaments: Set<Tournament>, generated: Tournament) : Int {
        return generated.competitors().map { c -> existingTournaments.count { it.competitors().contains(c) } }
                .sum()
    }

    private fun verifyHasNoDuplicates(league: League, generatedTournaments: Set<Tournament>) {
        val hasDuplicate = tournamentRepository.findAllByName(league.name)
                .anyMatch({ saved -> generatedTournaments.any { generated -> generated.competition == saved.competition } })

        if (hasDuplicate) {
            throw TournamentAlreadyExistsException(league.name)
        }
    }
}