package com.leanforge.soccero.readiness

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.readiness.domain.LeagueStatusMessage
import com.leanforge.soccero.readiness.repo.LeagueStatusMessageRepository
import com.leanforge.soccero.result.TournamentMatchService
import com.leanforge.soccero.result.domain.MatchResult
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.TournamentService
import com.leanforge.soccero.tournament.domain.TournamentState
import com.leanforge.soccero.updater.UpdateMarkService
import com.leanforge.soccero.updater.domain.UpdateMark
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
        private val updateMarkService: UpdateMarkService,
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

    @Scheduled(fixedDelay = 10000)
    fun updateLeagueReadiness() {
        if (updateMarkService.needsUpdate(UpdateMark.Type.LEAGUE_READINESS)) {
            updateStatusMessagesForAllStartedLeagues()
            updateMarkService.markUpdated(UpdateMark.Type.LEAGUE_READINESS)
        }
    }

    @Scheduled(fixedDelay = 60000)
    fun scheduleReadyGames() {
        val battle = findRandomBattleInLowestRound() ?: return

        val teams = battle.third.toList()
        teams.flatMap { it.slackIds }
                .onEach { readinessService.markBusy(it) }
        tournamentMatchService.createMatch(battle.first, battle.second.tournament.competition, teams[0], teams[1])
        updateStatusMessagesForAllStartedLeagues()
    }

    private fun findRandomBattleInLowestRound(): Triple<League, TournamentState, Set<LeagueTeam>>? {
        val readyCompetitors = allReadyCompetitors().shuffled()
        val lowestRound = readyCompetitors.map { it.second.round }
                .distinct()
                .min()
        return readyCompetitors
                .firstOrNull { it.second.round == lowestRound }
    }

    private fun allReadyCompetitors() : List<Triple<League, TournamentState, Set<LeagueTeam>>> {
        val readyPlayers = readinessService.readyPlayers()
        return leagueService.findAllStarted()
                .flatMap { league ->
                    val currentState = currentState(league)
                    val finale = isFinale(currentState)
                    currentState
                            .filter { !it.tournament.isFinalRound() || finale }
                            .flatMap { state ->
                                state.pendingCompetitors
                                        .filter { c -> c.all { readyPlayers.containsAll(it.slackIds) } }
                                        .map { Triple(league, state, it) }
                            }
                }
    }

    private fun currentState(league: League) : List<TournamentState> {
        return league.competitions
                .map {tournamentService.currentState(league, it, tournamentMatchService.getResults(league.name, it))}
    }

    private fun isFinale(state: List<TournamentState>): Boolean {
        return state.all { it.tournament.isFinalRound() }
    }

    fun createNewStatusMessageForAllStartedLeagues() {
        markEveryoneBusy()
        invalidateAllStatusMessages()
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

    private fun invalidateAllStatusMessages() {
        val messages = leagueStatusMessageRepository.findAll()

        messages.onEach { msg ->
            slackService.updateMessage(msg.toSlackMessage(), "_This message is outdated_")
        }

        if (messages.isNotEmpty()) {
            leagueStatusMessageRepository.delete(messages)
        }
    }

    private fun sendCommercials(currentRound: TournamentState, playersReady: Set<String>) {
        currentRound.pendingCompetitors.forEach { comp ->
            comp.forEach {
                sendCommercials(it, playersReady, comp.minus(it).single())
            }
        }
    }

    private fun sendCommercials(team: LeagueTeam, playersReady: Set<String>, opponent: LeagueTeam) {
        val readyPlayers = team.slackIds.count { playersReady.contains(it) }
        if ( readyPlayers > 0 && readyPlayers < team.size()) {
            team.slackIds
                    .filter { !playersReady.contains(it) }
                    .forEach { readinessService.trySendMateReadinessMessage(it) }
        }
        if (!playersReady.containsAll(team.slackIds) && playersReady.containsAll(opponent.slackIds)) {
            team.slackIds
                    .filter { !playersReady.contains(it) }
                    .forEach { readinessService.trySendOpponentReadinessMessage(it) }
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
        return "${league.name} ${competition.label()}".replace(" ", "_!_");
    }

    private fun statusMessage(round: TournamentState, playersReady: Set<String>, hash: String) : String {
        if (round.tournament.isFinished()) {
            return ":trophy: #${round.round + 1} `${round.tournament.competition.label()}`\n" +
                    ":checkered_flag: Finished!\n" +
                    ":balloon: :balloon: :balloon:" +
                    "\n\nTree view: http://soccero-panda-manta.playroom.leanforge.pl/#$hash"
        }

        var addon = ""
        if (round.tournament.isFinalRound()) {
            addon = "\n:waving_white_flag: Final round! _(Finale can be played when all competitions reach final round)_"
        }
        return ":trophy: #${round.round + 1} `${round.tournament.competition.label()}`$addon\n" +
                listedCompetitors(round.tournament.competitors(), round.currentRoundResults, playersReady) +
                "\n\nTree view: http://soccero-panda-manta.playroom.leanforge.pl/#$hash"
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