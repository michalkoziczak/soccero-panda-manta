package com.leanforge.soccero.result.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.CONFLICT)
class FrozenResultException(callerId: String) : IllegalStateException("<@$callerId> entry is frozen. Ask your team leader for advice.")