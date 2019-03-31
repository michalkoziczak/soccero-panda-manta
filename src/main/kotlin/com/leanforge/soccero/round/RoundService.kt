package com.leanforge.soccero.round

import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.domain.Tournament

interface RoundService {
    fun getAllRoundsForTournament(tournament: Tournament) : List<Round>
    fun createNewRoundForTournament(tournament: Tournament, winners: List<LeagueTeam>, losers : List<LeagueTeam>, number: Int) : Round
    fun createSimpleNewRoundForTournament(tournament: Tournament, winners: List<LeagueTeam>, losers : List<LeagueTeam>, number: Int) : Round
}