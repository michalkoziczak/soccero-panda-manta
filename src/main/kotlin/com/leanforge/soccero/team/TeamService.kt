package com.leanforge.soccero.team

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.LeaguePlayer
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.team.domain.TeamBuilder
import com.leanforge.soccero.team.domain.TeamExclusion
import com.leanforge.soccero.team.repo.TeamExclusionRepository
import com.ullink.slack.simpleslackapi.SlackSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
open class TeamService @Autowired constructor(private val teamExclusionRepository: TeamExclusionRepository) : TeamServiceInterface {

    override fun composeTeams(leagueName: String, size: Int, players: Set<LeaguePlayer>) : Set<LeagueTeam> {
        return TeamBuilder(size, teamExclusionRepository.findAllByLeagueName(leagueName).toSet(), players.map({ it.slackId }).toSet())
                .randomTeams()
    }

    override fun addExclusion(leagueName: String, slackIds: Set<String>) {
        teamExclusionRepository.save(TeamExclusion(leagueName, slackIds))
    }

    override fun findExclusions(leagueName: String) : List<TeamExclusion> {
        return teamExclusionRepository.findAllByLeagueName(leagueName)
    }
}