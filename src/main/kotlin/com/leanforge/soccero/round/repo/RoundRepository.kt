package com.leanforge.soccero.round.repo

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.round.Round
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*
import java.util.stream.Stream

@Repository
interface RoundRepository : MongoRepository<Round, UUID> {

    fun findAllByLeague(league: String) : Stream<Round>

    fun findAllByCompetitionAndLeague(competition: Competition, league: String) : Stream<Round>
}