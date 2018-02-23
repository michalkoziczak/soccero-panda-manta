package com.leanforge.soccero.team

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.LeaguePlayer
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.team.exception.BadTeamSizeException
import com.leanforge.soccero.team.repo.TeamExclusionRepository
import spock.lang.Specification
import spock.lang.Subject

class TeamServiceTest extends Specification {

    SlackService slackService = Mock()
    TeamExclusionRepository repository = Mock()
    @Subject
    TeamService teamService = new TeamService(repository)


    def "should compose teams"() {
        given:
        def players = [
                new LeaguePlayer('a', 'u1'),
                new LeaguePlayer('a', 'u2'),
                new LeaguePlayer('a', 'u3'),
                new LeaguePlayer('a', 'u4')
        ].toSet()

        when:
        def teams = teamService.composeTeams("", 2, players)

        then:
        1 * repository.findAllByLeagueName(_) >> []
        teams.size() == 2
        teams[0].slackIds.size() == 2
        teams[1].slackIds.size() == 2
        teams[0].slackIds + teams[1].slackIds == players.collect({it.slackId}).toSet()
    }


    def "should refuse to compose teams for bad size"() {
        given:
        def players = [
                new LeaguePlayer('a', 'u1'),
                new LeaguePlayer('a', 'u2'),
                new LeaguePlayer('a', 'u3'),
                new LeaguePlayer('a', 'u4')
        ].toSet()

        when:
        teamService.composeTeams("", 3, players)

        then:
        1 * repository.findAllByLeagueName(_) >> []
        thrown(BadTeamSizeException)
    }
}
