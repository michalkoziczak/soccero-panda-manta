package com.leanforge.soccero.match

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.IdsExctractor
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.parser.CompetitionParser
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class TournamentMatchController @Autowired constructor(
        val leagueService: LeagueService,
        val tournamentMatchService: TournamentMatchService
) {

    @SlackMessageListener(value = "createMatch '(.+)' ([^\\s]+) (.*)", sendTyping = true)
    fun createMatch(
            @SlackMessageRegexGroup(1) leagueName: String,
            @SlackMessageRegexGroup(2) competitionLabel: String,
            @SlackMessageRegexGroup(3) playerIdsString: String
    ) : SlackReactionResponse {
        val league = leagueService.findStartedLeagueByName(leagueName) ?: return SlackReactionResponse("confused")

        val playerIds = IdsExctractor.extractIds(playerIdsString)
        val competition = CompetitionParser.parseSingleDefinition(competitionLabel)

        tournamentMatchService.createMatch(league, competition, playerIds.toSet())

        return SlackReactionResponse("ok_hand")
    }

    @SlackReactionListener("trophy")
    fun registerWin(
            @SlackUserId winnerId: String,
            slackMessage: SlackMessage
    ) {
        tournamentMatchService.registerResult(winnerId, slackMessage)
    }

    @SlackReactionListener(value = "trophy", action = SlackReactionListener.Action.REMOVE)
    fun unregisterWin(
            @SlackUserId winnerId: String,
            slackMessage: SlackMessage
    ) {
        tournamentMatchService.removeResult(winnerId, slackMessage)
    }

    @SlackMessageListener("listResults '(.*)' ([^\\s]+)")
    fun listResults(
            @SlackMessageRegexGroup(1) leagueName : String,
            @SlackMessageRegexGroup(2) competitionLabel: String
                    ) : String {
        return tournamentMatchService.listResults(leagueName, CompetitionParser.parseSingleDefinition(competitionLabel))
    }
}