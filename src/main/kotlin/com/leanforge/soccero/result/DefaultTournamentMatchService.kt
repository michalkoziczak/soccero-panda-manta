package com.leanforge.soccero.result

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.league.parser.MissingCompetitionException
import com.leanforge.soccero.result.domain.MatchResult
import com.leanforge.soccero.result.domain.TournamentMatch
import com.leanforge.soccero.result.exception.AmbiguousPlayerToTeamMappingException
import com.leanforge.soccero.result.exception.FrozenResultException
import com.leanforge.soccero.result.exception.MissingPlayerException
import com.leanforge.soccero.result.exception.WinnersCollisionException
import com.leanforge.soccero.result.repo.MatchResultRepository
import com.leanforge.soccero.result.repo.TournamentMatchRepository
import com.leanforge.soccero.queue.QueueService
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.TournamentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

interface TournamentMatchService {
    fun createMatch(league: League, competition: Competition, players: Set<String>)
    fun createMatch(league: League, competition: Competition, teamA: LeagueTeam, teamB: LeagueTeam)
    fun registerResult(winningSlackId: String, slackMessage: SlackMessage) : MatchResult?
    fun removeResult(winningSlackId: String, slackMessage: SlackMessage) : MatchResult?
    fun listResults(leagueName: String, competition: Competition) : String
    fun getResults(leagueName: String, competition: Competition) : List<MatchResult>
}

@Service
class DefaultTournamentMatchService @Autowired constructor(
        private val tournamentMatchRepository: TournamentMatchRepository,
        private val matchResultRepository: MatchResultRepository,
        private val queueService: QueueService,
        private val tournamentService: TournamentService,
        private val slackService: SlackService) : TournamentMatchService {

    override fun createMatch(league: League, competition: Competition, players: Set<String>) {
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
            throw MissingPlayerException(excludedPlayer)
        }

        createMatch(league, competition, teams[0], teams[1])
    }

    override fun createMatch(league: League, competition: Competition, teamA: LeagueTeam, teamB: LeagueTeam) {
        if (!league.competitions.contains(competition)) {
            throw MissingCompetitionException()
        }

        val results = getResults(league.name, competition)
        val competitors = tournamentService.pendingCompetitors(league, competition, results)

        if (!competitors.contains(setOf(teamA, teamB))) {
            throw AmbiguousPlayerToTeamMappingException(
                    "Listen! " +
                            "There is no upcoming match for `${competition.label()}` " +
                            "${teamKeywordsLabel(teamA)} vs ${teamKeywordsLabel(teamB)}." +
                            "\n:smoking: :smoking: :smoking:")
        }

        queueService.triggerGameScheduler(competition, setOf(teamA, teamB))
        val message = slackService.sendChannelMessage(league.slackChannelId, createMatchMessage(teamA, teamB, competition, null), "trophy")
        tournamentMatchRepository.save(TournamentMatch(league.name, competition, setOf(teamA, teamB), message.channelId, message.timestamp))
    }

    override fun registerResult(winningSlackId: String, slackMessage: SlackMessage) : MatchResult? {
        val match = tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(slackMessage.timestamp, slackMessage.channelId) ?: return null
        verifyNoFrozen(winningSlackId, match)
        val winningTeam = findWinningTeam(winningSlackId, match)

        val result = matchResultRepository.save(MatchResult(match.leagueName, match.competition, match.competitors.minusElement(winningTeam).first(), winningTeam, match.uuid))

        if (updateResultMessage(match, slackMessage)) {
            throw WinnersCollisionException(winningSlackId)
        }

        return result
    }

    override fun removeResult(winningSlackId: String, slackMessage: SlackMessage) : MatchResult? {
        val match = tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(slackMessage.timestamp, slackMessage.channelId) ?: return null
        verifyNoFrozen(winningSlackId, match)

        val winningTeam = findWinningTeam(winningSlackId, match)
        val result : MatchResult? = matchResultRepository.findAllByMatchId(match.uuid)
                .filter { it.winner == winningTeam }
                .findFirst()
                .orElse(null)
        if (result != null) {
            matchResultRepository.delete(result)
        }

        updateResultMessage(match, slackMessage)

        return result
    }

    private fun verifyNoFrozen(callerId: String, match: TournamentMatch) {
        val lastUpdate = matchResultRepository.findAllByMatchId(match.uuid)
                .map { it.createDate }
                .max(Comparator.naturalOrder())
                .orElseGet({ Instant.now() })

        if (lastUpdate.isBefore(Instant.now().minus(1, ChronoUnit.HOURS))) {
            throw FrozenResultException(callerId)
        }
    }

    private fun updateResultMessage(match: TournamentMatch, slackMessage: SlackMessage) : Boolean {
        val opponents = match.competitors.toList()
        val winners = matchResultRepository.findAllByMatchId(match.uuid)
                .map { it.winner }
                .collect(Collectors.toSet())

        if (winners.size > 1) {
            slackService.updateMessage(slackMessage, createMatchMessage(opponents[0], opponents[1], match.competition, null))
            return true
        }

        val winner = winners.singleOrNull()
        slackService.updateMessage(slackMessage, createMatchMessage(opponents[0], opponents[1], match.competition, winner))
        return false
    }

    override fun listResults(leagueName: String, competition: Competition) : String {
        return "Results:\n>>>" + getResults(leagueName, competition)
                .mapIndexed { index, it -> "${index + 1}. " + matchLineMessage(it.winner, it.loser, it.competition, it.winner) }
                .joinToString("\n")
    }

    override fun getResults(leagueName: String, competition: Competition) : List<MatchResult> {

        val results = matchResultRepository.findAllByLeagueNameAndCompetition(leagueName, competition)
                .collect(Collectors.toList())
        val matches = results.map { it.matchId }
                .distinct()
                .toList()
        val nonConflictingMatches = matches
                .filter {
                    results.filter { r -> r.matchId == it }
                            .map { r -> r.winner }
                            .distinct()
                            .count() == 1
                }

        return nonConflictingMatches
                .map { m -> results.first { it.matchId == m } }
                .toList()
    }

    private fun findWinningTeam(slackId: String, match: TournamentMatch) : LeagueTeam {
        return match.competitors.find {
            it.slackIds.contains(slackId)
        } ?: throw MissingPlayerException(slackId)
    }

    private fun createMatchMessage(teamA: LeagueTeam, teamB: LeagueTeam, competition: Competition, winner: LeagueTeam?) : String {
        return "${matchLineMessage(teamA, teamB, competition, winner)}`\n" +
                "Did you win? Click :trophy:";
    }

    private fun teamKeywordsLabel(team: LeagueTeam) : String {
        return teamKeywordsLabel(team, null)
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