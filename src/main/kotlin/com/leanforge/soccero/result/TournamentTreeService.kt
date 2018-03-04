package com.leanforge.soccero.result

import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.result.domain.TournamentTreeNode
import com.leanforge.soccero.tournament.TournamentService
import com.leanforge.soccero.tournament.domain.TournamentState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TournamentTreeService @Autowired constructor(
        private val tournamentService: TournamentService,
        private val tournamentMatchService: TournamentMatchService
) {
    

    fun tournamentTree(league: League, competition: Competition) : List<TournamentTreeNode> {
        val allRounds = tournamentService.allRounds(league, competition, tournamentMatchService.getResults(league.name, competition))
        val allNodes = allRounds.flatMap { toNodes(it) }

        return allNodes + allNodes.flatMap { assignOpponentAndResult(it, allRounds, allNodes) };
    }

    private fun toNodes(tournamentState: TournamentState): List<TournamentTreeNode> {
        return (tournamentState.tournament.winners + tournamentState.tournament.losers)
                .map { TournamentTreeNode(team = it, round = tournamentState.round) }
    }

    private fun assignOpponentAndResult(node: TournamentTreeNode, allRounds: List<TournamentState>, allNodes: List<TournamentTreeNode>): List<TournamentTreeNode> {
        val state = allRounds.single { it.round == node.round }
        val competitors = state.tournament.competitors().firstOrNull { it.contains(node.team) }
        if (competitors == null) {
            node.state = TournamentTreeNode.State.BLOCKED
            return emptyList()
        }
        node.opponent = competitors.minus(node.team).single();
        val result = state.currentRoundResults.firstOrNull {it.hasTeams(competitors)} ?: return emptyList()

        if (result.loser == node.team) {
            node.state = TournamentTreeNode.State.LOST
        }

        if (result.winner != node.team) {
            return emptyList()
        }

        node.state = TournamentTreeNode.State.WON

        val nextRound = allNodes.singleOrNull { it.round == node.round + 1 && it.team == node.team }

        if (nextRound == null) {
            val child = TournamentTreeNode(team = node.team, round = node.round + 1)
            node.child = child.id
            return listOf(child)
        }

        node.child = nextRound.id
        return emptyList()
    }
}