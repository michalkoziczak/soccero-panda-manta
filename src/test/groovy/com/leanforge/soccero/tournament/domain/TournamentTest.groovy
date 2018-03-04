package com.leanforge.soccero.tournament.domain

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.result.domain.MatchResult
import com.leanforge.soccero.team.domain.LeagueTeam
import spock.lang.Specification

import java.time.Instant

class TournamentTest extends Specification {

    def "should calculate next round correctly"() {
        given:
        def compA = new Competition("compA", 2)

       def tournament = new Tournament(
                "test",
                compA,
                [
                        new LeagueTeam(['p1', 'p2'].toSet()),
                        new LeagueTeam(['p3', 'p4'].toSet()),
                        new LeagueTeam(['p5', 'p6'].toSet()),
                        new LeagueTeam(['p7', 'p8'].toSet()),
                        new LeagueTeam(['p9', 'p10'].toSet()),
                        new LeagueTeam(['p11', 'p12'].toSet()),
                        new LeagueTeam(['p13', 'p14'].toSet()),
                        new LeagueTeam(['p15', 'p16'].toSet()),
                        new LeagueTeam(['p17', 'p18'].toSet()),
                ].toList(),
                [].toList(),
                UUID.randomUUID())


        when:
        def ret2 = tournament.nextRound([
                new MatchResult("test1", compA,
                        new LeagueTeam(['p3', 'p4'].toSet()),
                        new LeagueTeam(['p1', 'p2'].toSet()),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now())].toList(), tournament.competitors())
        then:
        ret2.losers == [new LeagueTeam(['p3', 'p4'].toSet())]
        ret2.competitors()[0] ==
                [ new LeagueTeam(['p1', 'p2'].toSet()),
                  new LeagueTeam(['p5', 'p6'].toSet())].toSet()
    }
/**
 * (1,2)--- (1,2)
 * (3,4)
 * (5,6)--- (5,6)
 * (7,8)
 * (9,10)
 */
    def "should calculate semi finals correctly"() {
        given:
        def compA = new Competition("compA", 2)

        def tournament = new Tournament(
                "test",
                compA,
                [
                        new LeagueTeam(['p1', 'p2'].toSet()),
                        new LeagueTeam(['p3', 'p4'].toSet()),
                        new LeagueTeam(['p5', 'p6'].toSet()),
                        new LeagueTeam(['p7', 'p8'].toSet()),
                        new LeagueTeam(['p9', 'p10'].toSet()),
                ].toList(),
                [].toList(),
                UUID.randomUUID())

        def results = [
                // result -> Winner vs Loser
                result(['p1', 'p2'], ['p3', 'p4']),
                result(['p5', 'p6'], ['p7', 'p8']),
                result(['p5', 'p6'], ['p1', 'p2']),
                result(['p9', 'p10'], ['p3', 'p4']),
                result(['p5', 'p6'], ['p9', 'p10']),
                result(['p7', 'p8'], ['p1', 'p2']),
                result(['p7', 'p8'], ['p9', 'p10']),
                result(['p7', 'p8'], ['p5', 'p6']),
                result(['p7', 'p8'], ['p5', 'p6'])
        ]


        when:
        def ret2 = tournament.currentState(results)
        then:
        ret2.losers == [new LeagueTeam(['p7', 'p8'].toSet())]

    }



    MatchResult result(List<String> winners, List<String> losers) {
        new MatchResult("test1", new Competition("compA", 2), new LeagueTeam(losers.toSet()),
                new LeagueTeam(winners.toSet()),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now())
    }
}
