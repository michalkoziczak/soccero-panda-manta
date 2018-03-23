package com.leanforge.soccero.queue

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.team.domain.LeagueTeam

interface QueueService {
    fun triggerGameScheduler(competition: Competition, teams: Set<LeagueTeam>)
    fun createConfig(competition: Competition, destinationSlackChannel: String, priority: Int)
    fun removeConfig(competition: Competition)
    fun listConfig() : String
}