package com.leanforge.soccero.league.domain

import com.leanforge.game.slack.SlackMessage
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.data.annotation.Id
import java.time.Instant
import java.util.*

data class League constructor(@Id var name : String = UUID.randomUUID().toString()) {
    var slackMessageId : String? = null
    var slackChannelId : String? = null
    var competitions: Set<Competition> = setOf()
    var state: LeagueState = LeagueState.PENDING
    var createdOn: Instant = Instant.now()
    var teams: Set<LeagueTeam> = setOf()


    fun startMessage() : SlackMessage {
        return SlackMessage(slackMessageId, slackChannelId, null)
    }

    enum class LeagueState(val icon: String) {
        PENDING("new"), STARTED("arrow_forward"), PAUSED("double_vertical_bar"), FINISHED("checkered_flag");
    }
}
