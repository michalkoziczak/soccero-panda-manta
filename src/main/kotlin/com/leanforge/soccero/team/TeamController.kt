package com.leanforge.soccero.team

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.IdsExctractor
import com.leanforge.soccero.league.LeagueService
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class TeamController @Autowired constructor(val teamService: TeamServiceInterface, val slackService: SlackService, val leagueService: LeagueService) {

    @SlackMessageListener("exclude '(.+)': (.*)")
    fun exclude(
            @SlackMessageRegexGroup(1) leagueName: String,
            @SlackMessageRegexGroup(2) slackIds: String,
            slackMessage: SlackMessage) : SlackReactionResponse {
        teamService.addExclusion(leagueName, IdsExctractor.extractIds(slackIds).toSet())
        return SlackReactionResponse("ok_hand")
    }

    @SlackThreadMessageListener("exclude (.*)")
    fun excludeInThisLeague(
            @SlackChannelId channel: String,
            @SlackThreadId thread: String,
            @SlackMessageRegexGroup(1) slackIds: String,
            slackMessage: SlackMessage) : SlackReactionResponse? {
        return leagueService.getPendingLeagueNameForThreadId(channel, thread).map {
            teamService.addExclusion(it, IdsExctractor.extractIds(slackIds).toSet())
            leagueService.updateMessage(it)
            SlackReactionResponse("ok_hand")
        }
                .orElse(null)
    }
}