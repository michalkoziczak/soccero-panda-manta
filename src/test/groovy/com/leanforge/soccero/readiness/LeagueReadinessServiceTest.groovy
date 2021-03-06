package com.leanforge.soccero.readiness

import com.leanforge.game.slack.SlackMessage
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.result.TournamentMatchService
import com.leanforge.soccero.readiness.domain.LeagueStatusMessage
import com.leanforge.soccero.readiness.repo.LeagueStatusMessageRepository
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.TournamentService
import com.leanforge.soccero.tournament.domain.Tournament
import com.leanforge.soccero.tournament.domain.TournamentState
import com.leanforge.soccero.updater.UpdateMarkService
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class LeagueReadinessServiceTest extends Specification {


    ReadinessService readinessService = Mock()
    LeagueStatusMessageRepository leagueStatusMessageRepository = Mock()
    TournamentService tournamentService = Mock()
    TournamentMatchService tournamentMatchService = Mock()
    LeagueService leagueService = Mock()
    SlackService slackService = Mock()
    UpdateMarkService updateMarkService = Mock()

    LeagueReadinessService leagueReadinessService = new LeagueReadinessService(
            readinessService,
            leagueStatusMessageRepository,
            tournamentService,
            tournamentMatchService,
            leagueService,
            slackService,
            updateMarkService,
            LocalTime.MIN
    )

    LeagueReadinessService beforeResetLeagueReadinessService = new LeagueReadinessService(
            readinessService,
            leagueStatusMessageRepository,
            tournamentService,
            tournamentMatchService,
            leagueService,
            slackService,
            updateMarkService,
            LocalTime.MAX
    )

    def league = new League(name: 'l1',
            slackChannelId: 'ch0',
            state: League.LeagueState.STARTED,
            competitions: [
                    new Competition('a', 2),
                    new Competition('b', 2),
                    new Competition('c', 2)
            ]
    )

    def "should send first status message"() {
        given:
        def tournament = new Tournament('', new Competition('a', 2), [], [], UUID.randomUUID())
        leagueService.findAllStarted() >> [league]
        tournamentService.currentState(league, _, _) >> new TournamentState(0, tournament, [], [], [])
        tournamentService.pendingCompetitors(league, _, _) >> []
        leagueStatusMessageRepository.findAll() >> []

        def msg1 = new SlackMessage('t1', 'ch0', null)
        def msg2 = new SlackMessage('t2', 'ch0', null)
        def msg3 = new SlackMessage('t3', 'ch0', null)

        when:
        leagueReadinessService.resendStatusMessageIfNeeded()

        then:
        3 * slackService.sendChannelMessage('ch0', _) >>> [msg1, msg2, msg3]
        1 * readinessService.sendGenericReadinessMessage('ch0')
        3 * leagueStatusMessageRepository.save(_) >> {
            LeagueStatusMessage msg = it[0]
            assert msg.slackChannelId == 'ch0'
            assert ['t1', 't2', 't3'].contains(msg.slackMessageId)
        }
        1 * readinessService.markEveryoneBusy()
    }

    def "should resend status message"() {
        given:
        def tournament = new Tournament('', new Competition('a', 2), [], [], UUID.randomUUID())
        leagueService.findAllStarted() >> [league]
        tournamentService.currentState(league, _, _) >> new TournamentState(0, tournament, [], [], [])
        tournamentService.pendingCompetitors(league, _, _) >> []
        leagueStatusMessageRepository.findAll() >> []

        def msg1 = new SlackMessage('t1', 'ch0', null)
        def msg2 = new SlackMessage('t2', 'ch0', null)
        def msg3 = new SlackMessage('t3', 'ch0', null)

        leagueStatusMessageRepository.findTopByOrderByCreationDateDesc() >> new LeagueStatusMessage('t1', 'ch0', 'l1', league.competitions.first(), Instant.now().minus(25, ChronoUnit.HOURS), UUID.randomUUID())

        when:
        leagueReadinessService.resendStatusMessageIfNeeded()

        then:
        3 * slackService.sendChannelMessage('ch0', _) >>> [msg1, msg2, msg3]
        1 * readinessService.sendGenericReadinessMessage('ch0')
        3 * leagueStatusMessageRepository.save(_) >> {
            LeagueStatusMessage msg = it[0]
            assert msg.slackChannelId == 'ch0'
            assert ['t1', 't2', 't3'].contains(msg.slackMessageId)
        }
        1 * readinessService.markEveryoneBusy()
    }

    def "should not resend status message if before reset time"() {
        when:
        beforeResetLeagueReadinessService.resendStatusMessageIfNeeded()

        then:
        0 * slackService.sendChannelMessage(_, _)
        0 * readinessService.sendGenericReadinessMessage(_)
        0 * leagueStatusMessageRepository.save(_)
        0 * readinessService.markEveryoneBusy()
    }

    @Ignore("Fails on specific time")
    def "should not resend status message message already sent"() {
        given:
        leagueService.findAllStarted() >> [league]
        tournamentService.currentState(league, _, _) >> new Tournament('', new Competition('a', 2), [], [], UUID.randomUUID())
        tournamentService.pendingCompetitors(league, _, _) >> [
        ]
        leagueStatusMessageRepository.findTopByOrderByCreationDateDesc() >> new LeagueStatusMessage('t1', 'ch0', 'l1', league.competitions.first(), Instant.now().minus(2, ChronoUnit.HOURS), UUID.randomUUID())

        when:
        leagueReadinessService.resendStatusMessageIfNeeded()

        then:
        0 * slackService.sendChannelMessage(_, _)
        0 * readinessService.sendGenericReadinessMessage(_)
        0 * leagueStatusMessageRepository.save(_)
        0 * readinessService.markEveryoneBusy()
    }

    def "should schedule game if all players are ready"() {
        given:
        leagueService.findAllStarted() >> [league]
        def tournament = new Tournament('', new Competition('a', 2), [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())], [], UUID.randomUUID())
        tournamentService.currentState(league, _, _) >> new TournamentState(0, tournament, [], [], [
                [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())].toSet()
        ])

        readinessService.readyPlayers() >> ['a','b','c','d'].toSet()

        when:
        leagueReadinessService.scheduleReadyGames()

        then:
        1 * readinessService.markBusy('a')
        1 * readinessService.markBusy('b')
        1 * readinessService.markBusy('c')
        1 * readinessService.markBusy('d')
        1 * tournamentMatchService.createMatch(league, _, _, _) >> {
            def teamA = it[2]
            def teamB = it[3]
            assert [teamA, teamB].toSet() == [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())].toSet()
        }
    }

    def "should prefer game in lower round"() {
        given:
        leagueService.findAllStarted() >> [league]
        def tournamentA = new Tournament('', new Competition('a', 2), [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())], [], UUID.randomUUID())
        def tournamentB = new Tournament('', new Competition('b', 2), [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())], [], UUID.randomUUID())
        def tournamentC = new Tournament('', new Competition('c', 2), [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())], [], UUID.randomUUID())
        tournamentService.currentState(league, tournamentA.competition, _) >> new TournamentState(3, tournamentA, [], [], [
                [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())].toSet()
        ])

        tournamentService.currentState(league, tournamentB.competition, _) >> new TournamentState(1, tournamentB, [], [], [
                [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())].toSet()
        ])

        tournamentService.currentState(league, tournamentC.competition, _) >> new TournamentState(2, tournamentC, [], [], [
                [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())].toSet()
        ])

        readinessService.readyPlayers() >> ['a','b','c','d'].toSet()

        when:
        leagueReadinessService.scheduleReadyGames()

        then:
        1 * tournamentMatchService.createMatch(league, _, _, _) >> {
            Competition comp = it[1]
            def teamA = it[2]
            def teamB = it[3]
            assert [teamA, teamB].toSet() == [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())].toSet()
            assert comp.name == 'b'
        }
    }

    def "should not schedule game if any of players is busy"() {
        given:
        leagueService.findAllStarted() >> [league]
        def tournament = new Tournament('', new Competition('a', 2), [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())], [], UUID.randomUUID())
        tournamentService.currentState(league, _, _) >> new TournamentState(0, tournament, [], [], [
                [new LeagueTeam(['a', 'b'].toSet()), new LeagueTeam(['c', 'd'].toSet())].toSet()
        ])

        readinessService.readyPlayers() >> ['a','b','d'].toSet()

        when:
        leagueReadinessService.scheduleReadyGames()

        then:
        0 * readinessService.markBusy(_)
        0 * tournamentMatchService.createMatch(_, _, _, _)
    }
}
