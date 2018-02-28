package com.leanforge.soccero.tournament

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.match.domain.MatchResult
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.domain.Tournament
import com.leanforge.soccero.tournament.exception.TournamentAlreadyExistsException
import com.leanforge.soccero.tournament.repo.TournamentRepository
import spock.lang.Specification
import spock.lang.Subject

import java.util.stream.Stream

class TournamentServiceTest extends Specification {

    TournamentRepository tournamentRepository = Mock()

    @Subject
    TournamentService tournamentService = new TournamentService(tournamentRepository)

    def "should create all tournaments for league"() {
        given:
        def compA = new Competition("compA", 2)
        def compB = new Competition("compB", 2)
        def compC = new Competition("compC", 2)


        def league = new League(
                name: 'test',
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
        tournamentRepository.save(_ as Iterable<Tournament>) >> {
            Set<Tournament> tournaments = it[0].toSet()
            println tournaments
            assert tournaments.any {it.competition == compA}
            assert tournaments.any {it.competition == compB}
            assert tournaments.any {it.competition == compC}
            assert tournaments.every {it.name == 'test'}
            assert tournaments.every {it.winners.toSet() == league.teams.toSet()}
        }
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

}
