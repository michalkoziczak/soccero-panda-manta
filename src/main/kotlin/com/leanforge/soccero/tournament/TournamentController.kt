package com.leanforge.soccero.tournament

import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.help.HelpController
import com.leanforge.soccero.help.domain.CommandManual
import com.leanforge.soccero.league.DefaultLeagueService
import com.leanforge.soccero.league.domain.League
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class TournamentController

@Autowired constructor(
        private val leagueService: DefaultLeagueService,
        private val tournamentService: TournamentService) : HelpController.AdminCommandManualProvider {

    override fun listAdminCommands(): Iterable<CommandManual> {
        return listOf(
                CommandManual(
                        name = "createTournament",
                        description = "(on started league thread) Generates tournament trees for all competitions in the league."
                )
        )
    }

    @SlackThreadMessageListener("createTournament")
    fun createTournament(
            @SlackChannelId channel: String,
            @SlackThreadId thread: String) : SlackReactionResponse {
        val league = leagueService.findLeagueByThreadAndState(channel, thread, League.LeagueState.STARTED) ?: return SlackReactionResponse("confused")

        tournamentService.createTournaments(league)

        return SlackReactionResponse("ok_hand")
    }
}