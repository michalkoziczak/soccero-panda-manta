package com.leanforge.soccero.league.domain

import org.springframework.data.annotation.Id
import java.util.*

data class LeaguePlayer(
        val leagueName: String,
        val slackId: String
) {
    @Id val uuid : UUID = UUID.randomUUID()
}