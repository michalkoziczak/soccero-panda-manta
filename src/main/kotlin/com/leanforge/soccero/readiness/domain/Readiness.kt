package com.leanforge.soccero.readiness.domain

import org.springframework.data.annotation.Id

data class Readiness(
        @Id val slackId: String,
        val state: State) {

    enum class State {
        READY, BUSY
    }
}