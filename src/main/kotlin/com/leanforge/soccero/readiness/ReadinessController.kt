package com.leanforge.soccero.readiness

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.listener.*
import com.leanforge.soccero.IdsExtractor
import com.leanforge.soccero.help.HelpController
import com.leanforge.soccero.help.domain.CommandArg
import com.leanforge.soccero.help.domain.CommandExample
import com.leanforge.soccero.help.domain.CommandManual
import org.springframework.beans.factory.annotation.Autowired

@SlackController
class ReadinessController
@Autowired constructor(
        private val readinessService: ReadinessService,
        private val leagueReadinessService: LeagueReadinessService)
    : HelpController.CommandManualProvider, HelpController.AdminCommandManualProvider {

    override fun listCommands(): Iterable<CommandManual> {
        return listOf(
                CommandManual(
                        name = ":heavy_plus_sign:",
                        description = "(on 'are you ready' question) Mark yourself ready",
                        isReaction = true
                ),
                CommandManual(
                        name = ":heavy_minus_sign:",
                        description = "(on 'are you ready' question) Mark yourself busy",
                        isReaction = true
                )
        )
    }

    override fun listAdminCommands(): Iterable<CommandManual> {
        return listOf(
                CommandManual(
                        name = "playerReady",
                        description = "Manually marks player as ready",
                        args = listOf(CommandArg("playerList", "(.*)", "List of players separated with space")),
                        examples = listOf(
                                CommandExample(
                                        "playerReady @player1 @player2 @player3",
                                        "Marks player1, player2 and player3 to be ready to play"
                                )
                        )
                ),
                CommandManual(
                        name = "playerBusy",
                        description = "Manually marks player as busy",
                        args = listOf(CommandArg("playerList", "(.*)", "List of players separated with space")),
                        examples = listOf(
                                CommandExample(
                                        "playerBusy @player1 @player2 @player3",
                                        "Marks player1, player2 and player3 to be busy"
                                )
                        )
                ),
                CommandManual(
                        name = "forceSendStatusList",
                        description = "Manually sends status message of started leagues, marks all players busy."
                )
        )
    }

    @SlackReactionListener("heavy_plus_sign")
    fun handleRaisedHand(@SlackUserId user: String, slackMessage: SlackMessage) {
        readinessService.markReady(slackMessage, user)
        leagueReadinessService.updateStatusMessagesForAllStartedLeagues()
    }

    @SlackReactionListener(value = "heavy_plus_sign", action = SlackReactionListener.Action.REMOVE)
    fun handleRemovedRaisedHand(@SlackUserId user: String, slackMessage: SlackMessage) {
        readinessService.markBusy(slackMessage, user)
        leagueReadinessService.updateStatusMessagesForAllStartedLeagues()

    }

    @SlackReactionListener("heavy_minus_sign")
    fun handleFist(@SlackUserId user: String, slackMessage: SlackMessage) {
        readinessService.markBusy(slackMessage, user)
        leagueReadinessService.updateStatusMessagesForAllStartedLeagues()
    }

    @SlackMessageListener("forceSendStatusList")
    fun initStatusList() {
        leagueReadinessService.createNewStatusMessageForAllStartedLeagues()
    }

    @SlackMessageListener("playerReady (.*)")
    fun manualPlayerReady(@SlackMessageRegexGroup(1) players: String) {
        IdsExtractor.extractIds(players)
                .onEach { readinessService.markReady(it) }
    }

    @SlackMessageListener("playerBusy (.*)")
    fun manualPlayerBusy(@SlackMessageRegexGroup(1) players: String) {
        IdsExtractor.extractIds(players)
                .onEach { readinessService.markBusy(it) }
    }

    @SlackActionListener(actionName = "state", actionValue = "ready")
    fun handleReadyButton(slackMessage: SlackMessage, @SlackUserId user: String) {
        readinessService.markReady(slackMessage, user)
    }

    @SlackActionListener(actionName = "state", actionValue = "busy")
    fun handleBusyButton(slackMessage: SlackMessage, @SlackUserId user: String) {
        readinessService.markBusy(slackMessage, user)
    }

    @SlackActionListener(actionName = "personalState", actionValue = "ready")
    fun handlePersonalReadyButton(slackMessage: SlackMessage, @SlackUserId user: String) {
        readinessService.markReady(slackMessage, user)
    }

    @SlackActionListener(actionName = "personalState", actionValue = "busy")
    fun handlePersonalBusyButton(slackMessage: SlackMessage, @SlackUserId user: String) {
        readinessService.markBusy(slackMessage, user)
    }
}