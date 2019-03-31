package com.leanforge.soccero.tournament.domain

import com.leanforge.soccero.result.domain.MatchResult
import com.leanforge.soccero.round.Round
import com.leanforge.soccero.team.domain.LeagueTeam

data class TournamentState(
        val round: Int,
        val tournament: Tournament,
        val allResults: List<MatchResult>,
        val currentRoundResults: List<MatchResult>,
        val pendingCompetitors: List<Set<LeagueTeam>>,
        val roundDescription: Round
)