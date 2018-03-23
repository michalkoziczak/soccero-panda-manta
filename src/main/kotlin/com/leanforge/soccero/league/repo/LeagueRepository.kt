package com.leanforge.soccero.league.repo

import com.leanforge.soccero.league.domain.League
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueRepository : MongoRepository<League, String> {

    fun findOneBySlackMessageIdAndSlackChannelId(slackMessageId: String, slackChannelId: String) : League?
}