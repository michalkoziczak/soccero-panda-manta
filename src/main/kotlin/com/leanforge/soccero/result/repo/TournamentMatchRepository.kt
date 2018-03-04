package com.leanforge.soccero.result.repo

import com.leanforge.soccero.result.domain.TournamentMatch
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

interface TournamentMatchRepository : MongoRepository<TournamentMatch, UUID> {

    fun findOneBySlackMessageIdAndSlackChannelId(slackMessageId: String, slackChannelId: String) : TournamentMatch?

}