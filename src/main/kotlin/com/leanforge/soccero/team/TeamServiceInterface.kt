package com.leanforge.soccero.team

import com.leanforge.soccero.league.domain.LeaguePlayer
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.team.domain.TeamExclusion

interface TeamServiceInterface {
    fun composeTeams(leagueName: String, size: Int, players: Set<LeaguePlayer>) : Set<LeagueTeam>
    fun addExclusion(leagueName: String, slackIds: Set<String>)
    fun findExclusions(leagueName: String) : List<TeamExclusion>
}