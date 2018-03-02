package com.leanforge.soccero.tournament

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.tournament.domain.Tournament
import com.leanforge.soccero.match.domain.MatchResult
import com.leanforge.soccero.team.domain.LeagueTeam

interface TournamentService {
    fun createTournaments(league: League)
    fun currentState(league: League, competition: Competition, results: List<MatchResult>) : Tournament
    fun pendingCompetitors(league: League, competition: Competition, results: List<MatchResult>) : List<Set<LeagueTeam>>
    fun listPendingCompetitors(league: League, competition: Competition, results: List<MatchResult>) : String
    fun currentResults(league: League, competition: Competition, results: List<MatchResult>): List<MatchResult>
}