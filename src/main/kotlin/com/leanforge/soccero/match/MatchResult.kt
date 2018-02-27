package com.leanforge.soccero.match


import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.team.domain.LeagueTeam
import java.util.*

data class MatchResult(
        val leagueName: String,
        val competition: Competition,
        val competitors: Set<LeagueTeam>,
        val winner: LeagueTeam,
        val uuid: UUID = UUID.randomUUID()) {

    fun looser() : LeagueTeam {
        return competitors.minus(winner).first()
    }


}