package com.leanforge.soccero.result

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.IdsExtractor
import com.leanforge.soccero.help.HelpController
import com.leanforge.soccero.help.domain.CommandArg
import com.leanforge.soccero.help.domain.CommandExample
import com.leanforge.soccero.help.domain.CommandManual
import com.leanforge.soccero.league.DefaultLeagueService
import com.leanforge.soccero.league.parser.CompetitionParser
import com.leanforge.soccero.readiness.LeagueReadinessService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class TournamentMatchController @Autowired constructor(
        val leagueService: DefaultLeagueService,
        val tournamentMatchService: DefaultTournamentMatchService,
        val leagueReadinessService: LeagueReadinessService
) : HelpController.CommandManualProvider, HelpController.AdminCommandManualProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun listCommands(): Iterable<CommandManual> {
        val leagueName = CommandArg("leagueName", "'([^']+)'", "Name of the league")
        val competition = CommandArg("competition", "([^\\s]+)", "Competition, like compA:1vs1 or compB:2vs2")
        return listOf(
                CommandManual(
                        name = ":trophy:",
                        description = "(on the match message) Mark your team as a winner",
                        isReaction = true
                ),
                CommandManual(
                        name = "listResults",
                        description = "Prints out results of the competition",
                        args = listOf(leagueName, competition),
                        examples = listOf(CommandExample(
                                "listResults 'MyLeague' tennis:2vs2",
                                "List all results for duel tennis in league MyLeague"
                        ))
                )
        )
    }

    override fun listAdminCommands(): Iterable<CommandManual> {
        val leagueName = CommandArg("leagueName", "'([^']+)'", "Name of the league")
        val competition = CommandArg("competition", "([^\\s]+)", "Competition, like compA:1vs1 or compB:2vs2")
        return listOf(
                CommandManual(
                        name = "createMatch",
                        description = "Creates match that is part of the league's tournament",
                        args = listOf(
                                leagueName, competition,
                                CommandArg(
                                        "playerList",
                                        "(.*)",
                                        "Slack users to add to the match. Players must belong to the 2 teams that are opponents in the tournament."
                                )
                        ),
                        examples = listOf(
                                CommandExample(
                                        "createMatch 'MyLeague' tennis:2vs2 @player1 @player2",
                                        "Finds teams of @player1 and @player2 and creates tournament match in league MyLeague - only when those players suppose to play against each other."
                                ),
                                CommandExample(
                                        "createMatch 'MyLeague' tennis:2vs2 @player1 @player2 @player3 @player4",
                                        "Finds teams of specified players and creates tournament match in league MyLeague."
                                )
                        )
                ),
                CommandManual(
                        name = "winner",
                        description = "(on match message) Manually marks team as a winner",
                        args = listOf(
                                CommandArg(
                                        "playerList",
                                        "(.*)",
                                        "Slack users that won the match"
                                )
                        )
                )
        )
    }

    @SlackMessageListener(value = "createMatch '(.+)' ([^\\s]+) (.*)", sendTyping = true)
    fun createMatch(
            @SlackUserId slackUserId: String,
            @SlackMessageRegexGroup(1) leagueName: String,
            @SlackMessageRegexGroup(2) competitionLabel: String,
            @SlackMessageRegexGroup(3) playerIdsString: String
    ) : SlackReactionResponse {
        logger.info("{} creating match {}#{} with players {}", slackUserId, leagueName, competitionLabel, playerIdsString)

        val league = leagueService.findStartedLeagueByName(leagueName) ?: return SlackReactionResponse("confused")

        val playerIds = IdsExtractor.extractIds(playerIdsString)
        val competition = CompetitionParser.parseSingleDefinition(competitionLabel)

        tournamentMatchService.createMatch(league, competition, playerIds.toSet())

        return SlackReactionResponse("ok_hand")
    }

    @SlackReactionListener("trophy")
    fun registerWin(
            @SlackUserId winnerId: String,
            slackMessage: SlackMessage
    ) {
        logger.info("{} registers win", winnerId)
        tournamentMatchService.registerResult(winnerId, slackMessage)
        leagueReadinessService.updateStatusMessagesForAllStartedLeagues()
    }

    @SlackThreadMessageListener("winner (.*)")
    fun manualRegisterWin(
            @SlackUserId slackUserId: String,
            @SlackMessageRegexGroup(1) winners: String,
            @SlackChannelId channel: String,
            @SlackThreadId thread: String
    ) : SlackReactionResponse {
        logger.info("{} manually registers winners: {}", slackUserId, winners)
        IdsExtractor.extractIds(winners)
                .onEach { tournamentMatchService.registerResult(it, SlackMessage(thread, channel, null)) }
        leagueReadinessService.updateStatusMessagesForAllStartedLeagues()

        return SlackReactionResponse("ok_hand")
    }

    @SlackReactionListener(value = "trophy", action = SlackReactionListener.Action.REMOVE)
    fun unregisterWin(
            @SlackUserId winnerId: String,
            slackMessage: SlackMessage
    ) {
        logger.info("{} removes win", winnerId)
        tournamentMatchService.removeResult(winnerId, slackMessage)
        leagueReadinessService.updateStatusMessagesForAllStartedLeagues()
    }

    @SlackMessageListener("listResults '(.*)' ([^\\s]+)")
    fun listResults(
            @SlackMessageRegexGroup(1) leagueName : String,
            @SlackMessageRegexGroup(2) competitionLabel: String
                    ) : String {
        return tournamentMatchService.listResults(leagueName, CompetitionParser.parseSingleDefinition(competitionLabel))
    }
}