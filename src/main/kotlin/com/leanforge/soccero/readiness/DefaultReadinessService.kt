package com.leanforge.soccero.readiness

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
        val message = slackService.sendChannelMessage(channelId, "Are you ready to play? Click :heavy_plus_sign:\n\nRemember to update your status.", "heavy_plus_sign")
        readinessMessageRepository.save(ReadinessMessage(message.timestamp, message.channelId))
    }

    private fun isReadinessMessage(message: SlackMessage): Boolean {
        return readinessMessageRepository.existsBySlackChannelIdAndSlackMessageId(message.channelId, message.timestamp)
    }

    override fun markEveryoneBusy() {
        val allBusy = readinessRepository.findAll()
                .map { it.copy(state = Readiness.State.BUSY) }
                .toList()
        readinessRepository.save(allBusy)
    }

    override fun readyPlayers() : Set<String> {
        return readinessRepository
                .findAllByState(Readiness.State.READY)
                .map { it.slackId }
                .toSet()
    }
}