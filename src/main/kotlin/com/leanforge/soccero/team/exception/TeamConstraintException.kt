package com.leanforge.soccero.team.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "I'm disappointed. You failed to provide rules, that has at least one possible solution. Please provide me 5 Whys for the cause by EOB.")
class TeamConstraintException() : IllegalArgumentException() {
}