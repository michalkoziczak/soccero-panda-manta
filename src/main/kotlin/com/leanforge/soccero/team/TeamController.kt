package com.leanforge.soccero.team

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.IdsExctractor
import com.leanforge.soccero.help.HelpController
import com.leanforge.soccero.help.domain.CommandArg
import com.leanforge.soccero.help.domain.CommandExample
import com.leanforge.soccero.help.domain.CommandManual
import com.leanforge.soccero.league.DefaultLeagueService
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class TeamController @Autowired constructor(val teamService: TeamServiceInterface, val slackService: SlackService, val leagueService: DefaultLeagueService)
    : HelpController.AdminCommandManualProvider
{
    override fun listAdminCommands(): Iterable<CommandManual> {
        return listOf(
                CommandManual(
                        name = "exclude",
                        description = "(on pending league thread) Adds rule, that group of players can't be in the same team.",
                        args = listOf(CommandArg("playerList", "(.*)", "List of players separated with space")),
                        examples = listOf(
                                CommandExample(
                                        "exclude @player1 @player2 @player3",
                                        "player1, player2 and player3 won't be members of the same team"
                                )
                        )
                )
        )
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