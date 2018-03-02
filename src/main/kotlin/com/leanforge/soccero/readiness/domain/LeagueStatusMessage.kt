package com.leanforge.soccero.readiness.domain

import com.leanforge.game.slack.SlackMessage
import com.leanforge.soccero.league.domain.Competition
import org.springframework.data.annotation.Id
import java.time.Instant
import java.util.*

data class LeagueStatusMessage(
        val slackMessageId: String,
        val slackChannelId: String,
        val leagueName: String,
        val competition: Competition,
        val creationDate: Instant = Instant.now(),
        @Id val uuid: UUID = UUID.randomUUID()) {

    fun toSlackMessage(): SlackMessage {
        return SlackMessage(slackMessageId, slackChannelId, null)
    }
}