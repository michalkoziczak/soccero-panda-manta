package com.leanforge.soccero.queue

import com.leanforge.game.slack.listener.SlackController
import com.leanforge.game.slack.listener.SlackMessageListener
import com.leanforge.game.slack.listener.SlackMessageRegexGroup
import com.leanforge.game.slack.listener.SlackReactionResponse
import com.leanforge.soccero.league.parser.CompetitionParser
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class QueueController @Autowired constructor(private val queueService: QueueService) {

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