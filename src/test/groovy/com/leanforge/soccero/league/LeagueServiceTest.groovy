package com.leanforge.soccero.league

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.league.domain.LeaguePlayer
import com.leanforge.soccero.league.repo.LeaguePlayerRepository
import com.leanforge.soccero.league.repo.LeagueRepository
import com.leanforge.soccero.team.TeamService
import com.leanforge.soccero.team.TeamServiceInterface
import spock.lang.Specification
import spock.lang.Subject

import java.util.stream.Stream

class LeagueServiceTest extends Specification {

    LeagueRepository leagueRepository = Mock()
    LeaguePlayerRepository leaguePlayerRepository = Mock()
    SlackService slackService = Mock()
    LeagueMessages leagueMessages = new LeagueMessages(slackService) // Kotlin won't let mock it
    TeamServiceInterface teamService = Mock(TeamServiceInterface)

    @Subject
    LeagueService leagueService = new LeagueService(leagueRepository, leaguePlayerRepository, slackService, leagueMessages, teamService)


    def "should create league"() {
        given:
        def user = 'username-u1'
        def channel = 'channel-1'
        def leagueName = 'test-league'
        leaguePlayerRepository.findAllByLeagueName(leagueName) >> { Stream.empty() }

        when:
        leagueService.createLeague(new SlackMessage('a', channel, user), leagueName, [].toSet())

        then:
        1 * leagueRepository.save(_)
        1 * slackService.sendChannelMessage(channel, _) >> new SlackMessage('abc123', channel, 'me')
        1 * leaguePlayerRepository.save(new LeaguePlayer(leagueName, user))
    }

    def "should not create league if already exists"() {
        given:
        def user = 'username-u1'
        def channel = 'channel-1'
        def leagueName = 'test-league'
        leaguePlayerRepository.findAllByLeagueName(leagueName) >> { Stream.empty() }
        leagueRepository.findOne(leagueName) >> new League(leagueName)

        when:
        leagueService.createLeague(new SlackMessage('a', channel, user), leagueName, [].toSet())

        then:
        0 * leagueRepository.save(_)
        0 * leaguePlayerRepository.save(_)
        thrown(IllegalArgumentException)
    }

    def "should add player"() {
        given:
        def user = 'username-u1'
        def channel = 'channel-1'
        def leagueName = 'test-league'
        leaguePlayerRepository.findAllByLeagueName(leagueName) >>> [Stream.empty(), Stream.of(new LeaguePlayer(leagueName, user))]
        leagueRepository.findOne(leagueName) >> new League(leagueName)
        leagueRepository.findOneBySlackMessageIdAndSlackChannelId('a', channel) >> new League(leagueName)
        teamService.findExclusions(_) >> []

        when:
        leagueService.addPlayerAndUpdateStatusMessage(new SlackMessage('a', channel, user), user)

        then:
        1 * slackService.updateMessage(_, _)
        1 * leaguePlayerRepository.save(new LeaguePlayer(leagueName, user))
    }
}
