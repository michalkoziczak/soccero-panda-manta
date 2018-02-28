package com.leanforge.soccero.team.domain

data class LeagueTeam(val slackIds: Set<String>) {

    fun size() : Int {
        return slackIds.size
    }
}