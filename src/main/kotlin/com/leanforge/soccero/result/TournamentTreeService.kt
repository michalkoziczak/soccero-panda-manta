package com.leanforge.soccero.result

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.LeagueService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.result.domain.TournamentTree
import com.leanforge.soccero.result.domain.TournamentTreeNode
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.TournamentService
import com.leanforge.soccero.tournament.domain.TournamentState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class TournamentTreeService @Autowired constructor(
        private val tournamentService: TournamentService,
        private val tournamentMatchService: TournamentMatchService,
        private val slackService: SlackService,
        private val leagueService: LeagueService
) {

    private val listeners : MutableSet<(TournamentTree) -> Boolean> = Collections.newSetFromMap(ConcurrentHashMap())
    private val logger = LoggerFactory.getLogger(javaClass)

    fun allActiveTrees(): List<TournamentTree> {
        val started =  leagueService.findAllStarted()
                .flatMap { league -> league.competitions.map { tournamentTree(league, it) } }
        val paused =  leagueService.findAllPaused()
                .flatMap { league -> league.competitions.map { tournamentTree(league, it) } }

        return started + paused
    }

    fun tournamentTree(league: League, competition: Competition) : TournamentTree {
        val allRounds = tournamentService.allRounds(league, competition, tournamentMatchService.getResults(league.name, competition))
        val allNodes = allRounds.flatMap { toNodes(it) }
                .toMutableList()

       allNodes.toList().onEach { assignOpponentAndResult(it, allRounds, allNodes, allRounds[it.round]) }

        return TournamentTree(league.name, competition.label(), allNodes)
    }

    private fun toNodes(tournamentState: TournamentState): List<TournamentTreeNode> {
        return (tournamentState.tournament.winners + tournamentState.tournament.losers)
                .map { TournamentTreeNode(team = it, round = tournamentState.round, label = label(it, tournamentState.round)) }
    }

    private fun label(leagueTeam: LeagueTeam, round: Int) : String {
        return "#${round + 1} " + leagueTeam.slackIds.joinToString(" & ") { slackService.getRealNameById(it) }
    }

    private fun assignOpponentAndResult(node: TournamentTreeNode, allRounds: List<TournamentState>, allNodes: MutableList<TournamentTreeNode>, tournamentState: TournamentState) {
        val state = allRounds.single { it.round == node.round }
        val competitors = state.tournament.competitors(state.roundDescription).firstOrNull { it.contains(node.team) }
        if (competitors == null) {
            node.state = TournamentTreeNode.State.BLOCKED
            node.child = allNodes.singleOrNull { it.team == node.team && it.round == node.round + 1 }
                    ?.id
            return
        }
        node.opponent = competitors.minus(node.team).single()
        val opponentNode = allNodes.single { it.team == node.opponent && it.round == node.round }
        opponentNode.opponent = node.team
        val result = state.currentRoundResults.firstOrNull {it.hasTeams(competitors)}

        if (result == null && opponentNode.child != null) {
            node.child = opponentNode.child
            return
        }

        if (result == null) {
            val child = TournamentTreeNode(team = null, round = node.round + 1, label = "#${node.round + 2} ?")
            node.child = child.id
            allNodes.add(child)
            return
        }

        var nextRound = allNodes.singleOrNull { it.round == node.round + 1 && it.team == result.winner }

        if (nextRound == null && opponentNode.child != null) {
            nextRound = allNodes.single { it.id == opponentNode.child }
        } else if (nextRound == null) {
            nextRound = TournamentTreeNode(team = result.winner, round = node.round + 1, label = label(result.winner, node.round + 1))
            allNodes.add(nextRound)
        }

        node.child = nextRound.id

        if (result.loser == node.team && tournamentState.tournament.winners.contains(result.loser)) {
            node.state = TournamentTreeNode.State.LOST
        }

        if (result.loser == node.team && tournamentState.tournament.losers.contains(result.loser)) {
            node.state = TournamentTreeNode.State.ELIMINATED
        }

        if (result.winner == node.team) {
            node.state = TournamentTreeNode.State.WON
        }
    }

    fun registerListener(listener: (TournamentTree) -> Boolean) {
        listeners.add(listener)
    }

    fun fireTreeChange(leagueName: String, competition: Competition) {
        val league = leagueService.findStartedLeagueByName(leagueName) ?: return
        val tree = tournamentTree(league, competition)
        val toRemove = mutableSetOf<(TournamentTree) -> Boolean>()
        listeners.onEach {
            try {
                val isConsumed = it(tree)
                if (!isConsumed) {
                    toRemove.add(it);
                }
            } catch (e: Exception) {
                logger.error("Failed to handle change", e)
            }
        }

        listeners.removeAll(toRemove)
    }
}