package com.leanforge.soccero.tournament

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.match.DefaultTournamentMatchService
import com.leanforge.soccero.match.TournamentMatchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TournamentStateService @Autowired constructor(
        private val tournamentService: TournamentService,
        private val tournamentMatchService: TournamentMatchService) {


    fun listPendingCompetitors(league: League, competition: Competition) : String {
        val results = tournamentMatchService.getResults(league.name, competition)
        return tournamentService.listPendingCompetitors(league, competition, results)
    }

    fun listPendingCompetitorsInAllCompetitions(league: League) : String {
        return league.competitions
                .map { listPendingCompetitors(league, it) }
                .joinToString("\n\n")
    }
}