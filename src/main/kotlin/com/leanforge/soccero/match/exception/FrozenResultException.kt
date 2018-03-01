package com.leanforge.soccero.match.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Entry is frozen. Ask your TeamLead for advice.")
class FrozenResultException : IllegalStateException() {
}