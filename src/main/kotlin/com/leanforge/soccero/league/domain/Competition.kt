package com.leanforge.soccero.league.domain

data class Competition(
        var name: String = "Competition",
        var players: Int = 2
) {

    fun label() : String {
        return "$name ${players}vs${players}"
    }
}
