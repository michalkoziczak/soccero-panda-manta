package com.leanforge.soccero.result.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class AmbiguousPlayerToTeamMappingException(reasonMessage: String) : IllegalArgumentException(reasonMessage) {
}