package com.leanforge.soccero.league.parser

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class CompetitionParsingException(definition: String) : IllegalArgumentException("I'm disappointed. You failed to provide a valid competition. `$definition` is invalid.")