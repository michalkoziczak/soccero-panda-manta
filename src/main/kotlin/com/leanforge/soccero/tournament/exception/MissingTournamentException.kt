package com.leanforge.soccero.tournament.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class MissingTournamentException(leagueName: String, competition: String) :
        IllegalStateException(":smoking: :smoking: :smoking: :smoking: :smoking: :smoking:\n" +
                "Seems like there is no `$competition` in league `$leagueName`. Please use MySQL to solve that.\n" +
                "Also check if tournaments are initialized for `$leagueName`")