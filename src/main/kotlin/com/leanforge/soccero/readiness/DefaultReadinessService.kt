package com.leanforge.soccero.readiness

import com.leanforge.game.slack.SlackAction
import com.leanforge.game.slack.SlackActions
import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.readiness.domain.Readiness
import com.leanforge.soccero.readiness.domain.ReadinessMessage
import com.leanforge.soccero.readiness.repo.ReadinessMessageRepository
import com.leanforge.soccero.readiness.repo.ReadinessRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

interface ReadinessService {
    fun markReady(slackMessage: SlackMessage, slackId: String)
    fun markBusy(slackMessage: SlackMessage, slackId: String)
    fun markReady(slackId: String)
    fun markBusy(slackId: String)
    fun isReady(slackId: String): Boolean
    fun sendGenericReadinessMessage(channelId: String)
    fun trySendPersonalReadinessMessage(userId: String)
    fun trySendMateReadinessMessage(userId: String)
    fun markEveryoneBusy()
    fun readyPlayers() : Set<String>
}

@Service
class DefaultReadinessService @Autowired constructor(
        private val readinessRepository: ReadinessRepository,
        private val readinessMessageRepository: ReadinessMessageRepository,
        private val slackService: SlackService) : ReadinessService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun markReady(slackMessage: SlackMessage, slackId: String) {
        if (!isReadinessMessage(slackMessage)) {
            return
        }

        markReady(slackId)
        trySendPersonalReadinessMessage(slackId)
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
        val message = slackService.sendChannelMessage(channelId, "_Remember to update your status_", readyActions("state"))
        readinessMessageRepository.save(ReadinessMessage(message.timestamp, message.channelId))
    }

    override fun trySendPersonalReadinessMessage(userId: String) {
        trySendPersonalReadinessMessage(userId, "You are ready. Great job. In case this is no longer true, click `busy` button:")
    }

    override fun trySendMateReadinessMessage(userId: String) {
        trySendPersonalReadinessMessage(userId, "Your mate is ready. Maybe you can join him?")
    }

    private fun trySendPersonalReadinessMessage(userId: String, msg: String) {
        try {
            val lastMessage = readinessMessageRepository.findTopByUserIdOrderByCreatedOnDesc(userId)
            val isOldEnough = lastMessage?.createdOn?.isBefore(Instant.now().minus(24, ChronoUnit.HOURS)) ?: true
            if (!isOldEnough) {
                return;
            }
            val message = slackService.sendDirectMessage(userId, msg, readyActions("personalState"))
            readinessMessageRepository.save(ReadinessMessage(message.timestamp, message.channelId, userId))
        } catch (e: Exception) {
            logger.error("Can't send a message", e)
        }
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

    private fun readyActions(actionName: String): SlackActions {
        return SlackActions(
                "Your status",
                "Are you ready to play a game?",
                "You can't use buttons, but you can add :heavy_plus_sign: reaction instead.",
                "#3AA3E3",
                SlackAction.button(actionName, "I'm ready!", "ready"),
                SlackAction.button(actionName, "Sorry, I'm busy...", "busy")
        )
    }
}
