package com.leanforge.soccero.updater.domain

import org.springframework.data.annotation.Id

data class UpdateMark(@Id val type: Type) {

    enum class Type {
        LEAGUE_READINESS
    }
}