package com.leanforge.soccero.help

import com.leanforge.game.slack.listener.SlackController
import com.leanforge.game.slack.listener.SlackMessageListener
import com.leanforge.soccero.help.domain.CommandExample
import com.leanforge.soccero.help.domain.CommandManual
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class HelpController

@Autowired constructor(
        val helpProviders: List<CommandManualProvider>,
        val adminHelpProviders: List<AdminCommandManualProvider>
) {

    private val helpCommands = listOf(
            CommandManual("help", "Prints this message."),
            CommandManual("adminHelp", "Describes usage of admin commands.")
    )

    @SlackMessageListener("help")
    fun printHelp() : String {
        return listCommands()
//                return "Features and commands:\n" +
//                        ":pushpin: `help` - prints help message.\n" +
//                        ":pushpin: `adminHelp` - prints help message for admin commands.\n" +
//                        ":pushpin: :heavy_plus_sign: - Add/Remove yourself from competition\n" +
//                        ":pushpin: `listLeagues` prints all existing leagues\n" +
//                        ":pushpin: `listResults '{leagueName}' competition:XvsX` prints results of given competition in league\n" +
//                        ":pushpin: `tournamentState` prints pending matches for started league\n" +
//                        ":pushpin: `tournamentState '{leagueName}'` prints pending matches for given league\n"
    }

    @SlackMessageListener("adminHelp")
    fun printAdminHelp() : String {
        return listAdminCommands()
//        return "Features and commands:\n" +
//                ":pushpin: `newLeague 'League Name' competition_1:1vs1 competition_2:2vs2` - creates league with competition_1 (1 player versus 1 player) and competition_2 (2 players vs 2 players)\n" +
//                ":pushpin: `startLeague '{leagueName}'` - Randomize teams and start league!\n" +
//                ":pushpin: `startLeague` in league thread - Randomize teams and start league!\n" +
//                ":pushpin: `pauseLeague '{leagueName}'` - puts league on hold\n" +
//                ":pushpin: `resumeLeague '{leagueName}'` - resumes league\n" +
//                ":pushpin: `exclude @player1 @player2 @player3 ...` in league thread - Adds exclusion group - players that can't be in the same team\n" +
//                ":pushpin: `createMatch '{leagueName}' competition:XvsX @player1 @player2 @player3 ...` creates (starts new) tournament match\n" +
//                ":pushpin: `defineQueue competition:XvsX #channelname p4` defines queue. Bot will use `startGame` command on this channel to schedule tournament game\n" +
//                ":pushpin: `removeQueue competition:XvsX` removes queue definition\n" +
//                ":pushpin: `listQueues` list defined queues (competition-channel bindings)\n"
    }


    private fun listCommands(): String {
        return helpProviders
                .flatMap { it.listCommands() }
                .plus(helpCommands)
                .map { it.slackLabel() }
                .joinToString("\n")
    }

    private fun listAdminCommands(): String {
        return adminHelpProviders
                .flatMap { it.listAdminCommands() }
                .map { it.slackLabel() }
                .joinToString("\n")
    }

    interface CommandManualProvider {
        fun listCommands() : Iterable<CommandManual>
    }

    interface AdminCommandManualProvider {
        fun listAdminCommands() : Iterable<CommandManual>
    }
}