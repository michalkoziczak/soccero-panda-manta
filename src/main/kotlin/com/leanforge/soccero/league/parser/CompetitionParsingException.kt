package com.leanforge.soccero.league.parser

import org.springframework.http.HttpStatus
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class CompetitionParsingException(definition: String) : IllegalArgumentException("[$definition] is invalid.")