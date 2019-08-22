package com.leanforge.soccero.league

import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.league.domain.LeaguePlayer
import com.leanforge.soccero.league.repo.LeaguePlayerRepository
import com.leanforge.soccero.league.repo.LeagueRepository
import com.leanforge.soccero.result.repo.MatchResultRepository
import com.leanforge.soccero.result.repo.TournamentMatchRepository
import com.leanforge.soccero.round.repo.RoundRepository
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.repo.TournamentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


interface LeaguePlayerReplacerService {
    fun replacePlayer(leagueName: String, playerIdToRemove: String, playerIdToAdd: String) : Boolean
}

@Service
class DefaultLeaguePlayerReplacerService @Autowired constructor(
        private val leagueRepository: LeagueRepository,
        private val leaguePlayerRepository: LeaguePlayerRepository,
        private val matchResultRepository: MatchResultRepository,
        private val roundRepository: RoundRepository,
        private val tournamentRepository: TournamentRepository,
        private val tournamentMatchRepository: TournamentMatchRepository
        ) : LeaguePlayerReplacerService {

    override fun replacePlayer(leagueName: String, playerIdToRemove: String, playerIdToAdd: String): Boolean {
        val league = leagueRepository.findOne(leagueName) ?: return false
        val oldLeagueTeam = getOldLeagueTeam(league, playerIdToRemove) ?: return false
        val newLeagueTeam = getNewLeagueTeam(oldLeagueTeam, playerIdToRemove, playerIdToAdd)

        if (!replaceLeaguePlayers(leagueName, playerIdToRemove, playerIdToAdd)) {
            return false
        }

        replaceTeamInLeague(league, oldLeagueTeam, newLeagueTeam)
        replaceInMatchResults(league, oldLeagueTeam, newLeagueTeam)
        replaceInRounds(league, oldLeagueTeam, newLeagueTeam)
        replaceInTournaments(league, oldLeagueTeam, newLeagueTeam)
        replaceInTournamentMatches(league, oldLeagueTeam, newLeagueTeam)


        return true
    }

    private fun getOldLeagueTeam(league: League, playerIdToRemove: String) : LeagueTeam? {
        return league.teams.find { it.slackIds.contains(playerIdToRemove) }
    }

    private fun getNewLeagueTeam(oldTeam: LeagueTeam, playerIdToRemove: String, playerIdToAdd: String) : LeagueTeam {
        var restPlayers = oldTeam.slackIds.filter { it != playerIdToRemove }.toMutableSet()
        restPlayers.add(playerIdToAdd)

        return oldTeam.copy(slackIds = restPlayers.toSet())
    }

    private fun replaceTeamInLeague(league: League, oldTeam: LeagueTeam, newTeam: LeagueTeam) {

        league.teams = league.teams.map { if (it == oldTeam) newTeam else it }.toSet()

        leagueRepository.save(league)
    }

    private fun replaceLeaguePlayers(leagueName: String, playerIdToRemove: String, playerIdToAdd: String) : Boolean {
        val leaguePlayers = leaguePlayerRepository.findAllByLeagueName(leagueName)

        // league players
        val leaguePlayerToRemove =  leaguePlayers.filter { it.slackId == playerIdToRemove }.findFirst().orElseGet { null } ?: return false
        val leaguePlayerToAdd = LeaguePlayer(leagueName, playerIdToAdd)

        leaguePlayerRepository.delete(leaguePlayerToRemove)
        leaguePlayerRepository.save(leaguePlayerToAdd)
        return true
    }

    private fun replaceInMatchResults(league: League, oldTeam: LeagueTeam, newTeam: LeagueTeam) {
        val matchResults = matchResultRepository.findAllByLeagueName(league.name)

        matchResults
                .filter { it.hasTeam(oldTeam) }
                .map {
                    if(it.winner == oldTeam) it.copy(winner = newTeam) else it.copy(loser = newTeam)
                }.forEach {
                    matchResultRepository.save(it)
                }
    }

    private fun replaceInRounds(league: League, oldTeam: LeagueTeam, newTeam: LeagueTeam) {
        val rounds = roundRepository.findAllByLeague(league.name)

        rounds.filter { it.isLeagueTeamPlaying(oldTeam) }
                .map { round ->
                    val newPairs = round.pairs.map { pair ->
                        when (oldTeam) {
                            pair.first -> Pair(newTeam, pair.second)
                            pair.second -> Pair(pair.first, newTeam)
                            else -> pair
                        }
                    }
                    round.copy(pairs = newPairs)
                }
                .forEach {
                    roundRepository.save(it)
                }
    }

    private fun replaceInTournaments(league: League, oldTeam: LeagueTeam, newTeam: LeagueTeam) {
        val tournaments = tournamentRepository.findAllByName(league.name)

        tournaments.map {
            var winners = it.winners.map { team -> if (team == oldTeam) newTeam else team }
            var losers = it.losers.map { team -> if (team == oldTeam) newTeam else team }
            it.copy(winners = winners, losers = losers)
        }.forEach {
            tournamentRepository.save(it)
        }
    }

    private fun replaceInTournamentMatches(league: League, oldTeam: LeagueTeam, newTeam: LeagueTeam) {
        val matches = tournamentMatchRepository.findAllByLeagueName(league.name)

        matches.map {
            if (it.competitors.contains(oldTeam)) {
              val newCompetitors = it.competitors.map { competitor -> if (competitor == oldTeam) newTeam else competitor }.toSet()
              it.copy(competitors = newCompetitors)
            }
            it
        }.forEach {
            tournamentMatchRepository.save(it)
        }
    }
}