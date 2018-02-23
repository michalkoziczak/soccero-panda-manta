package com.leanforge.soccero.team.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class BadTeamSizeException(msg: String) : IllegalArgumentException(msg) {
}