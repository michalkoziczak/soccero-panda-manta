package com.leanforge.soccero.round

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.data.annotation.Id
import java.util.*

data class Round(
    @Id val uuid : UUID = UUID.randomUUID(),
    val number: Int,
    val competition: Competition,
    val league: String,
    val pairs: List<Pair<LeagueTeam, LeagueTeam>>
) {
    fun isLeagueTeamPlaying(leagueTeam: LeagueTeam) : Boolean {
        return pairs.any { it.first == leagueTeam || it.second == leagueTeam }
    }
}