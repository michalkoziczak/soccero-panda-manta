package com.leanforge.soccero

import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.SlackUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import spock.lang.Specification

import java.time.ZoneId

@SpringBootTest(classes = TestApplication)
@Configuration
class ConsoleLockingServiceIntegrationTest extends Specification {

    @Autowired
    SlackSession slackSession

    def setup() {
        slackSession.findUserById(_) >> Stub(SlackUser) {
            getId() >> UUID.randomUUID().toString()
            getTimeZone() >> ZoneId.systemDefault().toString()
            getUserName() >> "username"
            getRealName() >> 'realname'
        }
        slackSession.getUsers() >> [
                Stub(SlackUser) {
                    getTimeZone() >> ZoneId.systemDefault().toString()
                    getUserName() >> "username-u1"
                    getRealName() >> 'realname-u1'
                    getId() >> 'u1'
                },
                Stub(SlackUser) {
                    getTimeZone() >> ZoneId.systemDefault().toString()
                    getUserName() >> "username-u2"
                    getRealName() >> 'realname-u2'
                    getId() >> 'u2'
                }
        ]
    }

    def "should find user username-u1"() {
        given:
        def userId = 'username-u1'

        when:
        def user = slackSession.findUserById(userId)

        then:
        user.getRealName() == 'realname'
    }
}
