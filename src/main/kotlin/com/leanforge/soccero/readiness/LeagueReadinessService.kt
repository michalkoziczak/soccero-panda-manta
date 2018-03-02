package com.leanforge.soccero.readiness

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.DefaultLeagueService
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.match.TournamentMatchService
import com.leanforge.soccero.match.domain.MatchResult
import com.leanforge.soccero.readiness.domain.LeagueStatusMessage
import com.leanforge.soccero.readiness.repo.LeagueStatusMessageRepository
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.TournamentService
import com.leanforge.soccero.tournament.domain.Tournament
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.*
import java.time.temporal.ChronoUnit


@Service
class LeagueReadinessService

@Autowired constructor(
        private val readinessService: ReadinessService,
        private val leagueStatusMessageRepository: LeagueStatusMessageRepository,
        private val tournamentService: TournamentService,
        private val tournamentMatchService: TournamentMatchService,
        private val leagueService: LeagueService,
        private val slackService: SlackService,
        @Qualifier("resetTime") private val resetTime: LocalTime) {

    @Scheduled(fixedDelay = 600000)
    fun resendStatusMessageIfNeeded() {
        if (LocalTime.now().isBefore(resetTime)) {
            return
        }
        val lastUpdate = leagueStatusMessageRepository.findTopByOrderByCreationDateDesc()?.creationDate ?: Instant.now().minus(60, ChronoUnit.DAYS)
        val lastUpdateDate = LocalDateTime.ofInstant(lastUpdate, ZoneId.systemDefault()).toLocalDate()
        if (LocalDate.now() == lastUpdateDate) {
            return
        }

        createNewStatusMessageForAllStartedLeagues()
    }

    @Scheduled(fixedDelay = 60000)
    fun scheduleReadyGames() {
        val battle = allReadyCompetitors().shuffled()
                .firstOrNull() ?: return

        val teams = battle.third.toList()
        teams.flatMap { it.slackIds }
                .onEach { readinessService.markBusy(it) }
        tournamentMatchService.createMatch(battle.first, battle.second, teams[0], teams[1])
        updateStatusMessagesForAllStartedLeagues()
    }

    private fun allReadyCompetitors() : List<Triple<League, Competition, Set<LeagueTeam>>> {
        val readyPlayers = readinessService.readyPlayers()
        return leagueService.findAllStarted()
                .flatMap { league ->
                    league.competitions
                            .flatMap { competition ->
                                tournamentService.pendingCompetitors(league, competition, tournamentMatchService.getResults(league.name, competition))
                                        .filter { c -> c.all { readyPlayers.containsAll(it.slackIds) } }
                                        .map { Triple(league, competition, it) }
                            }
                }
    }

    fun createNewStatusMessageForAllStartedLeagues() {
        markEveryoneBusy()
        leagueService.findAllStarted()
                .onEach { league ->
                    league.competitions
                            .onEach { createNewStatusMessage(league, it) }
                    readinessService.sendGenericReadinessMessage(league.slackChannelId ?: "")
                }
    }

    fun updateStatusMessagesForAllStartedLeagues() {
        leagueService.findAllStarted()
                .onEach { league ->
                    league.competitions
                            .onEach { updateLastStatusMessage(league, it) }
                }
    }

    private fun markEveryoneBusy() {
        readinessService.markEveryoneBusy()
    }

    private fun updateLastStatusMessage(league: League, competition: Competition) {
        val slackMessage = leagueStatusMessageRepository.findTopByLeagueNameAndCompetitionOrderByCreationDateDesc(league.name, competition)
                ?.toSlackMessage() ?: return

        val allResults = tournamentMatchService.getResults(league.name, competition)
        val currentResults = tournamentService.currentResults(league, competition, allResults)
        val currentRound = tournamentService.currentState(league, competition, allResults)

        val statusMessage = statusMessage(currentRound, currentResults)
        slackService.updateMessage(slackMessage, statusMessage)
    }


    private fun createNewStatusMessage(league: League, competition: Competition) {
        val allResults = tournamentMatchService.getResults(league.name, competition)
        val currentResults = tournamentService.currentResults(league, competition, allResults)
        val currentRound = tournamentService.currentState(league, competition, allResults)

        val statusMessage = statusMessage(currentRound, currentResults)
        val msg = slackService.sendChannelMessage(league.slackChannelId, statusMessage)
        leagueStatusMessageRepository.save(LeagueStatusMessage(msg.timestamp, msg.channelId, league.name, competition))
    }

    private fun statusMessage(round: Tournament, roundResults: List<MatchResult>) : String {
        return ":trophy: `${round.competition.label()}`\n>>>" +
                listedCompetitors(round.competitors(), roundResults)
    }

    private fun listedCompetitors(competitors: List<Set<LeagueTeam>>, roundResults: List<MatchResult>) : String {
        return competitors
                .mapIndexed { index, teams -> "${index + 1}. ${competitorsLine(teams, roundResults)}" }
                .joinToString("\n")
    }

    private fun teamKeywordsLabel(team: LeagueTeam, result: MatchResult?) : String {
        var pre = ""
        var post = ""
        var showReadyIcon = true

        if (team == result?.winner) {
            pre = ":sports_medal: "
            showReadyIcon = false
        }

        if (team == result?.loser) {
            pre = "~"
            post = "~"
            showReadyIcon = false
        }

        return "$pre(" + team.slackIds.map { "${readyIcon(showReadyIcon, it)}<@$it>" }
                .joinToString(" & ") + ")$post"
    }

    private fun readyIcon(show: Boolean, slackId: String) : String {
        if (!show) {
            return ""
        }
        if (readinessService.isReady(slackId)) {
            return ":black_medium_small_square: "
        }
        return ":white_medium_small_square: "
    }

    private fun competitorsLine(teams: Set<LeagueTeam>, roundResults: List<MatchResult>) : String {
        val result = roundResults.singleOrNull { it.hasTeams(teams) }
        return teams.map { teamKeywordsLabel(it, result) }
                .joinToString(" vs ")
    }
}