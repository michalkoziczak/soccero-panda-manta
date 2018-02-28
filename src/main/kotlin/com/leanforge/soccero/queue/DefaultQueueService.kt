package com.leanforge.soccero.queue

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.queue.domain.QueueConfig
import com.leanforge.soccero.queue.repo.QueueConfigRepository
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DefaultQueueService @Autowired constructor(
        private val slackService: SlackService,
        private val queueConfigRepository: QueueConfigRepository) : QueueService {
    override fun listConfig(): String {
        return "Defined queues:\n" + queueConfigRepository.findAll()
                .map { ":gear: ${it.competition.label()} -> <#${it.slackChannelId}|${slackService.getChannelName(it.slackChannelId)}> _(p${it.priority})_" }
                .joinToString("\n")
    }

    override fun removeConfig(competition: Competition) {
        queueConfigRepository.delete(competition)
    }

    override fun createConfig(competition: Competition, destinationSlackChannel: String, priority: Int) {
        queueConfigRepository.save(QueueConfig(competition, destinationSlackChannel, priority))
    }

    override fun triggerGameScheduler(competition: Competition, teams: Set<LeagueTeam>) {
        val config = queueConfigRepository.findOne(competition) ?: return
        slackService.sendChannelMessage(config.slackChannelId, queueMessage(teams, competition, config.priority))
    }

    private fun queueMessage(teams: Set<LeagueTeam>, competition: Competition, priority: Int) : String {
        val teamsString = teams
                .map { "(${teamLabel(it)})" }
                .joinToString(" vs ")
        return "startGame p$priority `${competition.label()} - $teamsString`";
    }

    private fun teamLabel(team: LeagueTeam) : String {
        return team.slackIds.map { slackService.getRealNameById(it) }
                .sorted()
                .joinToString(" & ")
    }
}