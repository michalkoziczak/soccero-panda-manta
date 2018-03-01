package com.leanforge.soccero.tournament

import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.league.domain.Competition
import com.leanforge.soccero.league.domain.League
import com.leanforge.soccero.match.domain.MatchResult
import com.leanforge.soccero.team.domain.LeagueTeam
import com.leanforge.soccero.tournament.domain.Tournament
import com.leanforge.soccero.tournament.exception.MissingTournamentException
import com.leanforge.soccero.tournament.exception.TournamentAlreadyExistsException
import com.leanforge.soccero.tournament.repo.TournamentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DefaultTournamentService @Autowired constructor(
        private val tournamentRepository: TournamentRepository,
        private val slackService: SlackService
) : TournamentService {


    override fun listPendingCompetitors(league: League, competition: Competition, results: List<MatchResult>): String {
        return ":trophy: `${competition.label()}`, Pending matches:\n" + listedCompetitors(pendingCompetitors(league, competition, results))
    }

    override fun pendingCompetitors(league: League, competition: Competition, results: List<MatchResult>): List<Set<LeagueTeam>> {
        val initial = tournamentRepository.findOneByNameAndCompetition(league.name, competition) ?: throw MissingTournamentException(league.name, competition.label())

        var round : Tournament = initial
        var resultsLeft : List<MatchResult> = results
        while(round.hasAllResults(resultsLeft)) {
            val currentRound = round.filterCurrentResults(resultsLeft)
            round = round.currentState(currentRound)
            resultsLeft -= currentRound
        }

        return round.competitors()
                .filter { c -> resultsLeft.none { r -> r.hasTeams(c) } }
                .toList()
    }


    override fun currentState(league: League, competition: Competition, results: List<MatchResult>): Tournament {
        val initial = tournamentRepository.findOneByNameAndCompetition(league.name, competition) ?: throw MissingTournamentException(league.name, competition.label())

        var round : Tournament = initial
        var resultsLeft : List<MatchResult> = results
        while(round.hasAllResults(resultsLeft)) {
            val currentRound = round.filterCurrentResults(resultsLeft)
            round = round.currentState(currentRound)
            resultsLeft -= currentRound
        }

        return round
    }

    override fun createTournaments(league: League) {
        val tournaments = mutableSetOf<Tournament>()
        league.competitions.onEach {
            tournaments.add(createTournament(tournaments, it, league))
        }

        verifyHasNoDuplicates(league, tournaments)
        tournamentRepository.save(tournaments)

        tournaments.map { initialStatusMessage(it) }
                .onEach { initialMessage -> slackService.sendChannelMessage(league.slackChannelId, initialMessage) }
    }

    private fun createTournament(existingTournaments: Set<Tournament>, competition: Competition, league: League) : Tournament {
        val teams = league.teams.filter { it.slackIds.size == competition.players }
                .toList()
        return List(100, { Tournament(league.name, competition, teams.shuffled(), emptyList()) })
                .minBy { generatedTournament -> countDuplicates(existingTournaments, generatedTournament) }
                ?: Tournament(league.name, competition, league.teams.shuffled(), emptyList())

    }

    private fun countDuplicates(existingTournaments: Set<Tournament>, generated: Tournament) : Int {
        return generated.competitors().map { c -> existingTournaments.count { it.competitors().contains(c) } }
                .sum()
    }

    private fun verifyHasNoDuplicates(league: League, generatedTournaments: Set<Tournament>) {
        val hasDuplicate = tournamentRepository.findAllByName(league.name)
                .anyMatch({ saved -> generatedTournaments.any { generated -> generated.competition == saved.competition } })

        if (hasDuplicate) {
            throw TournamentAlreadyExistsException(league.name)
        }
    }

    private fun initialStatusMessage(tournament: Tournament) : String {
        return ":trophy: `${tournament.competition.label()}`\n>>>" +
                listedCompetitors(tournament.competitors())
    }

    private fun listedCompetitors(competitors: List<Set<LeagueTeam>>) : String {
        return competitors
                .mapIndexed { index, teams -> "${index + 1}. ${competitorsLine(teams)}" }
                .joinToString("\n")
    }

    private fun teamKeywordsLabel(team: LeagueTeam) : String {
        return "(" + team.slackIds.map { "<@$it>" }
                .joinToString(" & ") + ")"
    }

    private fun competitorsLine(teams: Set<LeagueTeam>) : String {
        return teams.map { teamKeywordsLabel(it) }
                .joinToString(" vs ")
    }
}