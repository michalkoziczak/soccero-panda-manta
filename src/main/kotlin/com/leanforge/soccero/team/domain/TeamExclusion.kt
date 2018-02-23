package com.leanforge.soccero.team.domain

import org.springframework.data.annotation.Id
import java.util.*

data class TeamExclusion(
        val leagueName: String,
        val slackIds: Set<String>,
        @Id val uuid: UUID = UUID.randomUUID()
) {


    fun isForbidden(team: Set<String>) : Boolean {
        return team.filter { slackIds.contains(it) }
                .count() > 1
    }
}