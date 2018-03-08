package com.leanforge.soccero.readiness.domain

import org.springframework.data.annotation.Id
import java.time.Instant
import java.util.*

data class ReadinessMessage(
        val slackMessageId: String,
        val slackChannelId: String,
        val userId: String? = null,
        val createdOn: Instant? = Instant.now(),
        @Id val uuid: UUID = UUID.randomUUID())