package com.leanforge.soccero.league

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.league.domain.LeaguePlayer
import com.leanforge.soccero.league.repo.LeaguePlayerRepository
import com.leanforge.soccero.league.repo.LeagueRepository
import com.leanforge.soccero.team.TeamServiceInterface
import com.leanforge.soccero.team.domain.LeagueTeam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Collectors

interface LeagueService {
    fun createLeague(initMessage: SlackMessage, name: String, competitions: Set<Competition>)
    fun addPlayerAndUpdateStatusMessage(message: SlackMessage, playerSlackId: String)
    fun addPlayerAndUpdateStatusMessage(name: String, playerSlackId: String)
    fun removePlayerAndUpdateStatusMessage(message: SlackMessage, playerSlackId: String)
    fun findStartedLeagueByName(name: String) : League?
    fun findOnlyStartedLeague() : League?
    fun findLeagueByThreadAndState(channelId: String, threadId: String, state: League.LeagueState) : League?
    fun pauseLeague(name: String) : League?
    fun resumeLeague(name: String) : League?
    fun updateMessage(leagueName: String)
    fun startLeague(name: String)
    fun getPendingLeagueNameForThreadId(channelId: String, threadId: String) : Optional<String>
    fun listLeagues(): String
    fun endLeague(name: String)
    fun findAllStarted() : List<League>
    fun findAllPaused() : List<League>
    fun deleteCompetition(leagueName: String, competition: Competition)
}

