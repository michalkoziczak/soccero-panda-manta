package com.leanforge.soccero.readiness.repo

import com.leanforge.game.slack.listener.SlackChannelId
import com.leanforge.soccero.readiness.domain.ReadinessMessage
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ReadinessMessageRepository : MongoRepository<ReadinessMessage, UUID> {

    fun existsBySlackChannelIdAndSlackMessageId(slackChannelId: String, slackMessageId: String) : Boolean
}