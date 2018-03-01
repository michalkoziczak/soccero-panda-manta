package com.leanforge.soccero.tournament

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.match.domain.MatchResult
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.domain.Tournament
import com.leanforge.soccero.tournament.exception.TournamentAlreadyExistsException
import com.leanforge.soccero.tournament.repo.TournamentRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant
import java.util.stream.Stream

class TournamentServiceTest extends Specification {

    TournamentRepository tournamentRepository = Mock()
    SlackService slackService = Mock()

    @Subject
    DefaultTournamentService tournamentService = new DefaultTournamentService(tournamentRepository, slackService)

    def "should create all tournaments for league"() {
        given:
        def compA = new Competition("compA", 2)
        def compB = new Competition("compB", 2)
        def compC = new Competition("compC", 2)


        def league = new League(
                name: 'test',
                slackChannelId: 'ch0',
                competitions: [compA, compB, compC].toSet(),
                teams: [
                        new LeagueTeam(['p1', 'p2'].toSet()),
                        new LeagueTeam(['p3', 'p4'].toSet()),
                        new LeagueTeam(['p5', 'p6'].toSet()),
                        new LeagueTeam(['p7', 'p8'].toSet()),
                        new LeagueTeam(['p9', 'p10'].toSet()),
                        new LeagueTeam(['p11', 'p12'].toSet()),
                        new LeagueTeam(['p13', 'p14'].toSet()),
                        new LeagueTeam(['p15', 'p16'].toSet()),
                        new LeagueTeam(['p17', 'p18'].toSet()),
                ].toSet()
        )

        tournamentRepository.findAllByName(_) >> Stream.empty()

        when:
        tournamentService.createTournaments(league)

        then:
        1 * tournamentRepository.save(_ as Iterable<Tournament>) >> {
            Set<Tournament> tournaments = it[0].toSet()
            println tournaments
            assert tournaments.any {it.competition == compA}
            assert tournaments.any {it.competition == compB}
            assert tournaments.any {it.competition == compC}
            assert tournaments.every {it.name == 'test'}
            assert tournaments.every {it.winners.toSet() == league.teams.toSet()}
            assert tournaments.every {it.winners.size() == league.teams.size()}
        }
        3 * slackService.sendChannelMessage('ch0', _)
    }


    def "should prevent duplicates"() {
        given:
        def compA = new Competition("compA", 2)

        def league = new League(
                name: 'test',
                competitions: [compA].toSet(),
                teams: [
                        new LeagueTeam(['p1', 'p2'].toSet()),
                        new LeagueTeam(['p3', 'p4'].toSet())
                ].toSet()
        )

        tournamentRepository.findAllByName(_) >> { Stream.of(new Tournament('test', compA, [], [], UUID.randomUUID())) }

        when:
        tournamentService.createTournaments(league)

        then:
        0 * tournamentRepository.save(_ as Iterable<Tournament>)
        thrown(TournamentAlreadyExistsException)
    }

    def "should handle variable team size"() {
        given:
        def compA = new Competition("compA", 2)
        def compB = new Competition("compA", 3)


        def league = new League(
                name: 'test',
                competitions: [compA, compB].toSet(),
                teams: [
                        new LeagueTeam(['p1', 'p2'].toSet()),
                        new LeagueTeam(['p3', 'p4'].toSet()),
                        new LeagueTeam(['p5', 'p6'].toSet()),
                        new LeagueTeam(['p1', 'p2', 'p3'].toSet()),
                        new LeagueTeam(['p4', 'p5', 'p6'].toSet())
                ].toSet()
        )

        def team2 = league.teams.findAll {team -> team.slackIds.size() == 2}
        def team3 = league.teams.findAll {team -> team.slackIds.size() == 3}

        tournamentRepository.findAllByName(_) >> Stream.empty()

        when:
        tournamentService.createTournaments(league)

        then:
        tournamentRepository.save(_ as Iterable<Tournament>) >> {
            Set<Tournament> tournaments = it[0].toSet()
            assert tournaments.any {it.competition == compA && it.winners.toSet() == team2.toSet()}
            assert tournaments.any {it.competition == compB && it.winners.toSet() == team3.toSet()}

        }
    }

    def "should calculate changing state"() {
        given:
        def compA = new Competition("compA", 2)

        def teams = [
                new LeagueTeam(['p1', 'p2'].toSet()),
                new LeagueTeam(['p3', 'p4'].toSet()),
                new LeagueTeam(['p5', 'p6'].toSet()),
                new LeagueTeam(['p7', 'p8'].toSet()),
                new LeagueTeam(['p9', 'p10'].toSet()),
        ]

        def league = new League(
                name: 'test',
                competitions: [compA].toSet(),
                teams: teams.toSet()
        )

        tournamentRepository.findOneByNameAndCompetition('test', compA) >> new Tournament('test', compA, teams, [], UUID.randomUUID())

        def initialResults = []

        def endOfRound1 = initialResults + [
                create(teams[0], teams[1]),
                create(teams[3], teams[2]),
        ]

        def middleOfRound2 = endOfRound1 + [
                create(teams[3], teams[0]),
        ]

        def endOfRound2 = middleOfRound2 + [
                create(teams[1], teams[4]),
        ]

        when:
        def round1 = tournamentService.currentState(league, compA, initialResults)
        def round1Competitors = tournamentService.pendingCompetitors(league, compA, initialResults)
        def round2 = tournamentService.currentState(league, compA, endOfRound1)
        def round2Competitors = tournamentService.pendingCompetitors(league, compA, endOfRound1)
        def middleRound2 = tournamentService.currentState(league, compA, middleOfRound2)
        def middleRound2Competitors = tournamentService.pendingCompetitors(league, compA, middleOfRound2)
        def round3 = tournamentService.currentState(league, compA, endOfRound2)
        def round3Competitors = tournamentService.pendingCompetitors(league, compA, endOfRound2)

        then:
        // all competitors in the round (pending + done)
        round1.competitors() == [
                [teams[0], teams[1]].toSet(),
                [teams[2], teams[3]].toSet()
        ]
        round2.competitors() == [
                [teams[0], teams[3]].toSet(),
                [teams[4], teams[1]].toSet()
        ]
        middleRound2.competitors() == [
                [teams[0], teams[3]].toSet(),
                [teams[4], teams[1]].toSet()
        ]
        round3.competitors() == [
                [teams[1], teams[3]].toSet(),
                [teams[0], teams[2]].toSet()
        ]

        // pending competitors in current round
        round1Competitors == [
                [teams[0], teams[1]].toSet(),
                [teams[2], teams[3]].toSet()
        ]
        round2Competitors == [
                [teams[0], teams[3]].toSet(),
                [teams[4], teams[1]].toSet()
        ]
        middleRound2Competitors == [
                [teams[4], teams[1]].toSet()
        ]
        round3Competitors == [
                [teams[1], teams[3]].toSet(),
                [teams[0], teams[2]].toSet()
        ]
    }

    MatchResult create(def winner, def loser) {
        new MatchResult('test', new Competition("compA", 2), loser, winner, UUID.randomUUID(), UUID.randomUUID(), Instant.now())
    }
}
