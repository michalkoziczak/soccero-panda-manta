package com.leanforge.soccero.readiness

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.result.TournamentMatchService
import com.leanforge.soccero.result.domain.MatchResult
import com.leanforge.soccero.readiness.domain.LeagueStatusMessage
import com.leanforge.soccero.readiness.repo.LeagueStatusMessageRepository
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.TournamentService
import com.leanforge.soccero.tournament.domain.TournamentState
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
        val currentRound = tournamentService.currentState(league, competition, allResults)
        val playersReady = readinessService.readyPlayers()

        val statusMessage = statusMessage(currentRound, playersReady, hash(league, competition))
        slackService.updateMessage(slackMessage, statusMessage)
        sendCommercials(currentRound, playersReady);
    }

    private fun sendCommercials(currentRound: TournamentState, playersReady: Set<String>) {
        currentRound.pendingCompetitors.forEach { comp ->
            comp.forEach {
                sendCommercials(it, playersReady)
            }
        }
    }

    private fun sendCommercials(team: LeagueTeam, playersReady: Set<String>) {
        val readyPlayers = team.slackIds.count { playersReady.contains(it) }
        if ( readyPlayers > 0 && readyPlayers < team.size()) {
            team.slackIds
                    .filter { !playersReady.contains(it) }
                    .forEach { readinessService.trySendMateReadinessMessage(it) }
        }
    }


    private fun createNewStatusMessage(league: League, competition: Competition) {
        val allResults = tournamentMatchService.getResults(league.name, competition)
        val currentRound = tournamentService.currentState(league, competition, allResults)

        val statusMessage = statusMessage(currentRound, emptySet(), hash(league, competition))
        val msg = slackService.sendChannelMessage(league.slackChannelId, statusMessage)
        leagueStatusMessageRepository.save(LeagueStatusMessage(msg.timestamp, msg.channelId, league.name, competition))
    }

    private fun hash(league: League, competition: Competition): String {
        return "${league.name} ${competition.label()}".replace(" ", "%_");
    }

    private fun statusMessage(round: TournamentState, playersReady: Set<String>, hash: String) : String {
        return ":trophy: #${round.round + 1} `${round.tournament.competition.label()}`\n" +
                listedCompetitors(round.tournament.competitors(), round.currentRoundResults, playersReady) +
                "\n\nhttp://soccero-panda-manta.dev.kende.pl/#$hash"
    }

    private fun listedCompetitors(competitors: List<Set<LeagueTeam>>, roundResults: List<MatchResult>, playersReady: Set<String>) : String {
        return competitors
                .mapIndexed { index, teams -> "> ${index + 1}. ${competitorsLine(teams, roundResults, playersReady)}" }
                .joinToString("\n")
    }

    private fun teamLabel(team: LeagueTeam, result: MatchResult?, playersReady: Set<String>) : String {
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

        return "$pre(" + team.slackIds.map { "${readyIcon(showReadyIcon, it, playersReady)}${slackService.getRealNameById(it)}" }
                .joinToString(" & ") + ")$post"
    }

    private fun readyIcon(show: Boolean, slackId: String, playersReady: Set<String>) : String {
        if (!show) {
            return ""
        }
        if (playersReady.contains(slackId)) {
            return ":low_brightness:"
        }
        return ":black_small_square:"
    }

    private fun competitorsLine(teams: Set<LeagueTeam>, roundResults: List<MatchResult>, playersReady: Set<String>) : String {
        val result = roundResults.singleOrNull { it.hasTeams(teams) }
        return teams.map { teamLabel(it, result, playersReady) }
                .joinToString(" vs ")
    }
}