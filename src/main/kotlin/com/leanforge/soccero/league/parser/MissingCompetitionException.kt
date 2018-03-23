package com.leanforge.soccero.league.parser

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "I'm disappointed. You failed to provide proper competition. Reflect on your actions.")
class MissingCompetitionException() : IllegalArgumentException()