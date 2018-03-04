package com.leanforge.soccero.result

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
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
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

class TournamentMatchServiceTest extends Specification {

    TournamentMatchRepository tournamentMatchRepository = Mock()
    MatchResultRepository matchResultRepository = Mock()
    SlackService slackService = Mock()
    QueueService queueService = Mock()
    TournamentService tournamentService = Mock()


    @Subject
    DefaultTournamentMatchService tournamentMatchService = new DefaultTournamentMatchService(tournamentMatchRepository, matchResultRepository, queueService, tournamentService, slackService)


    def "should create match"() {
        given:
        def channel = 'a1'
        def competition = new Competition("c1", 2)
        def teamA = new LeagueTeam(['p1', 'p2'].toSet())
        def teamB = new LeagueTeam(['p3', 'p4'].toSet())
        def league = new League(name: "l1", slackChannelId: channel, competitions: [competition].toSet())
        slackService.getRealNameById(_) >> {it[0]}
        matchResultRepository.findAllByLeagueNameAndCompetition(_, _) >> Stream.empty()
        tournamentService.pendingCompetitors(_, _, _) >> [[teamA, teamB].toSet()]


        when:
        tournamentMatchService.createMatch(league, competition, teamA, teamB)

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
        1 * queueService.triggerGameScheduler(competition, [teamA, teamB].toSet())
    }

    def "should throw exception for missing opponents"() {
        given:
        def channel = 'a1'
        def competition = new Competition("c1", 2)
        def teamA = new LeagueTeam(['p1', 'p2'].toSet())
        def teamB = new LeagueTeam(['p3', 'p4'].toSet())
        def league = new League(name: "l1", slackChannelId: channel, competitions: [competition].toSet())
        slackService.getRealNameById(_) >> {it[0]}
        matchResultRepository.findAllByLeagueNameAndCompetition(_, _) >> Stream.empty()
        tournamentService.pendingCompetitors(_, _, _) >> []


        when:
        tournamentMatchService.createMatch(league, competition, teamA, teamB)

        then:
        0 * tournamentMatchRepository.save(_)
        0 * slackService.sendChannelMessage(_, _, _)
        0 * queueService.triggerGameScheduler(_, _)
        thrown(AmbiguousPlayerToTeamMappingException)
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
                new com.leanforge.soccero.result.domain.MatchResult('l1', competition, teamA, teamB, UUID.randomUUID(), UUID.randomUUID(), Instant.now()),
                new com.leanforge.soccero.result.domain.MatchResult('l1', competition, teamB, teamA, UUID.randomUUID(), UUID.randomUUID(), Instant.now())
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
        matchResultRepository.findAllByMatchId(_) >> Stream.empty()

        when:
        tournamentMatchService.registerResult('p5', message)

        then:
        thrown(MissingPlayerException)
    }

    def "should throw error on frozen entry"() {
        given:
        def competition = new Competition("c1", 2)
        def teamA = new LeagueTeam(['p1', 'p2'].toSet())
        def teamB = new LeagueTeam(['p3', 'p4'].toSet())
        def channel = 'a1'
        def message =  new SlackMessage('abc', channel, '')
        def match = new TournamentMatch('l1', competition, [teamA, teamB].toSet(), channel, 'abc', UUID.randomUUID())
        tournamentMatchRepository.findOneBySlackMessageIdAndSlackChannelId(message.timestamp, message.channelId) >> match
        matchResultRepository.findAllByMatchId(match.uuid) >> { Stream.of(
                new MatchResult('l1', competition, teamA, teamB, UUID.randomUUID(), UUID.randomUUID(), Instant.now().minus(2, ChronoUnit.HOURS))
        ) }

        slackService.getRealNameById(_) >> { it[0] }


        when:
        tournamentMatchService.registerResult('p2', message)

        then:
        0 * matchResultRepository.save(_)
        thrown(FrozenResultException)
    }
}
