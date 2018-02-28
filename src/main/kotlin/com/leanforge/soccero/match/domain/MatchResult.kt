package com.leanforge.soccero.match.domain

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.LeaguePlayer
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.data.annotation.Id
import java.time.Instant
import java.util.*

data class MatchResult(
        val leagueName: String,
        val competition: Competition,
        val loser: LeagueTeam,
        val winner: LeagueTeam,
        val matchId: UUID,
        @Id val uuid: UUID = UUID.randomUUID(),
        val createDate : Instant = Instant.now()) {

    fun hasTeams(teams: Set<LeagueTeam>) : Boolean {
        return setOf(loser, winner) == teams
    }
}