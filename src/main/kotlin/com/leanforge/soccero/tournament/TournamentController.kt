package com.leanforge.soccero.tournament

import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.domain.League
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class TournamentController

@Autowired constructor(
        private val leagueService: LeagueService,
        private val tournamentService: TournamentService) {


    @SlackThreadMessageListener("createTournament")
    fun createTournament(
            @SlackChannelId channel: String,
            @SlackThreadId thread: String) : SlackReactionResponse {
        val league = leagueService.findLeagueByThreadAndState(channel, thread, League.LeagueState.STARTED) ?: return SlackReactionResponse("confused")

        tournamentService.createTournaments(league)

        return SlackReactionResponse("ok_hand")
    }
}