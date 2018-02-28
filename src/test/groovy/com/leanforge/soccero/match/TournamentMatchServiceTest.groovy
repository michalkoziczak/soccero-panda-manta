package com.leanforge.soccero.match

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.match.domain.TournamentMatch
import com.leanforge.soccero.match.exception.MissingPlayerException
import com.leanforge.soccero.match.exception.WinnersCollisionException
import com.leanforge.soccero.match.repo.MatchResultRepository
import com.leanforge.soccero.match.repo.TournamentMatchRepository
import com.leanforge.soccero.team.domain.LeagueTeam
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant
import java.util.stream.Stream

class TournamentMatchServiceTest extends Specification {

    TournamentMatchRepository tournamentMatchRepository = Mock()
    MatchResultRepository matchResultRepository = Mock()
    SlackService slackService = Mock()


    @Subject
    TournamentMatchService tournamentMatchService = new TournamentMatchService(tournamentMatchRepository, matchResultRepository, slackService)


    def "should create match"() {
        given:
        def league = new League("l1")
        def competition = new Competition("c1", 2)
        def teamA = new LeagueTeam(['p1', 'p2'].toSet())
        def teamB = new LeagueTeam(['p3', 'p4'].toSet())
        def channel = 'a1'
        slackService.getRealNameById(_) >> {it[0]}


        when:
        tournamentMatchService.createMatch(channel, league, competition, teamA, teamB)

        then:
        1 * tournamentMatchRepository.save(_) >> {
            TournamentMatch tm = it[0]
            assert tm.leagueName == 'l1'
            assert tm.competition == competition
            assert tm.competitors == [teamA, teamB].toSet()
            assert tm.slackChannelId == channel
            assert tm.slackMessageId == 'abc'
            assert tm.uuid != null
        }
        1 * slackService.sendChannelMessage(channel, _, 'trophy') >> new SlackMessage('abc', channel, '')
        1 * slackService.sendChannelMessage(channel, _) >> {
            String message = it[1]
            assert message.matches(/startGame p4 `.*`/)
            new SlackMessage('abc', channel, '')
        }
    }

    def "should register result"() {
        given:
        def competition = new Competition("c1", 2)
        def teamA = new LeagueTeam(['p1', 'p2'].toSet())
        def teamB = new LeagueTeam(['p3', 'p4'].toSet())
        def channel = 'a1'
        def message =  new SlackMessage('abc', channel, '')
        def tournament = new TournamentMatch('l1', competition, [teamA, teamB].toSet(), channel, 'abc', UUID.randomUUID())
        tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(message.timestamp, message.channelId) >> tournament
        matchResultRepository.findAllByMatchId(tournament.uuid) >> { Stream.empty() }

        slackService.getRealNameById(_) >> { it[0] }


        when:
        tournamentMatchService.registerResult('p2', message)

        then:
        1 * matchResultRepository.save(_) >> { it[0] }
    }

    def "should throw error on collision"() {
        given:
        def competition = new Competition("c1", 2)
        def teamA = new LeagueTeam(['p1', 'p2'].toSet())
        def teamB = new LeagueTeam(['p3', 'p4'].toSet())
        def channel = 'a1'
        def message =  new SlackMessage('abc', channel, '')
        def tournament = new TournamentMatch('l1', competition, [teamA, teamB].toSet(), channel, 'abc', UUID.randomUUID())
        tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(message.timestamp, message.channelId) >> tournament
        matchResultRepository.findAllByMatchId(tournament.uuid) >> { Stream.of(
                new com.leanforge.soccero.match.domain.MatchResult('l1', competition, teamA, teamB, UUID.randomUUID(), UUID.randomUUID(), Instant.now()),
                new com.leanforge.soccero.match.domain.MatchResult('l1', competition, teamB, teamA, UUID.randomUUID(), UUID.randomUUID(), Instant.now())
        ) }

        slackService.getRealNameById(_) >> { it[0] }


        when:
        tournamentMatchService.registerResult('p2', message)

        then:
        1 * matchResultRepository.save(_) >> { it[0] }
        thrown(WinnersCollisionException)
    }

    def "should throw error on missing player"() {
        given:
        def competition = new Competition("c1", 2)
        def teamA = new LeagueTeam(['p1', 'p2'].toSet())
        def teamB = new LeagueTeam(['p3', 'p4'].toSet())
        def channel = 'a1'
        def message =  new SlackMessage('abc', channel, '')
        def tournament = new TournamentMatch('l1', competition, [teamA, teamB].toSet(), channel, 'abc', UUID.randomUUID())
        tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(message.timestamp, message.channelId) >> tournament

        when:
        tournamentMatchService.registerResult('p5', message)

        then:
        thrown(MissingPlayerException)
    }
}
