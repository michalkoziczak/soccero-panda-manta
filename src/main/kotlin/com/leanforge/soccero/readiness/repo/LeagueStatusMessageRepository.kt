package com.leanforge.soccero.readiness.repo

import com.leanforge.game.slack.listener.SlackChannelId
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.readiness.domain.LeagueStatusMessage
import com.leanforge.soccero.readiness.domain.ReadinessMessage
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LeagueStatusMessageRepository : MongoRepository<LeagueStatusMessage, UUID> {

    fun findTopByOrderByCreationDateDesc() : LeagueStatusMessage?
    fun findTopByLeagueNameAndCompetitionOrderByCreationDateDesc(leagueName: String, competition: Competition) : LeagueStatusMessage?
}