package com.leanforge.soccero.league

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.IdsExctractor
import com.leanforge.soccero.league.parser.CompetitionParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import java.util.regex.Pattern

@SlackController
open class LeagueController @Autowired constructor(val slackService: SlackService, val leagueService: LeagueService, val competitionParser: CompetitionParser) {

    @SlackMessageListener("newLeague\\s+'([^']+)'\\s+(.*)")
    fun createLeague(
            slackMessage: SlackMessage,
            @SlackMessageRegexGroup(1) name: String,
            @SlackMessageRegexGroup(2) competitions: String) {
        val definitions = competitionParser.parseDefinition(competitions)
        leagueService.createLeague(slackMessage, name, definitions)
    }

    @SlackMessageListener(value = "startLeague '([^']+)'", sendTyping = true)
    fun startLeague(@SlackMessageRegexGroup(1) name: String, slackMessage: SlackMessage) {
        slackService.addReactions(slackMessage, "coffee")
        leagueService.startLeague(name)
        slackService.addReactions(slackMessage, "ok_hand")

    }

    @SlackThreadMessageListener("startLeague")
    fun startThisLeague(@SlackChannelId channel: String, @SlackThreadId thread: String, slackMessage: SlackMessage) {
        leagueService.getNameForThreadId(channel, thread).ifPresent {
            slackService.addReactions(slackMessage, "coffee")

            leagueService.startLeague(it)

            slackService.addReactions(slackMessage, "ok_hand")
        }
    }

    @SlackMessageListener("\\+ '([^']+)' <@(.*)>")
    fun addPlayer(@SlackMessageRegexGroup(1) name: String, @SlackMessageRegexGroup(2) userId: String) {
        leagueService.addPlayerAndUpdateStatusMessage(name, userId)
    }

    @SlackThreadMessageListener("add (.*)")
    fun addPlayerToThisLeague(@SlackMessageRegexGroup(1) userIds: String, @SlackThreadId thread: String, @SlackChannelId channel: String) {
        leagueService.getNameForThreadId(channel, thread).ifPresent { name ->
            IdsExctractor.extractIds(userIds).forEach { id -> leagueService.addPlayerAndUpdateStatusMessage(name, id)}
        }
    }

    @SlackReactionListener("heavy_plus_sign")
    fun addPlayer(slackMessage: SlackMessage, @SlackUserId userId: String) {
        leagueService.addPlayerAndUpdateStatusMessage(slackMessage, userId)
    }

    @SlackReactionListener(value = "heavy_plus_sign", action = SlackReactionListener.Action.REMOVE)
    fun removePlayer(slackMessage: SlackMessage, @SlackUserId userId: String) {
        leagueService.removePlayerAndUpdateStatusMessage(slackMessage, userId)
    }
}