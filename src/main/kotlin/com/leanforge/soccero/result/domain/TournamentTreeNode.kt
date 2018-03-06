package com.leanforge.soccero.result.domain

import com.leanforge.soccero.team.domain.LeagueTeam
import java.util.*

data class TournamentTreeNode(
        var team: LeagueTeam? = null,
        var opponent: LeagueTeam? = null,
        var round: Int = 0,
        var state: State = State.PENDING,
        var child: String? = null,
        var label: String? = null,
        var id: String = UUID.randomUUID().toString()
) {


    enum class State {
        WON, LOST, PENDING, BLOCKED
    }
}