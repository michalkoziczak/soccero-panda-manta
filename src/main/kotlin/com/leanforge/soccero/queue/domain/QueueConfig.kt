package com.leanforge.soccero.queue.domain

import com.leanforge.soccero.league.domain.Competition
import org.springframework.data.annotation.Id

data class QueueConfig(
        @Id val competition: Competition,
        val slackChannelId: String,
        val priority: Int
)