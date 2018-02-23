package com.leanforge.soccero.team.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Can't form teams because of constraints")
class TeamConstraintException() : IllegalArgumentException() {
}