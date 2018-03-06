package com.leanforge.soccero.result

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Controller
class TournamentTreeController
    @Autowired constructor(
            val tournamentTreeService: TournamentTreeService
    )
    : TextWebSocketHandler() {

    val objectMapper = ObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        tournamentTreeService.registerListener({
            if (!session.isOpen) {
                return@registerListener false
            }
            val json = objectMapper.writeValueAsString(it)
            session.sendMessage(TextMessage(json))
            true
        })
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        if (message.payload == "init") {
            tournamentTreeService.allActiveTrees()
                    .map { objectMapper.writeValueAsString(it) }
                    .onEach { session.sendMessage(TextMessage(it)) }
        }
    }
}