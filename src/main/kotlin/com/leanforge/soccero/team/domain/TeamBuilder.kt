package com.leanforge.soccero.team.domain

import com.leanforge.soccero.team.exception.BadTeamSizeException
import com.leanforge.soccero.team.exception.TeamConstraintException
import org.slf4j.LoggerFactory

class TeamBuilder(val size: Int, val teamExclusions: Set<TeamExclusion>, val players: Set<String>) {

    val logger = LoggerFactory.getLogger(javaClass)


    fun randomTeams() : Set<LeagueTeam> {
        if (size == 1) {
            return players.map { LeagueTeam(setOf(it)) }
                    .toSet()
        }
        logger.debug("Calculating random teams of size {} for {}", size, players)
        val all = generateAllPossibleTeams()
        logger.debug("All possible teams: {}", all)
        return findSolution(all)
                .map { LeagueTeam(it) }
                .toSet()
    }

    protected fun generateAllPossibleTeams() : Set<Set<String>> {
        val combinations : MutableSet<Set<String>> = mutableSetOf()
        val playerList = players.toList()
        val i = Array(size, {0})

        while (!i.all { it >= players.size - 1 }) {
            increment(i, players.size)
            val team = mutableSetOf<String>()
            i.forEach { team.add(playerList[it]) }
            if (team.size == size) {
                combinations.add(team)
            }
        }

        return filterExcluded(combinations)
    }

    private fun increment(array: Array<Int>, max: Int) {
        for ((i) in array.withIndex()) {
            if (++array[i] >= max) {
                array[i] = 0;
            } else {
                return
            }
        }
    }

    private fun filterExcluded(combinations: Set<Set<String>>) : Set<Set<String>> {
        return combinations.filter { !isForbidden(it) }
                .toSet()
    }

    private fun isForbidden(team: Set<String>) : Boolean {
        return teamExclusions.any { it.isForbidden(team) }
    }

    private fun findSolution(teams: Set<Set<String>>) : Set<Set<String>> {
        if (players.size % size != 0) {
            throw BadTeamSizeException("Can't form teams of $size out of ${players.size}")
        }

        val numberOfTeams = players.size/size
        val teamList = teams.shuffled()
        val playerList = players.shuffled()

        return TeamCombinationIterator(numberOfTeams, teamList, playerList).next() ?: throw TeamConstraintException()
    }

    private class TeamCombinationIterator(numberOfTeams: Int, val teams: List<Set<String>>, val players: List<String>) {
        val skipList : Array<Int> = Array(numberOfTeams, {0})

        fun next() : Set<Set<String>>? {
            var next : Set<Set<String>>? = null
            val max = Array(skipList.size, {Int.MAX_VALUE})

            while (next == null && hasNext(max)) {
                next = tryToGetNext(max)
                inc(max)
            }

            return next
        }

        private fun hasNext(max : Array<Int>) : Boolean {
            for(i in 0..(skipList.size - 1)) {
                if (skipList[i] < max[i]) {
                    return true
                }
            }

            return false
        }

        private fun inc(max : Array<Int>) {
            for((i, maxValue) in max.withIndex().reversed()) {
                if (skipList[i] < maxValue) {
                    skipList[i]++
                    return
                } else {
                    skipList[i] = 0
                }
            }
        }

        private fun tryToGetNext(max : Array<Int>) : Set<Set<String>>? {
            val toHandle = players.toMutableList()
            val handled = mutableSetOf<String>()
            val ret = mutableSetOf<Set<String>>()

            for(i in 0..(skipList.size - 1)) {
                val player = toHandle.first()
                val possibleTeams = teamsForPlayer(handled, player)
                max[i] = possibleTeams.size - 1
                if (skipList[i] > max[i]) {
                    return null
                }
                val teamForPlayer = possibleTeams[skipList[i]]
                handled.addAll(teamForPlayer)
                toHandle.removeAll(teamForPlayer)
                ret.add(teamForPlayer)
            }

            return ret;
        }

        private fun teamsForPlayer(skipPlayers: Set<String>, player: String) : List<Set<String>> {
            return teams.filter { team -> skipPlayers.all { !team.contains(it) } }
                    .filter { it.contains(player) }
                    .toList()
        }
    }
}