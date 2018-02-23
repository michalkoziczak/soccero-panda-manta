package com.leanforge.soccero.league.repo

import com.leanforge.soccero.league.domain.LeaguePlayer
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*
import java.util.stream.Stream

interface LeaguePlayerRepository : MongoRepository<LeaguePlayer, UUID> {
    fun findAllByLeagueName(name: String) : Stream<LeaguePlayer>
}