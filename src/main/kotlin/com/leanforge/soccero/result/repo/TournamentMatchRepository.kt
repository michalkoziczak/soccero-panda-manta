package com.leanforge.soccero.result.repo

import com.leanforge.soccero.result.domain.TournamentMatch
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*
import java.util.stream.Stream

interface TournamentMatchRepository : MongoRepository<TournamentMatch, UUID> {

    fun findAllByLeagueName(leagueName: String): Stream<TournamentMatch>
    fun findOneBySlackMessageIdAndSlackChannelId(slackMessageId: String, slackChannelId: String) : TournamentMatch?

}