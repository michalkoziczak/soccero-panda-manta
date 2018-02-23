package com.leanforge.soccero.league

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.LeaguePlayer
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.team.domain.TeamExclusion
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
open class LeagueMessages @Autowired constructor(val slackService: SlackService) {

    fun pendingMessage(
            name: String,
            players: Set<LeaguePlayer>,
            competitions: Set<Competition>,
            excludes: List<TeamExclusion>) : String {
        return "Join the league `$name`. Click :heavy_plus_sign:\n\n" +
                "Competitions:\n${competitionToMessage(competitions)}\n\n" +
                "Players (${players.size}):\n${playersToMessage(players, excludes)}"
    }

    fun startedMessage(
            name: String,
            competitions: Set<Competition>,
            teams: Set<LeagueTeam>) : String {
        return "League `$name` started!\n\n" +
                "Competitions:\n${competitionToMessage(competitions)}\n\n" +
                teams(teams)
    }

    private fun playersToMessage(players: Set<LeaguePlayer>, excludes: List<TeamExclusion>) : String {
        val excludeArray = excludes.toTypedArray()

        return players.sortedBy { slackService.getUsername(it.slackId) }
                .map { "> :joystick: <@${it.slackId}> ${groups(it.slackId, excludeArray)}" }
                .joinToString("\n")
    }

    private fun groups(slackId: String, excludes: Array<TeamExclusion>): String {
        return excludes.withIndex()
                .filter { it.value.slackIds.contains(slackId) }
                .map { "(`${it.index}`)" }
                .joinToString(" ")
    }

    private fun competitionToMessage(players: Set<Competition>) : String {
        return players
                .map { "> :trophy: ${it.name} ${it.players}vs${it.players}" }
                .joinToString("\n")
    }

    fun leagueConflict(name: String): String {
        return "League `$name` already exist. Choose other name."
    }

    fun teams(teams: Set<LeagueTeam>): String {
        val sortedTeams = teams
                .sortedWith(
                        compareBy(
                                { it.slackIds.size },
                                {it.slackIds.map({ slackService.getRealNameById(it) }).sorted().joinToString()}
                        )
                )

        val teamsMsg = sortedTeams.filter({ it.slackIds.size > 1 }).map { "> ${it.slackIds.size}vs${it.slackIds.size}: " + sortIdsByRealName(it.slackIds).map { "<@$it>" }.joinToString(" - ") }
                .joinToString("\n")

        return "Teams: \n$teamsMsg"
    }

    private fun sortIdsByRealName(ids : Collection<String>) : List<String> {
        return ids.sortedBy { slackService.getUsername(it) }
    }
}