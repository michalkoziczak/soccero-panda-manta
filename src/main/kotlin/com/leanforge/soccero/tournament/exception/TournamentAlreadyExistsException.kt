package com.leanforge.soccero.tournament.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class TournamentAlreadyExistsException(leagueName: String) :
        IllegalStateException("What is the reason you tried to create duplicated tournaments for " +
                "league $leagueName? " +
                "This leads to serous issues, therefore I'll ask you to do the following:\n" +
                "- reflect on the problems you caused\n" +
                "- explain to everyone why you wanted to break the system.")