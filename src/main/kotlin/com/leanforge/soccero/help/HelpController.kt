package com.leanforge.soccero.help

import com.leanforge.game.slack.SlackService
import com.leanforge.game.slack.listener.SlackChannelId
import com.leanforge.game.slack.listener.SlackController
import com.leanforge.game.slack.listener.SlackMessageListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller

@SlackController
class HelpController {

    @SlackMessageListener("help")
    fun printHelp() : String {
                return "Features and commands:\n" +
                        ":pushpin: `help` - prints help message.\n" +
                        ":pushpin: `newLeague 'League Name' competition_1:1vs1 competition_2:2vs2` - creates league with competition_1 (1 player versus 1 player) and competition_2 (2 players vs 2 players)\n" +
                        ":pushpin: :heavy_plus_sign: - Add/Remove yourself from competition\n" +
                        ":pushpin: `startLeague 'League Name'` - Randomize teams and start league!\n" +
                        ":pushpin: `startLeague` in league thread - Randomize teams and start league!\n" +
                        ":pushpin: `listLeagues` prints all existing leagues\n" +
                        ":pushpin: `exclude @player1 @player2 @player3 ...` in league thread - Adds exclusion group - players that can't be in the same team\n" +
                        ":pushpin: `createMatch '{leagueName}' competition:XvsX @player1 @player2 @player3 ...` creates (starts new) tournament match\n" +
                        ":pushpin: `listResults '{leagueName}' competition:XvsX` prints results of given competition in league\n" +
                        ":pushpin: `defineQueue competition:XvsX #channelname p4` defines queue. Bot will use `startGame` command on this channel to schedule tournament game\n" +
                        ":pushpin: `removeQueue competition:XvsX` removes queue definition\n" +
                        ":pushpin: `listQueues` list defined queues (competition-channel bindings)\n"
    }
}