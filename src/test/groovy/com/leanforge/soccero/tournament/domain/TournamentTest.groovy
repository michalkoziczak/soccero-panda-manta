package com.leanforge.soccero.tournament.domain

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.match.MatchResult
import com.leanforge.soccero.team.domain.LeagueTeam
import spock.lang.Specification

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
                        [new LeagueTeam(['p1', 'p2'].toSet()),
                         new LeagueTeam(['p3', 'p4'].toSet())].toSet(),
                        new LeagueTeam(['p1', 'p2'].toSet()),
                        UUID.randomUUID())].toList(), tournament.competitors())
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


        when:
        def ret2 = tournament.currentState([
                new MatchResult("test1", compA, [new LeagueTeam(['p1', 'p2'].toSet()), new LeagueTeam(['p3', 'p4'].toSet())].toSet(),
                        new LeagueTeam(['p1', 'p2'].toSet()),
                        UUID.randomUUID()),
                new MatchResult("test1", compA, [new LeagueTeam(['p5', 'p6'].toSet()), new LeagueTeam(['p7', 'p8'].toSet())].toSet(),
                        new LeagueTeam(['p5', 'p6'].toSet()),
                        UUID.randomUUID()),
                new MatchResult("test1", compA, [new LeagueTeam(['p1', 'p2'].toSet()), new LeagueTeam(['p5', 'p6'].toSet())].toSet(),
                        new LeagueTeam(['p5', 'p6'].toSet()),
                        UUID.randomUUID()),
                new MatchResult("test1", compA, [new LeagueTeam(['p9', 'p10'].toSet()), new LeagueTeam(['p3', 'p4'].toSet())].toSet(),
                        new LeagueTeam(['p9', 'p10'].toSet()),
                        UUID.randomUUID()),
                new MatchResult("test1", compA, [new LeagueTeam(['p5', 'p6'].toSet()), new LeagueTeam(['p9', 'p10'].toSet())].toSet(),
                        new LeagueTeam(['p5', 'p6'].toSet()),
                        UUID.randomUUID()),
                new MatchResult("test1", compA, [new LeagueTeam(['p1', 'p2'].toSet()), new LeagueTeam(['p7', 'p8'].toSet())].toSet(),
                        new LeagueTeam(['p7', 'p8'].toSet()),
                        UUID.randomUUID()),
                new MatchResult("test1", compA, [new LeagueTeam(['p7', 'p8'].toSet()), new LeagueTeam(['p9', 'p10'].toSet())].toSet(),
                        new LeagueTeam(['p7', 'p8'].toSet()),
                        UUID.randomUUID()),
                new MatchResult("test1", compA, [new LeagueTeam(['p5', 'p6'].toSet()), new LeagueTeam(['p7', 'p8'].toSet())].toSet(),
                        new LeagueTeam(['p7', 'p8'].toSet()),
                        UUID.randomUUID()),
                new MatchResult("test1", compA, [new LeagueTeam(['p5', 'p6'].toSet()), new LeagueTeam(['p7', 'p8'].toSet())].toSet(),
                        new LeagueTeam(['p7', 'p8'].toSet()),
                        UUID.randomUUID()),
                ].toList())
        then:
        ret2.losers == [new LeagueTeam(['p7', 'p8'].toSet())]

    }



}
