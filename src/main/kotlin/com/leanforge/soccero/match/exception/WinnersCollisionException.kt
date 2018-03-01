package com.leanforge.soccero.match.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.CONFLICT)
class WinnersCollisionException(slackId: String) :
        IllegalStateException("<@$slackId>, you caused collision. Please reflect on it and propose changes in " +
                "your's part of the process to prevent such cases in the future. " +
                "I expect written answer by end of business.\n" +
                "If you are still unsure as to what I'm expecting from you then please " +
                "have a chat with bot authors, or let me know directly by `help` command.")