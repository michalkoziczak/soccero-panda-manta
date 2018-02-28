package com.leanforge.soccero.match.repo

import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.match.domain.TournamentMatch
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

interface TournamentMatchRepository : MongoRepository<TournamentMatch, UUID> {

    fun findOneBySlackMessageIdAndSlackChannelId(slackMessageId: String, slackChannelId: String) : TournamentMatch?

}