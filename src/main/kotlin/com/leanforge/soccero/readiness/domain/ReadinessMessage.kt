package com.leanforge.soccero.readiness.domain

import org.springframework.data.annotation.Id
import java.util.*

data class ReadinessMessage(
        val slackMessageId: String,
        val slackChannelId: String,
        @Id val uuid: UUID = UUID.randomUUID()) {
}