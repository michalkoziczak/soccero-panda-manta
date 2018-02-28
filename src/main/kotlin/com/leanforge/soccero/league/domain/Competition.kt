package com.leanforge.soccero.league.domain

import java.io.Serializable

data class Competition(
        var name: String = "Competition",
        var players: Int = 2
) : Serializable {

    fun label() : String {
        return "$name ${players}vs${players}"
    }
}
