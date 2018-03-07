package com.leanforge.soccero.league

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.IdsExtractor
import com.leanforge.soccero.help.HelpController
import com.leanforge.soccero.help.domain.CommandArg
import com.leanforge.soccero.help.domain.CommandExample
import com.leanforge.soccero.help.domain.CommandManual
import com.leanforge.soccero.league.parser.CompetitionParser
import org.springframework.beans.factory.annotation.Autowired

@SlackController
open class LeagueController @Autowired constructor(val slackService: SlackService, val leagueService: DefaultLeagueService) : HelpController.CommandManualProvider, HelpController.AdminCommandManualProvider {


    override fun listCommands(): Iterable<CommandManual> {
        return listOf(
                CommandManual(
                        name = ":heavy_plus_sign:",
                        description = "(on pending league thread) Join the league",
                        isReaction = true
                )
        )
    }

    override fun listAdminCommands(): Iterable<CommandManual> {
        val leagueName = CommandArg("leagueName", "'([^']+)'", "Name of the league")
        val competitions = CommandArg("competitions", "(.*)", "List of competitions, like compA:1vs1 compB:2vs2")
        return listOf(
                CommandManual(
                        name = "listLeagues",
                        description = "Prints out all existing leagues"
                ),
                CommandManual(
                        name = "newLeague",
                        description = "creates new league",
                        args = listOf(leagueName, competitions),
                        examples = listOf(
                                CommandExample(
                                        "newLeague 'MyLeague' football:4vs4 tennis:2vs2 tennis:1vs1 handball:4vs4",
                                        "Create league named MyLeague with 4 competitions: football and handball with teams of four, tennis with teams of two and solo tennis"
                                )
                        )
                ),
                CommandManual(
                        name = "startLeague",
                        description = "(on pending league thread) Randomizes teams and starts league"
                ),
                CommandManual(
                        name = "add",
                        description = "(on pending league thread) Adds players to the league",
                        args = listOf(CommandArg("playerList", "(.*)", "List of players separated with space")),
                        examples = listOf(
                                CommandExample(
                                        "add @player1 @player2 @player3",
                                        "Add player1 player2 player3 to the league"
                                )
                        )
                ),
                CommandManual(
                        name = "pauseLeague",
                        args = listOf(leagueName),
                        description = "Puts league on hold"
                ),
                CommandManual(
                        name = "resumeLeague",
                        args = listOf(leagueName),
                        description = "Resumes league"
                ),
                CommandManual(
                        name = "endLeague",
                        args = listOf(leagueName),
                        description = "Closes league"
                )
        )
    }

    @SlackMessageListener("newLeague\\s+'([^']+)'\\s+(.*)")
    fun createLeague(
            slackMessage: SlackMessage,
            @SlackMessageRegexGroup(1) name: String,
            @SlackMessageRegexGroup(2) competitions: String) {
        val definitions = CompetitionParser.parseDefinition(competitions)
        leagueService.createLeague(slackMessage, name, definitions)
    }

    @SlackMessageListener(value = "endLeague '([^']+)'", sendTyping = true)
    fun endLeague(@SlackMessageRegexGroup(1) name: String, slackMessage: SlackMessage) {
        leagueService.endLeague(name)
        slackService.addReactions(slackMessage, "ok_hand")

    }

    @SlackMessageListener(value = "pauseLeague '([^']+)'", sendTyping = true)
    fun pauseLeague(@SlackMessageRegexGroup(1) name: String) : SlackReactionResponse {
        leagueService.pauseLeague(name) ?: return SlackReactionResponse("confused")

        return SlackReactionResponse("ok_hand")
    }

    @SlackMessageListener(value = "resumeLeague '([^']+)'", sendTyping = true)
    fun resumeLeague(@SlackMessageRegexGroup(1) name: String) : SlackReactionResponse {
        leagueService.resumeLeague(name) ?: return SlackReactionResponse("confused")

        return SlackReactionResponse("ok_hand")
    }

    @SlackMessageListener(value = "listLeagues", sendTyping = true)
    fun listLeagues() : String {
        return leagueService.listLeagues()
    }

    @SlackMessageListener(value = "removeCompetition '([^']+)' (.*)")
    fun removeCompetition(@SlackMessageRegexGroup(1) leagueName: String, @SlackMessageRegexGroup(2) competition: String) : SlackReactionResponse {
        leagueService.deleteCompetition(leagueName, CompetitionParser.parseSingleDefinition(competition))
        return SlackReactionResponse("wastebasket")
    }

    @SlackThreadMessageListener("startLeague")
    fun startThisLeague(@SlackChannelId channel: String, @SlackThreadId thread: String, slackMessage: SlackMessage) {
        leagueService.getPendingLeagueNameForThreadId(channel, thread).ifPresent {
            slackService.addReactions(slackMessage, "coffee")

            leagueService.startLeague(it)

            slackService.addReactions(slackMessage, "ok_hand")
        }
    }

    @SlackThreadMessageListener("add (.*)")
    fun addPlayerToThisLeague(@SlackMessageRegexGroup(1) userIds: String, @SlackThreadId thread: String, @SlackChannelId channel: String) {
        leagueService.getPendingLeagueNameForThreadId(channel, thread).ifPresent { name ->
            IdsExtractor.extractIds(userIds).forEach { id -> leagueService.addPlayerAndUpdateStatusMessage(name, id)}
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