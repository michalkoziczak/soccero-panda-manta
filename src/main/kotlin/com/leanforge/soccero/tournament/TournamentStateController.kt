package com.leanforge.soccero.tournament

import com.leanforge.game.slack.listener.SlackController
import com.leanforge.game.slack.listener.SlackMessageListener
import com.leanforge.game.slack.listener.SlackMessageRegexGroup
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.parser.CompetitionParser
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class TournamentStateController @Autowired constructor(
        private val tournamentStateService: TournamentStateService,
        private val leagueService: LeagueService) {


    @SlackMessageListener("tournamentState '(.*)' ([^\\s]+)")
    fun printPendingCompetitors(
            @SlackMessageRegexGroup(1) leagueName : String,
            @SlackMessageRegexGroup(2) competitionLabel : String

    ) : String {
        val league = leagueService.findStartedLeagueByName(leagueName) ?: return "There is no such league"
        return tournamentStateService.listPendingCompetitors(league, CompetitionParser.parseSingleDefinition(competitionLabel))
    }

    @SlackMessageListener("tournamentState ([^\\s]+)")
    fun printPendingCompetitorsSingleStartedLeague(
            @SlackMessageRegexGroup(1) competitionLabel : String

    ) : String {
        val league = leagueService.findOnlyStartedLeague() ?: return "There is no single started league"
        return tournamentStateService.listPendingCompetitors(league, CompetitionParser.parseSingleDefinition(competitionLabel))
    }

    @SlackMessageListener("tournamentState")
    fun printAllPendingCompetitorsSingleStartedLeague() : String {
        val league = leagueService.findOnlyStartedLeague() ?: return "There is no single started league"
        return tournamentStateService.listPendingCompetitorsInAllCompetitions(league)
    }
}