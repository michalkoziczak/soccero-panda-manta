package com.leanforge.soccero.team.domain

import spock.lang.Specification
import spock.lang.Timeout

class TeamBuilderTest extends Specification {


    def "should list all possible teams with no excludes"() {
        given:
        def players = ['p1', 'p2', 'p3', 'p4', 'p5', 'p6', 'p7', 'p8', 'p9', 'p10', 'p11', 'p12']
        def excludes = [].toSet()
        def teamSize = 2

        when:
        def allPossibleTeams = new TeamBuilder(teamSize, excludes, players.toSet()).generateAllPossibleTeams()

        then:
        allPossibleTeams.size() == 66
    }

    def "should list all possible teams of size 2"() {
        given:
        def players = ['p1', 'p2', 'p3', 'p4', 'p5', 'p6', 'p7', 'p8', 'p9', 'p10', 'p11', 'p12']
        def excludes = [
                new TeamExclusion("", players[0..3].toSet(), UUID.randomUUID()),
                new TeamExclusion("", players[4..7].toSet(), UUID.randomUUID()),
                new TeamExclusion("", players[8..11].toSet(), UUID.randomUUID())
        ].toSet()
        def teamSize = 2

        when:
        def allPossibleTeams = new TeamBuilder(teamSize, excludes, players.toSet()).generateAllPossibleTeams()

        then:
        !allPossibleTeams.isEmpty()
        allPossibleTeams.contains(['p1', 'p7'].toSet())
        allPossibleTeams.contains(['p1', 'p8'].toSet())
        allPossibleTeams.contains(['p1', 'p9'].toSet())
        allPossibleTeams.contains(['p1', 'p12'].toSet())
        allPossibleTeams.contains(['p1', 'p5'].toSet())
        allPossibleTeams.contains(['p2', 'p5'].toSet())
        allPossibleTeams.contains(['p4', 'p5'].toSet())
        allPossibleTeams.contains(['p9', 'p5'].toSet())
        !allPossibleTeams.contains(['p1', 'p2'].toSet())
        !allPossibleTeams.contains(['p1', 'p4'].toSet())
        !allPossibleTeams.contains(['p5', 'p7'].toSet())
        !allPossibleTeams.contains(['p9', 'p10'].toSet())
        !allPossibleTeams.contains(['p9', 'p12'].toSet())
    }


    def "should list all possible teams of size 3"() {
        given:
        def players = ['p1', 'p2', 'p3', 'p4', 'p5', 'p6', 'p7', 'p8', 'p9', 'p10', 'p11', 'p12']
        def excludes = [
                new TeamExclusion("", players[0..3].toSet(), UUID.randomUUID()),
                new TeamExclusion("", players[4..7].toSet(), UUID.randomUUID()),
                new TeamExclusion("", players[8..11].toSet(), UUID.randomUUID())
        ].toSet()
        def teamSize = 3

        when:
        def allPossibleTeams = new TeamBuilder(teamSize, excludes, players.toSet()).generateAllPossibleTeams()

        then:
        !allPossibleTeams.isEmpty()
        allPossibleTeams.contains(['p1', 'p5', 'p9'].toSet())
        allPossibleTeams.contains(['p2', 'p5', 'p9'].toSet())
        allPossibleTeams.contains(['p3', 'p5', 'p9'].toSet())
        allPossibleTeams.contains(['p4', 'p5', 'p9'].toSet())
        !allPossibleTeams.contains(['p1', 'p2', 'p9'].toSet())
        !allPossibleTeams.contains(['p1', 'p3', 'p9'].toSet())
        !allPossibleTeams.contains(['p1', 'p4', 'p9'].toSet())
        !allPossibleTeams.contains(['p1', 'p2', 'p3'].toSet())
    }


    def "should should find solution for problem"() {
        given:
        def players = ['p1', 'p2', 'p3', 'p4', 'p5', 'p6', 'p7', 'p8', 'p9', 'p10', 'p11', 'p12']
        def excludes = [
                new TeamExclusion("", players[0..3].toSet(), UUID.randomUUID()),
                new TeamExclusion("", players[4..7].toSet(), UUID.randomUUID()),
                new TeamExclusion("", players[8..11].toSet(), UUID.randomUUID())
        ].toSet()
        def teamSize = 2

        when:
        def randomTeams = new TeamBuilder(teamSize, excludes, players.toSet()).randomTeams()

        then:
        !randomTeams.isEmpty()
        players.every { player ->
            randomTeams.count {it.slackIds.contains(player)} == 1
        }
        randomTeams.each { team ->
            assert !excludes.any {
                it.slackIds.containsAll(team.slackIds)
            }
        }

    }


    @Timeout(60)
    def "should should find solution for problem of many players of 2"() {
        given:
        def players = generatedPlayers(6 * 50)
        def excludes = evenlyDistributed(6, players)
        def teamSize = 2

        when:
        def randomTeams = new TeamBuilder(teamSize, excludes, players.toSet()).randomTeams()

        then:
        !randomTeams.isEmpty()
        players.every { player ->
            randomTeams.count {it.slackIds.contains(player)} == 1
        }
        randomTeams.each { team ->
            assert !excludes.any {
                it.slackIds.containsAll(team.slackIds)
            }
        }
    }

    @Timeout(60)
    def "should should find solution for problem of many players of 3"() {
        given:
        def players = generatedPlayers(6 * 10)
        def excludes = evenlyDistributed(6, players)
        def teamSize = 3

        when:
        def randomTeams = new TeamBuilder(teamSize, excludes, players.toSet()).randomTeams()

        then:
        !randomTeams.isEmpty()
        players.every { player ->
            randomTeams.count {it.slackIds.contains(player)} == 1
        }
        randomTeams.each { team ->
            assert !excludes.any {
                it.slackIds.containsAll(team.slackIds)
            }
        }
    }

    @Timeout(60)
    def "should should find solution for problem of many players of 1"() {
        given:
        def players = generatedPlayers(6 * 10)
        def excludes = evenlyDistributed(6, players)
        def teamSize = 1

        when:
        def randomTeams = new TeamBuilder(teamSize, excludes, players.toSet()).randomTeams()

        then:
        !randomTeams.isEmpty()
        players.every { player ->
            randomTeams.count {it.slackIds.contains(player)} == 1
        }
    }

    List<String> generatedPlayers(int size) {
        def ret = []

        size.times { i ->
            ret.add("p" + i)
        }

        ret
    }

    Set<TeamExclusion> evenlyDistributed(int groupSize, List<String> players) {
        if (players.size() % groupSize != 0) {
            throw new IllegalArgumentException("Can't do that...")
        }

        int groupCount = players.size() / groupSize

        def ret = []
        groupCount.times { i ->
            int start = i * groupSize
            int end = start + groupSize - 1
            ret.add(new TeamExclusion("", players[start..end].toSet(), UUID.randomUUID()))
        }

        ret
    }
}
