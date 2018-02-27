package com.leanforge.soccero.tournament.domain

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.LeaguePlayer

data class TournamentResult(
        val leagueName: String,
        val competition: Competition,
        val competitors: Set<LeaguePlayer>) {
}