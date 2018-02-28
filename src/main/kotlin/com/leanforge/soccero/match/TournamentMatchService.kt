package com.leanforge.soccero.match

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.match.domain.MatchResult
import com.leanforge.soccero.match.domain.TournamentMatch
import com.leanforge.soccero.match.exception.MissingPlayerException
import com.leanforge.soccero.match.exception.WinnersCollisionException
import com.leanforge.soccero.match.repo.MatchResultRepository
import com.leanforge.soccero.match.repo.TournamentMatchRepository
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TournamentMatchService @Autowired constructor(
        private val tournamentMatchRepository: TournamentMatchRepository,
        private val matchResultRepository: MatchResultRepository,
        private val slackService: SlackService) {

    fun createMatch(channel: String, league: League, competition: Competition, teamA: LeagueTeam, teamB: LeagueTeam) {
        val message = slackService.sendChannelMessage(channel, createMatchMessage(teamA, teamB, competition), "trophy")
        tournamentMatchRepository.save(TournamentMatch(league.name, competition, setOf(teamA, teamB), message.channelId, message.timestamp))
        slackService.sendChannelMessage(channel, queueMessage(teamA, teamB, competition))
    }

    fun registerResult(winningSlackId: String, slackMessage: SlackMessage) : MatchResult? {
        val match = tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(slackMessage.timestamp, slackMessage.channelId) ?: return null
        val winningTeam = match.competitors.find {
            it.slackIds.contains(winningSlackId)
        } ?: throw MissingPlayerException(winningSlackId, match.leagueName, match.competition.label())

        val result = matchResultRepository.save(MatchResult(match.leagueName, match.competition, match.competitors.minusElement(winningTeam).first(), winningTeam, match.uuid))

        val winnersCount = matchResultRepository.findAllByMatchId(result.matchId)
                .map { it.winner }
                .distinct()
                .count()

        if (winnersCount > 1) {
            throw WinnersCollisionException(winningSlackId)
        }

        return result
    }

    private fun createMatchMessage(teamA: LeagueTeam, teamB: LeagueTeam, competition: Competition) : String {
        return "${teamKeywordsLabel(teamA)} vs ${teamKeywordsLabel(teamB)} - ${competition.label()}\n" +
                "Did you win? Click :trophy:";
    }

    private fun queueMessage(teamA: LeagueTeam, teamB: LeagueTeam, competition: Competition) : String {
        return "startGame p4 `${competition.label()} - ${teamLabel(teamA)} vs ${teamLabel(teamB)}`";
    }

    private fun teamLabel(team: LeagueTeam) : String {
        return team.slackIds.map { slackService.getRealNameById(it) }
                .sorted()
                .joinToString(" & ")
    }

    private fun teamKeywordsLabel(team: LeagueTeam) : String {
        return team.slackIds.map { "<@$it>" }
                .joinToString(" & ")
    }
}