@Service
class DefaultLeagueService @Autowired constructor(
        private val leagueRepository: LeagueRepository,
        private val leaguePlayerRepository: LeaguePlayerRepository,
        private val slackService: SlackService,
        private val leagueMessages: LeagueMessages,
        private val teamService: TeamServiceInterface) : LeagueService {

    override fun deleteCompetition(leagueName: String, competition: Competition) {
        val league = leagueRepository.findOne(leagueName) ?: throw IllegalArgumentException("There is no league $leagueName")
        if (!league.competitions.contains(competition)) {
            throw IllegalArgumentException("There is no competition ${competition.label()} in $leagueName")
        }
        league.competitions = league.competitions.minus(competition)
        leagueRepository.save(league)
    }

    override fun createLeague(initMessage: SlackMessage, name: String, competitions: Set<Competition>) {
        requireLeagueNotPresent(name)
        val startMessage = slackService.sendChannelMessage(initMessage.channelId, leagueMessages.pendingMessage(name, emptySet(), competitions, emptyList()))
        val league = League(name)
        league.slackChannelId = startMessage.channelId
        league.slackMessageId = startMessage.timestamp
        league.competitions = competitions
        leagueRepository.save(league)
        slackService.addReactions(startMessage, "heavy_plus_sign")
    }

    override fun addPlayerAndUpdateStatusMessage(message: SlackMessage, playerSlackId: String) {
        findPendingLeagueBySlackMessage(message).ifPresent { league ->
            addPlayer(league.name, playerSlackId)
            updateMessage(league)
        }
    }

    override fun addPlayerAndUpdateStatusMessage(name: String, playerSlackId: String) {
        findPendingLeagueByName(name).ifPresent { league ->
            addPlayer(league.name, playerSlackId)
            updateMessage(league)
        }
    }

    override fun removePlayerAndUpdateStatusMessage(message: SlackMessage, playerSlackId: String) {
        findPendingLeagueBySlackMessage(message).ifPresent { league ->
            findPlayer(league.name, playerSlackId).ifPresent { leaguePlayerRepository.delete(it) }
            updateMessage(league)
        }
    }

    override fun findStartedLeagueByName(name: String) : League? {
        val league = leagueRepository.findOne(name)
        if (league.state != League.LeagueState.STARTED) {
            return null
        }

        return league
    }

    override fun findOnlyStartedLeague() : League? {
        return leagueRepository.findAll()
                .filter { it.state == League.LeagueState.STARTED }
                .singleOrNull()
    }

    override fun findLeagueByThreadAndState(channelId: String, threadId: String, state: League.LeagueState) : League? {
        val league = leagueRepository.findOneBySlackMessageIdAndSlackChannelId(threadId, channelId) ?: return null

        if (league.state != state) {
            return null
        }

        return league
    }

    override fun pauseLeague(name: String) : League? {
        val league = leagueRepository.findOne(name) ?: return null
        if (league.state != League.LeagueState.STARTED) {
            return null
        }
        league.state = League.LeagueState.PAUSED
        return leagueRepository.save(league)
    }

    override fun resumeLeague(name: String) : League? {
        val league = leagueRepository.findOne(name) ?: return null
        if (league.state != League.LeagueState.PAUSED) {
            return null
        }
        league.state = League.LeagueState.STARTED
        return leagueRepository.save(league)
    }

    private fun findPendingLeagueBySlackMessage(message: SlackMessage) : Optional<League> {
        return Optional.ofNullable(findLeagueByThreadAndState(message.channelId, message.timestamp, League.LeagueState.PENDING))
    }

    private fun findPendingLeagueByName(name: String) : Optional<League> {
        return Optional.ofNullable(leagueRepository.findOne(name))
                .filter({ it.state == League.LeagueState.PENDING })
    }

    private fun addPlayer(name: String, playerSlackId: String) {
        if(findPlayer(name, playerSlackId).isPresent) {
            return
        }
        leaguePlayerRepository.save(LeaguePlayer(name, playerSlackId))
    }

    private fun getPlayers(leagueName: String) : Set<LeaguePlayer> {
        return leaguePlayerRepository.findAllByLeagueName(leagueName).collect(Collectors.toSet())
    }

    private fun findPlayer(leagueName: String, slackId: String) : Optional<LeaguePlayer> {
        return leaguePlayerRepository.findAllByLeagueName(leagueName)
                .filter({ it.slackId == slackId })
                .findAny()
    }

    override fun updateMessage(leagueName: String) {
        findPendingLeagueByName(leagueName).ifPresent { updateMessage(it) }
    }

    private fun updateMessage(league: League) {
        slackService.updateMessage(league.startMessage(), composeMessage(league))
    }

    private fun composeMessage(league: League) : String {

        if (league.state == League.LeagueState.PENDING) {
            return leagueMessages.pendingMessage(league.name, getPlayers(league.name), league.competitions, teamService.findExclusions(league.name))
        }

        return leagueMessages.startedMessage(league.name, league.competitions, league.teams)
    }

    private fun requireLeagueNotPresent(name: String) {
        if (leagueRepository.findOne(name) != null) {
            throw IllegalArgumentException(leagueMessages.leagueConflict(name))
        }
    }

    override fun startLeague(name: String) {
        findPendingLeagueByName(name).ifPresent { league ->
            markStarted(league)
            setupTeams(league)
            leagueRepository.save(league)
            slackService.sendChannelMessage(league.slackChannelId, leagueMessages.teams(league.teams))
            updateMessage(league)
        }
    }

    override fun getPendingLeagueNameForThreadId(channelId: String, threadId: String) : Optional<String> {
        return findPendingLeagueBySlackMessage(SlackMessage(threadId, channelId, null)).map { it.name }
    }

    private fun setupTeams(league: League) {
        val players = getPlayers(league.name)
        val teams = mutableSetOf<LeagueTeam>()
        teamSizes(league).forEach({teams.addAll(teamService.composeTeams(league.name, it, players))})
        league.teams = teams
    }

    private fun markStarted(league: League) {
        league.state = League.LeagueState.STARTED
    }


    private fun teamSizes(league: League) : Set<Int> {
        return league.competitions.map({ it.players }).toSet()
    }

    override fun listLeagues(): String {
        return "Leagues:\n" + leagueRepository
                .findAll(Sort(Sort.Direction.DESC, "createdOn"))
                .map { ":${it.state.icon}: `${it.name}`" }
                .joinToString("\n") +

                "\n\nStatuses: \n" + League.LeagueState.values()
                .map { ":${it.icon}: - ${it.name}" }
                .joinToString("\n")

    }

    override fun endLeague(name: String) {
        val league = findStartedLeagueByName(name) ?: return
        league.state = League.LeagueState.FINISHED
        leagueRepository.save(league)
    }

    override fun findAllStarted() : List<League> {
        return leagueRepository.findAll()
                .filter { it.state == League.LeagueState.STARTED }
                .toList()
    }

    override fun findAllPaused(): List<League> {
        return leagueRepository.findAll()
                .filter { it.state == League.LeagueState.PAUSED }
                .toList()    }
}