package com.leanforge.soccero.result.repo

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.result.domain.MatchResult
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*
import java.util.stream.Stream

@Repository
interface MatchResultRepository : MongoRepository<MatchResult, UUID> {
    fun findAllByMatchId(matchId: UUID) : Stream<MatchResult>
    fun findAllByLeagueNameAndCompetition(leagueName: String, competition: Competition) : Stream<MatchResult>
    fun findAllByLeagueName(leagueName: String) : Stream<MatchResult>
}