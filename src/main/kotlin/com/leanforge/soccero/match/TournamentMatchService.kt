package com.leanforge.soccero.match

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.league.parser.MissingCompetitionException
import com.leanforge.soccero.match.domain.MatchResult
import com.leanforge.soccero.match.domain.TournamentMatch
import com.leanforge.soccero.match.exception.AmbiguousPlayerToTeamMappingException
import com.leanforge.soccero.match.exception.MissingPlayerException
import com.leanforge.soccero.match.exception.WinnersCollisionException
import com.leanforge.soccero.match.repo.MatchResultRepository
import com.leanforge.soccero.match.repo.TournamentMatchRepository
import com.leanforge.soccero.queue.QueueService
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class TournamentMatchService @Autowired constructor(
        private val tournamentMatchRepository: TournamentMatchRepository,
        private val matchResultRepository: MatchResultRepository,
        private val queueService: QueueService,
        private val slackService: SlackService) {

    fun createMatch(league: League, competition: Competition, players: Set<String>) {
        val teams = league.teams
                .filter { it.size() == competition.players }
                .filter { team -> team.slackIds.any { players.contains(it) } }
                .toList()

        if (teams.size != 2) {
            throw AmbiguousPlayerToTeamMappingException("Listen! Players are not members of exactly two teams. Consider to use MySQL to solve that.")
        }

        val playingPlayers = teams.flatMap({ it.slackIds }).toSet()

        val excludedPlayer = players.firstOrNull { !playingPlayers.contains(it) }

        if (excludedPlayer != null) {
            throw MissingPlayerException(excludedPlayer, league.name, competition.label())
        }

        createMatch(league, competition, teams[0], teams[1])
    }

    fun createMatch(league: League, competition: Competition, teamA: LeagueTeam, teamB: LeagueTeam) {
        if (!league.competitions.contains(competition)) {
            throw MissingCompetitionException()
        }
        queueService.triggerGameScheduler(competition, setOf(teamA, teamB))
        val message = slackService.sendChannelMessage(league.slackChannelId, createMatchMessage(teamA, teamB, competition, null), "trophy")
        tournamentMatchRepository.save(TournamentMatch(league.name, competition, setOf(teamA, teamB), message.channelId, message.timestamp))
    }

    fun registerResult(winningSlackId: String, slackMessage: SlackMessage) : MatchResult? {
        val match = tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(slackMessage.timestamp, slackMessage.channelId) ?: return null
        val competitors = match.competitors.toList()


        val winningTeam = findWinningTeam(winningSlackId, match)

        val result = matchResultRepository.save(MatchResult(match.leagueName, match.competition, match.competitors.minusElement(winningTeam).first(), winningTeam, match.uuid))

        val winners = matchResultRepository.findAllByMatchId(match.uuid)
                .map { it.winner }
                .collect(Collectors.toSet())

        if (winners.size > 1) {
            slackService.updateMessage(slackMessage, createMatchMessage(competitors[0], competitors[1], match.competition, null))
            throw WinnersCollisionException(winningSlackId)
        }

        val winner = winners.singleOrNull()
        slackService.updateMessage(slackMessage, createMatchMessage(competitors[0], competitors[1], match.competition, winner))

        return result
    }

    fun removeResult(winningSlackId: String, slackMessage: SlackMessage) : MatchResult? {
        val match = tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(slackMessage.timestamp, slackMessage.channelId) ?: return null
        val competitors = match.competitors.toList()
        val winningTeam = findWinningTeam(winningSlackId, match)
        val result : MatchResult? = matchResultRepository.findAllByMatchId(match.uuid)
                .filter { it.winner == winningTeam }
                .findFirst()
                .orElse(null)
        if (result != null) {
            matchResultRepository.delete(result)
        }

        val winners = matchResultRepository.findAllByMatchId(match.uuid)
                .map { it.winner }
                .collect(Collectors.toSet())

        if (winners.size > 1) {
            slackService.updateMessage(slackMessage, createMatchMessage(competitors[0], competitors[1], match.competition, null))
        } else {
            val winner = winners.singleOrNull()
            slackService.updateMessage(slackMessage, createMatchMessage(competitors[0], competitors[1], match.competition, winner))
        }

        return result
    }

    fun listResults(leagueName: String, competition: Competition) : String {
        return "Results:\n>>>" + getResults(leagueName, competition)
                .mapIndexed { index, it -> "${index + 1}. " + matchLineMessage(it.winner, it.loser, it.competition, it.winner) }
                .joinToString("\n")
    }

    fun getResults(leagueName: String, competition: Competition) : List<MatchResult> {
        val finalResults = mutableListOf<MatchResult>()
        val duplicatedResults = mutableListOf<MatchResult>()
        matchResultRepository.findAllByLeagueNameAndCompetition(leagueName, competition)
                .sorted(Comparator.comparing({ it : MatchResult -> it.createDate }))
                .forEachOrdered { result ->
                    if (finalResults.all({it.uuid != result.uuid})) {
                        finalResults.add(result)
                    } else {
                        duplicatedResults.add(result)
                    }
                }

        duplicatedResults.onEach { duplicated ->
            finalResults.removeIf { final -> final.uuid == duplicated.uuid && final.winner != duplicated.winner }
        }

        return finalResults.toList()
    }

    private fun findWinningTeam(slackId: String, match: TournamentMatch) : LeagueTeam {
        return match.competitors.find {
            it.slackIds.contains(slackId)
        } ?: throw MissingPlayerException(slackId, match.leagueName, match.competition.label())
    }

    private fun createMatchMessage(teamA: LeagueTeam, teamB: LeagueTeam, competition: Competition, winner: LeagueTeam?) : String {
        return "${matchLineMessage(teamA, teamB, competition, winner)}`\n" +
                "Did you win? Click :trophy:";
    }

    private fun teamKeywordsLabel(team: LeagueTeam, winner: LeagueTeam?) : String {
        var prefix = ""
        var postfix = ""
        if (winner != null && team == winner) {
            prefix = ":sports_medal: "
        }

        if (winner != null && team != winner) {
            prefix = "~"
            postfix = "~"
        }
        return prefix + "(" + team.slackIds.map { "<@$it>" }
                .joinToString(" & ") + ")" + postfix
    }

    private fun matchLineMessage(teamA: LeagueTeam, teamB: LeagueTeam, competition: Competition, winner: LeagueTeam?) : String {
        return "${teamKeywordsLabel(teamA, winner)} vs ${teamKeywordsLabel(teamB, winner)} - `${competition.label()}`"
    }
}