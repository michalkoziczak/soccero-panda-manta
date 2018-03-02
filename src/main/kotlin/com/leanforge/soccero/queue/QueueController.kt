package com.leanforge.soccero.queue

import com.leanforge.game.slack.SlackService
import com.leanforge.game.slack.listener.SlackController
import com.leanforge.game.slack.listener.SlackMessageListener
import com.leanforge.game.slack.listener.SlackMessageRegexGroup
import com.leanforge.game.slack.listener.SlackReactionResponse
import com.leanforge.soccero.help.HelpController
import com.leanforge.soccero.help.domain.CommandArg
import com.leanforge.soccero.help.domain.CommandExample
import com.leanforge.soccero.help.domain.CommandManual
import com.leanforge.soccero.league.parser.CompetitionParser
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class QueueController @Autowired constructor(private val queueService: QueueService, private val slackService: SlackService)
    : HelpController.AdminCommandManualProvider {

    override fun listAdminCommands(): Iterable<CommandManual> {
        val competition = CommandArg("competition", "([^\\s]+)", "Competition, like compA:1vs1 or compB:2vs2")
        val channel = CommandArg("channel", "#([^\\s]+)", "Name of the slack channel")
        val priority = CommandArg("priority", "p(\\d{1})", "Priority of the game")
        return listOf(
                CommandManual(
                        name = "defineQueue",
                        description = "Defines channel where `startGame` message will be send to queue the tournament match.",
                        args = listOf(competition, channel, priority),
                        examples = listOf(
                                CommandExample(
                                        "defineQueue tennis:2vs2 #general p1",
                                        "Defines that `startGame p1 comment` message will be send on #general to schedule tennis match"
                                ),
                                CommandExample(
                                        "defineQueue rocket_league:1vs1 #rocket_league p7",
                                        "Defines that `startGame p7 comment` message will be send on #rocket_league to schedule rocket_league match"
                                )
                        )
                ),
                CommandManual(
                        name = "removeQueue",
                        description = "Removes binding of competition to slack channel",
                        args = listOf(competition),
                        examples = listOf(
                                CommandExample(
                                        "removeQueue tennis:2vs2",
                                        "There will be no game scheduling for duel tennis"
                                )
                        )
                ),
                CommandManual(
                        name = "listQueues",
                        description = "List all bindings of competition to slack channel"
                )
        )
    }

    @SlackMessageListener("defineQueue ([^\\s]+) <#([^\\|]*)\\|[^>]*> p(\\d{1})")
    fun defineQueue(
            @SlackMessageRegexGroup(1) competitionString: String,
            @SlackMessageRegexGroup(2) destChannel: String,
            @SlackMessageRegexGroup(3) priority: String

    ) : SlackReactionResponse {
        val competition = CompetitionParser.parseSingleDefinition(competitionString)
        queueService.createConfig(competition, destChannel, Integer.parseInt(priority))
        return SlackReactionResponse("ok_hand")
    }

    @SlackMessageListener("defineQueue ([^\\s]+) #([^\\s]+) p(\\d{1})")
    fun definePrivateQueue(
            @SlackMessageRegexGroup(1) competitionString: String,
            @SlackMessageRegexGroup(2) destChannelName: String,
            @SlackMessageRegexGroup(3) priority: String

    ) : SlackReactionResponse {
        val competition = CompetitionParser.parseSingleDefinition(competitionString)
        val destChannel = slackService.getChannelId(destChannelName).orElse(null) ?: return SlackReactionResponse("confused")
        queueService.createConfig(competition, destChannel, Integer.parseInt(priority))
        return SlackReactionResponse("ok_hand")
    }

    @SlackMessageListener("removeQueue ([^\\s]+)")
    fun removeQueue(@SlackMessageRegexGroup(1) competitionString: String) : SlackReactionResponse {
        val competition = CompetitionParser.parseSingleDefinition(competitionString)
        queueService.removeConfig(competition)
        return SlackReactionResponse("ok_hand")
    }

    @SlackMessageListener("listQueues")
    fun listQueues() : String {
        return queueService.listConfig()
    }
}