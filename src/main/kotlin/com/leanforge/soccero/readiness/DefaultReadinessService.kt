package com.leanforge.soccero.readiness

import com.leanforge.game.slack.SlackAction
import com.leanforge.game.slack.SlackActions
import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.readiness.domain.Readiness
import com.leanforge.soccero.readiness.domain.ReadinessMessage
import com.leanforge.soccero.readiness.repo.ReadinessMessageRepository
import com.leanforge.soccero.readiness.repo.ReadinessRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface ReadinessService {
    fun markReady(slackMessage: SlackMessage, slackId: String)
    fun markBusy(slackMessage: SlackMessage, slackId: String)
    fun markReady(slackId: String)
    fun markBusy(slackId: String)
    fun isReady(slackId: String): Boolean
    fun sendGenericReadinessMessage(channelId: String)
    fun markEveryoneBusy()
    fun readyPlayers() : Set<String>
}

@Service
class DefaultReadinessService @Autowired constructor(
        private val readinessRepository: ReadinessRepository,
        private val readinessMessageRepository: ReadinessMessageRepository,
        private val slackService: SlackService) : ReadinessService {


    override fun markReady(slackMessage: SlackMessage, slackId: String) {
        if (!isReadinessMessage(slackMessage)) {
            return
        }

        markReady(slackId)
    }

    override fun markBusy(slackMessage: SlackMessage, slackId: String) {
        if (!isReadinessMessage(slackMessage)) {
            return
        }

        markBusy(slackId)
    }

    override fun markReady(slackId: String) {
        readinessRepository.save(Readiness(slackId, Readiness.State.READY))
    }

    override fun markBusy(slackId: String) {
        readinessRepository.save(Readiness(slackId, Readiness.State.BUSY))
    }

    override fun isReady(slackId: String): Boolean {
        val readiness = readinessRepository.findOne(slackId) ?: return false;

        return readiness.state == Readiness.State.READY
    }

    override fun sendGenericReadinessMessage(channelId: String) {
        val actions = SlackActions(
                "Your status",
                "Are you ready to play a game?",
                "You can't use buttons, but you can add :heavy_plus_sign: reaction instead.",
                "#3AA3E3",
                SlackAction.button("state", "I'm ready!", "ready"),
                SlackAction.button("state", "Sorry, Busy...", "busy")
        )
        val message = slackService.sendChannelMessage(channelId, "_Remember to update your status_", actions)
        readinessMessageRepository.save(ReadinessMessage(message.timestamp, message.channelId))
    }

    private fun isReadinessMessage(message: SlackMessage): Boolean {
        return readinessMessageRepository.existsBySlackChannelIdAndSlackMessageId(message.channelId, message.timestamp)
    }

    override fun markEveryoneBusy() {
        val allBusy = readinessRepository.findAllByState(Readiness.State.READY)
                .map { it.copy(state = Readiness.State.BUSY) }
                .toList()
        if (allBusy.isEmpty()) {
            return;
        }
        readinessRepository.save(allBusy)
    }

    override fun readyPlayers() : Set<String> {
        return readinessRepository
                .findAllByState(Readiness.State.READY)
                .map { it.slackId }
                .toSet()
    }
}