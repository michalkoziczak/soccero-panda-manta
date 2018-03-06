package com.leanforge.soccero.result

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.result.domain.MatchResult
import com.leanforge.soccero.result.domain.TournamentTreeNode
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.TournamentService
import com.leanforge.soccero.tournament.domain.Tournament
import com.leanforge.soccero.tournament.domain.TournamentState
import spock.lang.Specification

import java.time.Instant

class TournamentTreeServiceTest extends Specification {

    TournamentService tournamentService = Mock()
    TournamentMatchService tournamentMatchService = Mock()
    SlackService slackService = Mock()
    LeagueService leagueService = Mock()

    TournamentTreeService tournamentTreeService = new TournamentTreeService(tournamentService, tournamentMatchService, slackService, leagueService)

    def "should calculate current tree"() {
        given:
        slackService.getRealNameById(_) >> "real"
        def league = new League('l1')
        def competition = new Competition('c1', 1)
        def teams = [
                team('p0'), team('p1'), team('p2'), team('p3'), team('p4')
        ]

        def round0 = new TournamentState(
                0,
                new Tournament(league.name, competition, teams, [], UUID.randomUUID()),
                [], [
                        result(teams[0], teams[1]),
                        result(teams[3], teams[2])
                ], []
        )
        def round1 = new TournamentState(
                1,
                new Tournament(league.name, competition, [teams[0], teams[3], teams[4]], [teams[1], teams[2]], UUID.randomUUID()),
                [], [
                result(teams[0], teams[3]),
                result(teams[4], teams[1])
        ], []
        )

        def round2 = new TournamentState(
                2,
                new Tournament(league.name, competition, [teams[0], teams[4]], [teams[1], teams[2], teams[3]], UUID.randomUUID()),
                [], [
                result(teams[0], teams[4])
        ], [
                [teams[1], teams[2]].toSet()
        ]
        )

        tournamentService.allRounds(league, competition, _) >> [round0, round1, round2]

        when:
        def tree = tournamentTreeService.tournamentTree(league, competition).nodes

        then:
        tree.any {
            it.team == team('p0') &&
                    findById(findById(findById(it.child, tree).child, tree).child, tree).team == team('p0')
        }
        tree.any {
            it.team == team('p0') &&
                    it.round == 0 &&
                    it.state == TournamentTreeNode.State.WON &&
                    findById(it.child, tree).team == team('p0') &&
                    findById(it.child, tree).round == 1
        }
        tree.any {
            it.team == team('p4') &&
                    it.round == 1 &&
                    it.state == TournamentTreeNode.State.WON &&
                    findById(it.child, tree).team == team('p4') &&
                    findById(it.child, tree).round == 2
        }
        tree.any {
            it.team == team('p4') &&
                    it.round == 2 &&
                    it.state == TournamentTreeNode.State.LOST
        }
        tree.any {
            it.team == team('p4') &&
                    it.round == 0 &&
                    it.state == TournamentTreeNode.State.BLOCKED
        }
        tree.any {
            it.team == team('p1') &&
                    it.round == 2 &&
                    it.state == TournamentTreeNode.State.PENDING
        }
        tree.any {
            it.team == team('p2') &&
                    it.round == 2 &&
                    it.state == TournamentTreeNode.State.PENDING
        }
        tree.any {
            it.team == team('p0') &&
                    it.round == 3 &&
                    it.state == TournamentTreeNode.State.PENDING
        }
    }

    def "should calculate first round current tree"() {
        given:
        slackService.getRealNameById(_) >> "real"
        def league = new League('l1')
        def competition = new Competition('c1', 1)
        def teams = [
                team('p0'), team('p1'), team('p2'), team('p3'), team('p4')
        ]

        def round0 = new TournamentState(
                0,
                new Tournament(league.name, competition, teams, [], UUID.randomUUID()),
                [], [], []
        )

        tournamentService.allRounds(league, competition, _) >> [round0]

        when:
        def tree = tournamentTreeService.tournamentTree(league, competition).nodes

        then:
        tree.any {
            it.team == team('p0') &&
                    it.round == 0 &&
                    it.state == TournamentTreeNode.State.PENDING &&
                    findById(it.child, tree).round == 1
        }
    }

    private LeagueTeam team(String id) {
        new LeagueTeam([id].toSet())
    }

    private MatchResult result(LeagueTeam winner, LeagueTeam loser) {
        new MatchResult('l1', new Competition('c1', 1), loser, winner, UUID.randomUUID(), UUID.randomUUID(), Instant.now())
    }

    private TournamentTreeNode findById(String id, List<TournamentTreeNode> nodes) {
        nodes.find { it.id == id }
    }
}
