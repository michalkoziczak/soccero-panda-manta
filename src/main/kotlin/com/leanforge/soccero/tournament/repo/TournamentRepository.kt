package com.leanforge.soccero.tournament.repo

import com.leanforge.soccero.tournament.domain.Tournament
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*
import java.util.stream.Stream

@Repository
interface TournamentRepository : MongoRepository<Tournament, UUID> {

    fun findAllByName(name: String) : Stream<Tournament>
}