package com.leanforge.soccero.button

import com.fasterxml.jackson.databind.ObjectMapper
import com.leanforge.game.slack.SlackService
import com.leanforge.soccero.readiness.LeagueReadinessService
import com.leanforge.soccero.readiness.ReadinessService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(value = "/buttons", consumes = ["application/x-www-form-urlencoded", "application/json"])
class ButtonController @Autowired constructor(
        val readinessService: ReadinessService,
        val leagueReadinessService: LeagueReadinessService,
        val slackService: SlackService) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    @PostMapping
    fun clicked(@RequestParam data: Map<String, String>) {
        logger.debug("Received button event: {}", data["payload"])
        val payload = objectMapper.readTree(data["payload"])
        val userId = payload?.get("user")?.get("id")?.textValue() ?: return
        val action = payload.get("actions")?.get(0) ?: return
        val actionName = action.get("name")?.textValue() ?: return
        val actionValue = action.get("value")?.textValue() ?: return

        if (actionName != "state") {
            return;
        }

        logger.debug("Handling state change: userId: {}, actionValue: {}", userId, actionValue)
        if (actionValue == "ready") {
            readinessService.markReady(userId)
        } else if (actionValue == "busy") {
            readinessService.markBusy(userId)
        }

        leagueReadinessService.updateStatusMessagesForAllStartedLeagues()
    }
}