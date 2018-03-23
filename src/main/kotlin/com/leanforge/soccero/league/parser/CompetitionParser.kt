package com.leanforge.soccero.league.parser

import com.leanforge.soccero.league.domain.Competition
import java.util.regex.Pattern

object CompetitionParser {

    val definitionPattern: Pattern = Pattern.compile("(\\w[\\w\\d_-]*):(\\d)vs(\\d)")
    val separatorPattern: Pattern = Pattern.compile("\\s+")

    fun parseDefinition(definition: String) : Set<Competition> {
        return definition.split(separatorPattern)
                .filter { !it.isBlank() }
                .map { toCompetition(it) }
                .toSet()
    }

    fun parseSingleDefinition(definition: String) : Competition {
        val competitions = parseDefinition(definition)

        if (competitions.size != 1) {
            throw CompetitionParsingException(definition)
        }

        return competitions.single()
    }

    private fun toCompetition(competitionString: String) : Competition {
        val matcher = definitionPattern.matcher(competitionString)
        if (!matcher.matches()) {
            throw CompetitionParsingException(competitionString)
        }

        val name = matcher.group(1)
        val count1 = matcher.group(2)
        val count2 = matcher.group(3)

        if (count1 != count2) {
            throw CompetitionParsingException(competitionString)
        }

        return Competition(name, count1.toInt())
    }
}