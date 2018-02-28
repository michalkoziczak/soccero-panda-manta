package com.leanforge.soccero.match.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class MissingPlayerException(slackId: String, leagueName: String, competitionName: String) :
        IllegalArgumentException("I'm disappointed. " +
                "<@$slackId> is excluded from `$competitionName` in `$leagueName`.\n" +
                "If you are still unsure as to what I'm expecting from you then please " +
                "have a chat with bot authors, or let me know directly by `help` command.")