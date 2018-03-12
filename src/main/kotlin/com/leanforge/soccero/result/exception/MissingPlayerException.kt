package com.leanforge.soccero.result.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class MissingPlayerException(slackId: String) :
        IllegalArgumentException("I'm disappointed. " +
                "<@$slackId> is excluded from this game. " +
                "It is crucial to not interrupt other players.\n" +
                "If you are still unsure as to what I'm expecting from you then please " +
                "have a chat with bot authors, or let me know directly by `help` command.")