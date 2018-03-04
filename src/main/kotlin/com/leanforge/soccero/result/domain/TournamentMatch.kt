package com.leanforge.soccero.result.domain

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.data.annotation.Id
import java.util.*

data class TournamentMatch(
        val leagueName: String,
        val competition: Competition,
        val competitors: Set<LeagueTeam>,
        val slackChannelId: String,
        val slackMessageId: String,
        @Id val uuid : UUID = UUID.randomUUID()
) {
}