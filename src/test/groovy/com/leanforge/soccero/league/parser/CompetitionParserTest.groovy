package com.leanforge.soccero.league.parser

import com.leanforge.soccero.league.domain.Competition
import spock.lang.Specification
import spock.lang.Unroll

class CompetitionParserTest extends Specification {

    CompetitionParser parser = new CompetitionParser()

    @Unroll
    def "should parse name: #name, count: #count"(String name, int count) {
        given:
        def definition = "$name:${count}vs${count}"

        when:
        def competitions = parser.parseDefinition(definition)

        then:
        competitions.size() == 1
        competitions.first().name == name
        competitions.first().players == count

        where:
        name        | count
        'abc'       | 1
        'abc123'    | 2
        'abc123'    | 9
        'abc_123'   | 2
        'a'         | 2
        'a-1'       | 2
    }

    def "should parse 'a1:1vs1 a1:2vs2 a2:2vs2'"() {
        given:
        def definition = "a1:1vs1 a1:2vs2 a2:2vs2"

        when:
        def competitions = parser.parseDefinition(definition)

        then:
        competitions.size() == 3
        competitions.containsAll(
                new Competition('a1', 1),
                new Competition('a1', 2),
                new Competition('a2', 2)
        )
    }

    def "should parse ' a1:1vs1 a1:2vs2 a2:2vs2'"() {
        given:
        def definition = " a1:1vs1 a1:2vs2 a2:2vs2"

        when:
        def competitions = parser.parseDefinition(definition)

        then:
        competitions.size() == 3
        competitions.containsAll(
                new Competition('a1', 1),
                new Competition('a1', 2),
                new Competition('a2', 2)
        )
    }

    def "should fail to parse a1:1vs2"() {
        given:
        def definition = 'a1:1vs2'

        when:
        parser.parseDefinition(definition)

        then:
        thrown(IllegalArgumentException)
    }

    def "should fail to parse a1:testtesttest"() {
        given:
        def definition = 'a1:testtesttest'

        when:
        parser.parseDefinition(definition)

        then:
        thrown(IllegalArgumentException)
    }
}
